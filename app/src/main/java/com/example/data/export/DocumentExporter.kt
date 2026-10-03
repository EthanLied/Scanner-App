package com.example.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.data.model.ScannedPage
import com.example.util.CrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object DocumentExporter {
    private const val TAG = "DocumentExporter"

    /**
     * Exports multiple scanned pages into a single combined multi-page PDF.
     * Uses Android's built-in PdfDocument API with memory safeguards to avoid OOM
     * and prevent 0-byte corrupt files.
     */
    suspend fun exportToCombinedPdf(
        context: Context,
        pages: List<ScannedPage>,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            Log.e(TAG, "Cannot export empty pages list to PDF")
            return@withContext false
        }

        var pdfDocument: PdfDocument? = null
        var pagesAdded = 0

        try {
            pdfDocument = PdfDocument()

            for ((index, page) in pages.withIndex()) {
                val file = File(page.filePath)
                if (!file.exists() || file.length() == 0L) {
                    Log.w(TAG, "Skipping missing or empty page file: ${page.filePath}")
                    continue
                }

                val bounds = getBitmapDimensions(file)
                val pixelWidth = if (bounds.first > 0) bounds.first else max(1, page.widthPx)
                val pixelHeight = if (bounds.second > 0) bounds.second else max(1, page.heightPx)

                // Convert pixel dimensions to standard PDF points (72 points per inch)
                // A4 @ 300 DPI (2480x3508 px) becomes 595 x 842 pt.
                val scanDpi = if (page.dpi > 0) page.dpi.toFloat() else 300f
                val scale = 72f / scanDpi
                val pagePtWidth = max(72, (pixelWidth * scale).toInt())
                val pagePtHeight = max(72, (pixelHeight * scale).toInt())

                val pageInfo = PdfDocument.PageInfo.Builder(pagePtWidth, pagePtHeight, pagesAdded + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Memory-safe bitmap decoding without caching in LRU cache
                val bitmap = CrashLogger.decodeBitmapForExport(
                    filePath = file.absolutePath,
                    reqWidth = 2000,
                    reqHeight = 2800,
                    preferredConfig = Bitmap.Config.RGB_565
                )

                if (bitmap != null) {
                    val destRect = Rect(0, 0, pagePtWidth, pagePtHeight)
                    val paint = Paint().apply {
                        isFilterBitmap = true
                        isDither = true
                    }
                    canvas.drawBitmap(bitmap, null, destRect, paint)
                    bitmap.recycle()
                    pdfDocument.finishPage(pdfPage)
                    pagesAdded++
                } else {
                    Log.e(TAG, "Failed decoding bitmap for page ${index + 1}: ${file.name}")
                    pdfDocument.finishPage(pdfPage)
                }
            }

            if (pagesAdded == 0) {
                Log.e(TAG, "No valid pages could be decoded into PDF document")
                CrashLogger.logNonFatal(TAG, "Export PDF failed: 0 pages added", null)
                return@withContext false
            }

            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            Log.d(TAG, "Successfully exported PDF with $pagesAdded pages")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error creating combined PDF: ${t.message}", t)
            CrashLogger.logNonFatal(TAG, "Failed creating combined PDF: ${t.message}", t)
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (_: Exception) {}
            try {
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Exports a single scanned page to a PDF OutputStream.
     */
    suspend fun exportSinglePageToPdf(
        context: Context,
        page: ScannedPage,
        outputStream: OutputStream
    ): Boolean = exportToCombinedPdf(context, listOf(page), outputStream)

    /**
     * Exports a single scanned page to PNG by decoding the scanner's raw JPEG and re-encoding.
     */
    suspend fun exportPageToPng(
        page: ScannedPage,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(page.filePath)
            if (!file.exists() || file.length() == 0L) return@withContext false

            val bitmap = CrashLogger.decodeBitmapForExport(
                filePath = file.absolutePath,
                reqWidth = 2400,
                reqHeight = 3500,
                preferredConfig = Bitmap.Config.ARGB_8888
            ) ?: return@withContext false

            val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            bitmap.recycle()
            success
        } catch (t: Throwable) {
            Log.e(TAG, "Failed exporting to PNG: ${t.message}", t)
            CrashLogger.logNonFatal(TAG, "Failed exporting to PNG: ${t.message}", t)
            false
        } finally {
            try {
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Exports multiple scanned pages to individual PNG files in a selected directory Uri (SAF tree).
     * Returns the count of successfully written PNG pages.
     */
    suspend fun exportPagesToTreeDirectory(
        context: Context,
        pages: List<ScannedPage>,
        treeUri: Uri
    ): Int = withContext(Dispatchers.IO) {
        var exportedCount = 0
        try {
            val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            for (page in pages) {
                val filename = "Scan_Page_${page.pageNumber}_${System.currentTimeMillis()}.png"
                val pageDocUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    treeDocUri,
                    "image/png",
                    filename
                ) ?: continue

                val outputStream = context.contentResolver.openOutputStream(pageDocUri)
                if (outputStream != null) {
                    val success = exportPageToPng(page, outputStream)
                    if (success) {
                        exportedCount++
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed exporting multiple PNGs to tree URI: ${t.message}", t)
            CrashLogger.logNonFatal(TAG, "Failed exporting multiple PNGs to tree URI: ${t.message}", t)
        }
        exportedCount
    }

    private fun getBitmapDimensions(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }

    /**
     * Prepares a temporary PDF file in cacheDir/shares with standard timestamp naming (e.g. 20261003150902.pdf)
     * and returns its FileProvider Uri and filename for sharing.
     */
    suspend fun prepareSharePdf(context: Context, pages: List<ScannedPage>): Pair<Uri, String>? = withContext(Dispatchers.IO) {
        try {
            val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
            val filename = "$timeStamp.pdf"
            val tempFile = File(shareDir, filename)
            val outputStream = tempFile.outputStream()
            val success = exportToCombinedPdf(context, pages, outputStream)
            if (success && tempFile.exists() && tempFile.length() > 0) {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                Pair(uri, filename)
            } else {
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed preparing share PDF: ${t.message}", t)
            null
        }
    }

    /**
     * Prepares temporary PNG files in cacheDir/shares and returns their FileProvider Uris for sharing.
     */
    suspend fun prepareSharePngs(context: Context, pages: List<ScannedPage>): List<Uri> = withContext(Dispatchers.IO) {
        val uriList = mutableListOf<Uri>()
        try {
            val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
            for (page in pages) {
                val tempFile = File(shareDir, "Scan_Page_${page.pageNumber}_${System.currentTimeMillis()}.png")
                val outputStream = tempFile.outputStream()
                val success = exportPageToPng(page, outputStream)
                if (success && tempFile.exists() && tempFile.length() > 0) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempFile
                    )
                    uriList.add(uri)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed preparing share PNGs: ${t.message}", t)
        }
        uriList
    }
}
