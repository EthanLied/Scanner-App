package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.example.protocol.ProtocolLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object CrashLogger {
    private const val TAG = "CrashLogger"
    private const val LOG_FILE_NAME = "crash_logs.txt"
    private const val MAX_LOG_SIZE = 100 * 1024 // 100 KB max

    private class CacheEntry(val bitmap: Bitmap, val byteCount: Int)

    // 16MB LRU Cache for decoded thumbnails
    private val bitmapCache = object : LruCache<String, CacheEntry>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: CacheEntry): Int {
            return value.byteCount
        }
    }

    private var logFile: File? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    private val _crashLogsFlow = MutableStateFlow("")
    val crashLogsFlow: StateFlow<String> = _crashLogsFlow.asStateFlow()

    fun install(context: Context) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        logFile = file
        refreshLogs()

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed recording crash", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun recordCrash(thread: Thread, throwable: Throwable) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)

        val entry = buildString {
            append("==================================================\n")
            append("CRASH EVENT: $timeStr\n")
            append("Thread: ${thread.name} (id: ${thread.id})\n")
            append("Exception: ${throwable.javaClass.name}: ${throwable.message}\n")
            append("Memory: Used ${usedMemMb}MB / Max ${maxMemMb}MB\n")
            append("Stack Trace:\n$stackTrace\n")
            append("==================================================\n\n")
        }

        appendToFile(entry)
        ProtocolLogger.log("CRASH", "System", thread.name, "", "FATAL", 0, "${throwable.javaClass.simpleName}: ${throwable.message}")
        Log.e(TAG, "Uncaught exception recorded:\n$entry")
    }

    fun logNonFatal(tag: String, message: String, throwable: Throwable?) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sw = StringWriter()
        throwable?.printStackTrace(PrintWriter(sw))
        val stackTrace = if (throwable != null) "\nStack Trace:\n$sw" else ""

        val entry = buildString {
            append("--------------------------------------------------\n")
            append("NON-FATAL ERROR: $timeStr [$tag]\n")
            append("Message: $message\n")
            if (throwable != null) {
                append("Exception: ${throwable.javaClass.name}: ${throwable.message}$stackTrace\n")
            }
            append("--------------------------------------------------\n\n")
        }

        appendToFile(entry)
        ProtocolLogger.log("ERROR", tag, message, "", "ERROR", 0, throwable?.message ?: "")
        Log.w(TAG, entry)
    }

    @Synchronized
    private fun appendToFile(text: String) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_LOG_SIZE) {
                // Trim older logs
                val existing = file.readText()
                val trimmed = existing.takeLast(MAX_LOG_SIZE / 2)
                file.writeText(trimmed)
            }
            file.appendText(text)
            refreshLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to crash log file", e)
        }
    }

    fun refreshLogs() {
        val file = logFile ?: return
        if (file.exists()) {
            try {
                _crashLogsFlow.value = file.readText()
            } catch (_: Exception) {}
        } else {
            _crashLogsFlow.value = ""
        }
    }

    fun clearLogs() {
        val file = logFile ?: return
        try {
            if (file.exists()) {
                file.delete()
            }
            _crashLogsFlow.value = ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing crash logs", e)
        }
    }

    fun getCachedBitmap(filePath: String, reqWidth: Int = 1600, reqHeight: Int = 2048): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null
        val cacheKey = "${file.absolutePath}_${file.lastModified()}_${reqWidth}x${reqHeight}"
        synchronized(bitmapCache) {
            val entry = bitmapCache.get(cacheKey)
            if (entry != null && !entry.bitmap.isRecycled) {
                return entry.bitmap
            }
        }
        return null
    }

    /**
     * Memory-safe image decoder that prevents OutOfMemoryError and Canvas texture size crashes.
     */
    fun decodeSampledBitmap(
        filePath: String,
        reqWidth: Int = 1600,
        reqHeight: Int = 2048,
        preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565,
        useCache: Boolean = true
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null

        val cacheKey = "${file.absolutePath}_${file.lastModified()}_${reqWidth}x${reqHeight}"
        if (useCache) {
            synchronized(bitmapCache) {
                val entry = bitmapCache.get(cacheKey)
                if (entry != null && !entry.bitmap.isRecycled) {
                    return entry.bitmap
                }
            }
        }

        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight

            if (rawWidth <= 0 || rawHeight <= 0) return null

            // Calculate inSampleSize
            var sampleSize = 1
            if (rawHeight > reqHeight || rawWidth > reqWidth) {
                val halfHeight = rawHeight / 2
                val halfWidth = rawWidth / 2
                while ((halfHeight / sampleSize) >= reqHeight || (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            // Ensure sample size prevents GL max texture size limit violation (2048)
            while ((rawWidth / sampleSize) > 2048 || (rawHeight / sampleSize) > 2048) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = max(1, sampleSize)
                inPreferredConfig = preferredConfig
            }

            val decoded = try {
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                logNonFatal("BitmapDecoder", "OOM decoding ${file.name} with sampleSize $sampleSize, retrying with ${sampleSize * 2}", oom)
                decodeOptions.inSampleSize = sampleSize * 2
                decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            }

            if (decoded != null && useCache) {
                val byteCount = try {
                    decoded.byteCount.coerceAtLeast(1)
                } catch (_: Throwable) {
                    (decoded.width * decoded.height * 2).coerceAtLeast(1)
                }
                synchronized(bitmapCache) {
                    bitmapCache.put(cacheKey, CacheEntry(decoded, byteCount))
                }
            }
            decoded
        } catch (t: Throwable) {
            logNonFatal("BitmapDecoder", "Fatal failure decoding ${file.name}", t)
            null
        }
    }

    /**
     * Decodes a fresh, standalone bitmap specifically for PDF/PNG exports.
     * Does NOT cache in the UI LRU cache, preventing LruCache corruption and memory pressure.
     * The caller is responsible for recycling this bitmap when export completes.
     */
    fun decodeBitmapForExport(
        filePath: String,
        reqWidth: Int = 2400,
        reqHeight: Int = 3500,
        preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            var sampleSize = 1
            if (rawHeight > reqHeight || rawWidth > reqWidth) {
                val halfHeight = rawHeight / 2
                val halfWidth = rawWidth / 2
                while ((halfHeight / sampleSize) >= reqHeight || (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = max(1, sampleSize)
                inPreferredConfig = preferredConfig
            }

            try {
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } catch (oom: OutOfMemoryError) {
                logNonFatal("BitmapDecoder", "OOM in export decoding ${file.name}, retrying with sampleSize ${sampleSize * 2}", oom)
                decodeOptions.inSampleSize = sampleSize * 2
                decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            }
        } catch (t: Throwable) {
            logNonFatal("BitmapDecoder", "Fatal failure decoding export bitmap for ${file.name}", t)
            null
        }
    }
}
