package com.example.protocol

import com.example.data.model.ScanSettings
import java.io.File

data class ProbeResult(
    val success: Boolean,
    val rungName: String,
    val message: String,
    val details: String = ""
)

data class ScanProgress(
    val stepName: String,
    val percentage: Float, // 0.0f to 1.0f
    val bytesReceived: Long = 0,
    val totalBytesExpected: Long = 0,
    val statusDetail: String = ""
)

data class ScanPageResult(
    val success: Boolean,
    val file: File?,
    val widthPx: Int,
    val heightPx: Int,
    val deliveredLines: Int,
    val error: String? = null
)

interface ScannerTransport {
    val name: String

    suspend fun probe(ip: String): ProbeResult

    suspend fun scanPage(
        ip: String,
        settings: ScanSettings,
        destinationFile: File,
        progressListener: (ScanProgress) -> Unit
    ): ScanPageResult
}
