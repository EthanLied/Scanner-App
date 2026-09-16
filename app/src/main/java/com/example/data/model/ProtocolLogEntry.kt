package com.example.data.model

data class ProtocolLogEntry(
    val id: Long,
    val timestamp: Long,
    val direction: String, // "TX" or "RX" or "SYS"
    val endpoint: String,
    val commandName: String,
    val hexPreview: String,
    val status: String,
    val byteCount: Int,
    val detail: String = ""
) {
    fun toFormattedString(): String {
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(timestamp))
        return "[$timeStr] [$direction] $commandName -> $endpoint ($byteCount bytes) | Status: $status | Data: $hexPreview | $detail"
    }
}
