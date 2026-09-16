package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScanSession(
    val sessionId: String,
    val createdAt: Long,
    val pages: List<ScannedPage> = emptyList()
)
