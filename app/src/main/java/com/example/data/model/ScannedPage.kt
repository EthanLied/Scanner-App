package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScannedPage(
    val id: String,
    val pageNumber: Int,
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val actualDeliveredLines: Int,
    val timestamp: Long,
    val dpi: Int,
    val colorMode: String,
    val pageSizeLabel: String,
    val fileSizeBytes: Long
)
