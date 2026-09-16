package com.example.data.history

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PrinterDevice
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SavedPrinterHistoryItem(
    val model: String,
    val lastIp: String,
    val port: Int = 80,
    val discoveryMethod: String = "Auto-Reconnect",
    val lastConnectedTimestamp: Long = System.currentTimeMillis()
)

class PrinterHistoryPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pixma_printer_history_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SavedPrinterHistoryItem::class.java)
    private val adapter = moshi.adapter<List<SavedPrinterHistoryItem>>(listType)

    fun saveConnectedPrinter(printer: PrinterDevice) {
        val currentList = getPrinterHistory().toMutableList()
        // Remove existing item with same model or same IP
        currentList.removeAll { it.model.equals(printer.model, ignoreCase = true) || it.lastIp == printer.ip }
        // Insert at beginning as most recent
        val newItem = SavedPrinterHistoryItem(
            model = printer.model,
            lastIp = printer.ip,
            port = printer.port,
            discoveryMethod = printer.discoveryMethod,
            lastConnectedTimestamp = System.currentTimeMillis()
        )
        currentList.add(0, newItem)

        val json = try {
            adapter.toJson(currentList.take(10))
        } catch (e: Exception) {
            "[]"
        }

        prefs.edit()
            .putString(KEY_LAST_MODEL, printer.model)
            .putString(KEY_LAST_IP, printer.ip)
            .putInt(KEY_LAST_PORT, printer.port)
            .putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_HISTORY_JSON, json)
            .apply()
    }

    fun getLastConnectedPrinter(): SavedPrinterHistoryItem? {
        val model = prefs.getString(KEY_LAST_MODEL, null) ?: return null
        val ip = prefs.getString(KEY_LAST_IP, "192.168.1.1") ?: "192.168.1.1"
        val port = prefs.getInt(KEY_LAST_PORT, 80)
        val timestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
        return SavedPrinterHistoryItem(model = model, lastIp = ip, port = port, lastConnectedTimestamp = timestamp)
    }

    fun getPrinterHistory(): List<SavedPrinterHistoryItem> {
        val json = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findMatchingDiscoveredPrinter(discoveredPrinters: List<PrinterDevice>): PrinterDevice? {
        val lastPrinter = getLastConnectedPrinter() ?: return null

        // 1. First priority: exact IP match if available
        val exactIpMatch = discoveredPrinters.firstOrNull { it.ip == lastPrinter.lastIp }
        if (exactIpMatch != null) return exactIpMatch

        // 2. Second priority: model name match (even if DHCP changed its IP!)
        val modelMatch = discoveredPrinters.firstOrNull { isModelMatching(it.model, lastPrinter.model) }
        if (modelMatch != null) return modelMatch

        // 3. Fallback: check any in history list
        val history = getPrinterHistory()
        for (item in history) {
            val match = discoveredPrinters.firstOrNull {
                it.ip == item.lastIp || isModelMatching(it.model, item.model)
            }
            if (match != null) return match
        }

        return null
    }

    private fun isModelMatching(modelA: String, modelB: String): Boolean {
        if (modelA.equals(modelB, ignoreCase = true)) return true
        val cleanA = modelA.replace(" ", "").lowercase()
        val cleanB = modelB.replace(" ", "").lowercase()
        if (cleanA.contains("g3010") && cleanB.contains("g3010")) return true
        if (cleanA.contains("g2010") && cleanB.contains("g2010")) return true
        if (cleanA.contains("ts3100") && cleanB.contains("ts3100")) return true
        if (cleanA.contains("mg3600") && cleanB.contains("mg3600")) return true
        return cleanA.contains(cleanB) || cleanB.contains(cleanA)
    }

    companion object {
        private const val KEY_LAST_MODEL = "last_connected_model"
        private const val KEY_LAST_IP = "last_connected_ip"
        private const val KEY_LAST_PORT = "last_connected_port"
        private const val KEY_LAST_TIMESTAMP = "last_connected_timestamp"
        private const val KEY_HISTORY_JSON = "printer_history_json"
    }
}
