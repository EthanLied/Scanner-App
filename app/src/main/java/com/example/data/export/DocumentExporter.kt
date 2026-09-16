package com.example.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import com.example.data.model.ScannedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

object DocumentExporter {
    private const val TAG = "DocumentExporter"

    /**
     * Exports multiple scanned pages into a single combined multi-page PDF.
     * Uses Android's built-in PdfDocument API.
     */
    suspend fun exportToCombinedPdf(
        context: Context,
        pages: List<ScannedPage>,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            for ((index, page) in pages.withIndex()) {
                val file = File(page.filePath)
                if (!file.exists()) continue

                val bounds = getBitmapDimensions(file)
                val width = if (bounds.first > 0) bounds.first else page.widthPx
                val height = if (bounds.second > 0) bounds.second else page.heightPx

                // Use standard 72 DPI PDF points (A4 is 595 x 842 pt, Letter is 612 x 792 pt)
                // or use pixel-based canvas
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Decode bitmap safely
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val destRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(bitmap, null, destRect, null)
                    bitmap.recycle()
                }

                pdfDocument.finishPage(pdfPage)
            }

            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating combined PDF", e)
            false
        } finally {
            pdfDocument.close()
            try { outputStream.close() } catch (_: Exception) {}
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
            if (!file.exists()) return@withContext false

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            bitmap.recycle()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting to PNG", e)
            false
        } finally {
            try { outputStream.close() } catch (_: Exception) {}
        }
    }

    private fun getBitmapDimensions(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }
}
