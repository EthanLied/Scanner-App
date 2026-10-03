package com.example.data.export

/**
 * Real-time progress model for document exporting.
 * Tracks current page number, total page count, status text, and percentage.
 */
data class ExportProgress(
    val currentPage: Int,
    val totalPages: Int,
    val statusText: String,
    val format: String = "PDF"
) {
    val progressFraction: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f

    val percent: Int
        get() = (progressFraction * 100).toInt()
}
