package com.example.data.session

import android.content.Context
import android.util.Log
import com.example.data.model.ScannedPage
import com.example.data.model.ScanSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SessionManager(private val context: Context) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val sessionAdapter = moshi.adapter(ScanSession::class.java)

    private val sessionDir = File(context.filesDir, "active_session").apply { mkdirs() }
    private val sessionIndexFile = File(sessionDir, "session.json")

    private val _currentSession = MutableStateFlow(loadOrCreateSession())
    val currentSession: StateFlow<ScanSession> = _currentSession.asStateFlow()

    private fun loadOrCreateSession(): ScanSession {
        if (sessionIndexFile.exists()) {
            try {
                val json = sessionIndexFile.readText()
                val loaded = sessionAdapter.fromJson(json)
                if (loaded != null) {
                    // Filter out any pages whose files were deleted
                    val validPages = loaded.pages.filter { File(it.filePath).exists() }
                    Log.d(TAG, "Restored scan session ${loaded.sessionId} with ${validPages.size} pages from disk")
                    return loaded.copy(pages = validPages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading session.json, creating new session", e)
            }
        }
        val newSession = ScanSession(
            sessionId = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            pages = emptyList()
        )
        saveSessionToDisk(newSession)
        return newSession
    }

    /**
     * Allocates a target file on disk for a new scan page.
     */
    fun createTargetPageFile(): File {
        val filename = "page_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        return File(sessionDir, filename)
    }

    /**
     * CRASH SAFETY RULE:
     * Write and persist session index to disk BEFORE updating in-memory state for the UI.
     */
    suspend fun recordNewPage(
        file: File,
        widthPx: Int,
        heightPx: Int,
        deliveredLines: Int,
        dpi: Int,
        colorMode: String,
        pageSizeLabel: String
    ): ScannedPage = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        val pageNumber = session.pages.size + 1
        val page = ScannedPage(
            id = UUID.randomUUID().toString(),
            pageNumber = pageNumber,
            filePath = file.absolutePath,
            widthPx = widthPx,
            heightPx = heightPx,
            actualDeliveredLines = deliveredLines,
            timestamp = System.currentTimeMillis(),
            dpi = dpi,
            colorMode = colorMode,
            pageSizeLabel = pageSizeLabel,
            fileSizeBytes = file.length()
        )

        val updatedPages = session.pages + page
        val updatedSession = session.copy(pages = updatedPages)

        // Persist to disk FIRST before updating in-memory state
        saveSessionToDisk(updatedSession)

        // Then notify UI
        _currentSession.value = updatedSession
        Log.d(TAG, "Page #${page.pageNumber} saved to disk and recorded: ${file.name}")
        page
    }

    suspend fun deletePage(pageId: String) = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        val pageToDelete = session.pages.find { it.id == pageId }
        if (pageToDelete != null) {
            try {
                File(pageToDelete.filePath).delete()
            } catch (_: Exception) {}
        }
        val updatedPages = session.pages.filter { it.id != pageId }.mapIndexed { idx, p ->
            p.copy(pageNumber = idx + 1)
        }
        val updatedSession = session.copy(pages = updatedPages)
        saveSessionToDisk(updatedSession)
        _currentSession.value = updatedSession
    }

    suspend fun reorderPages(fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (fromIndex !in session.pages.indices || toIndex !in session.pages.indices) return@withContext
        val list = session.pages.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val renumbered = list.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        val updatedSession = session.copy(pages = renumbered)
        saveSessionToDisk(updatedSession)
        _currentSession.value = updatedSession
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        for (page in session.pages) {
            try {
                File(page.filePath).delete()
            } catch (_: Exception) {}
        }
        val newSession = ScanSession(
            sessionId = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            pages = emptyList()
        )
        saveSessionToDisk(newSession)
        _currentSession.value = newSession
    }

    private fun saveSessionToDisk(session: ScanSession) {
        try {
            val json = sessionAdapter.toJson(session)
            val tmp = File(sessionDir, "session.json.tmp")
            tmp.writeText(json)
            if (tmp.exists()) {
                tmp.renameTo(sessionIndexFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing session.json to disk", e)
        }
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
