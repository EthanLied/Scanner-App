package com.example.protocol

import com.example.data.model.ProtocolLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

object ProtocolLogger {
    private val counter = AtomicLong(0)
    private val entries = mutableListOf<ProtocolLogEntry>()
    private val _logsFlow = MutableStateFlow<List<ProtocolLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<ProtocolLogEntry>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(
        direction: String,
        endpoint: String,
        commandName: String,
        hexPreview: String,
        status: String,
        byteCount: Int,
        detail: String = ""
    ) {
        val entry = ProtocolLogEntry(
            id = counter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            direction = direction,
            endpoint = endpoint,
            commandName = commandName,
            hexPreview = hexPreview,
            status = status,
            byteCount = byteCount,
            detail = detail
        )
        entries.add(entry)
        // Keep last 500 entries in memory
        if (entries.size > 500) {
            entries.removeAt(0)
        }
        _logsFlow.value = entries.toList()
    }

    fun formatHex(bytes: ByteArray, maxBytes: Int = 32): String {
        val count = Math.min(bytes.size, maxBytes)
        val sb = StringBuilder()
        for (i in 0 until count) {
            sb.append(String.format("%02x ", bytes[i]))
        }
        if (bytes.size > maxBytes) {
            sb.append("... (${bytes.size} bytes total)")
        }
        return sb.toString().trim()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        _logsFlow.value = emptyList()
    }

    @Synchronized
    fun getAllLogsAsText(): String {
        val sb = StringBuilder()
        sb.append("=== CANON PIXMA G3010 CHMP PROTOCOL LOG ===\n")
        sb.append("Generated at: ").append(java.util.Date()).append("\n\n")
        for (entry in entries) {
            sb.append(entry.toFormattedString()).append("\n")
        }
        return sb.toString()
    }
}
