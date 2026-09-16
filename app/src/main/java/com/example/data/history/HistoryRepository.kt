package com.example.data.history

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.ScannedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HistoryRepository(
    private val context: Context,
    private val dao: ScanHistoryDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pixma_history_prefs", Context.MODE_PRIVATE)

    private val _purgeExpiryDays = MutableStateFlow(
        prefs.getInt(KEY_PURGE_EXPIRY_DAYS, DEFAULT_PURGE_EXPIRY_DAYS)
    )
    val purgeExpiryDays: StateFlow<Int> = _purgeExpiryDays.asStateFlow()

    val allHistory: Flow<List<ScanHistoryEntity>> = dao.getAllHistory()

    fun setPurgeExpiryDays(days: Int) {
        prefs.edit().putInt(KEY_PURGE_EXPIRY_DAYS, days).apply()
        _purgeExpiryDays.value = days
    }

    /**
     * Records an export event.
     * Deduplication rule: If exporting multiple times with the exact same collection of images,
     * do NOT create another scan ID; update latest update timestamp on the existing scan ID.
     */
    suspend fun recordExport(pages: List<ScannedPage>): String = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext ""

        // Calculate unique fingerprint from ordered page file paths and sizes
        val fingerprint = pages.joinToString("|") { "${it.filePath}:${it.fileSizeBytes}" }

        // Check if an existing scan ID matches this fingerprint
        val existing = dao.getHistoryByFingerprint(fingerprint)
        val now = System.currentTimeMillis()

        if (existing != null) {
            Log.d(TAG, "Reusing existing scan ID ${existing.scanId} for identical export collection")
            dao.updateTimestamp(existing.scanId, now)
            return@withContext existing.scanId
        }

        val dateTag = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
        val shortRand = UUID.randomUUID().toString().take(4).uppercase()
        val scanId = "SCAN-$dateTag-$shortRand"

        val latestPage = pages.last()
        val totalSize = pages.sumOf { it.fileSizeBytes }

        val entity = ScanHistoryEntity(
            scanId = scanId,
            fingerprint = fingerprint,
            createdAt = now,
            updatedAt = now,
            itemCount = pages.size,
            latestImageFilePath = latestPage.filePath,
            totalSizeBytes = totalSize
        )

        val items = pages.mapIndexed { index, page ->
            ScanHistoryItemEntity(
                scanId = scanId,
                pageNumber = index + 1,
                imageFilePath = page.filePath,
                widthPx = page.widthPx,
                heightPx = page.heightPx,
                dpi = page.dpi,
                colorMode = page.colorMode,
                pageSizeLabel = page.pageSizeLabel,
                fileSizeBytes = page.fileSizeBytes
            )
        }

        dao.insertScanWithItems(entity, items)
        Log.d(TAG, "Recorded new scan history $scanId with ${items.size} pages")
        scanId
    }

    suspend fun getPagesForScan(scanId: String): List<ScannedPage> = withContext(Dispatchers.IO) {
        val items = dao.getItemsForScan(scanId)
        items.map { item ->
            ScannedPage(
                id = UUID.randomUUID().toString(),
                pageNumber = item.pageNumber,
                filePath = item.imageFilePath,
                widthPx = item.widthPx,
                heightPx = item.heightPx,
                actualDeliveredLines = item.heightPx,
                timestamp = System.currentTimeMillis(),
                dpi = item.dpi,
                colorMode = item.colorMode,
                pageSizeLabel = item.pageSizeLabel,
                fileSizeBytes = item.fileSizeBytes
            )
        }
    }

    /**
     * Deletes a single scan history item.
     * Safely checks reference count: removes images from storage ONLY if no other scan ID
     * and no active session page depends on them.
     */
    suspend fun deleteScan(scanId: String, activeSessionPagePaths: Set<String>) = withContext(Dispatchers.IO) {
        val items = dao.getItemsForScan(scanId)
        dao.deleteScanWithItems(scanId)

        // Reference counting check for each image file
        for (item in items) {
            cleanupImageIfUnreferenced(item.imageFilePath, activeSessionPagePaths)
        }
        Log.d(TAG, "Deleted scan history $scanId")
    }

    /**
     * Purges expired scan history on app opening.
     * Checks each scan ID's latest update timestamp against current timestamp.
     * Default: 14 days (or user configured). 0 = never purge.
     */
    suspend fun purgeExpiredOnStartup(activeSessionPagePaths: Set<String>) = withContext(Dispatchers.IO) {
        val expiryDays = _purgeExpiryDays.value
        if (expiryDays <= 0) {
            Log.d(TAG, "Auto-purge disabled (expiryDays = $expiryDays)")
            return@withContext
        }

        val cutoff = System.currentTimeMillis() - (expiryDays.toLong() * 24L * 60 * 60 * 1000L)
        val expired = dao.getExpiredHistory(cutoff)
        if (expired.isEmpty()) {
            Log.d(TAG, "Startup purge check: 0 expired scan IDs found")
            return@withContext
        }

        Log.d(TAG, "Startup purge check: Purging ${expired.size} scan IDs older than $expiryDays days")
        for (scan in expired) {
            deleteScan(scan.scanId, activeSessionPagePaths)
        }
    }

    /**
     * Removes an image file from storage ONLY when no scan ID and no active session depends on it.
     */
    private suspend fun cleanupImageIfUnreferenced(
        filePath: String,
        activeSessionPagePaths: Set<String>
    ) {
        if (activeSessionPagePaths.contains(filePath)) {
            // Still in active session
            return
        }

        val remainingRefCount = dao.countReferencesToImage(filePath)
        if (remainingRefCount == 0) {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Cleaned unreferenced image file from storage: ${file.name} (deleted=$deleted)")
            }
        }
    }

    companion object {
        private const val TAG = "HistoryRepository"
        const val KEY_PURGE_EXPIRY_DAYS = "purge_expiry_days"
        const val DEFAULT_PURGE_EXPIRY_DAYS = 14
    }
}
