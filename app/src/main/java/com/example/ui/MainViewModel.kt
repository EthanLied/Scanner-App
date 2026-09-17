package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PixmaScannerApp
import com.example.data.export.DocumentExporter
import com.example.data.model.PrinterDevice
import com.example.data.model.ScanDpi
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings
import com.example.data.model.ScannerModelRegistry
import com.example.data.model.ScannedPage
import com.example.data.model.ScanSession
import com.example.protocol.ProbeResult
import com.example.protocol.ProtocolLogger
import com.example.protocol.ScanProgress
import com.example.service.ScanForegroundService
import kotlinx.coroutines.Dispatchers
import com.example.data.history.PrinterHistoryPreferences
import com.example.data.history.SavedPrinterHistoryItem
import com.example.data.history.ScanHistoryEntity
import com.example.util.CrashLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    SCAN,
    HISTORY,
    CONNECT,
    LOGS
}

class MainViewModel : ViewModel() {
    private val app = PixmaScannerApp.instance
    private val wifiNetworkManager = app.wifiNetworkManager
    private val sessionManager = app.sessionManager
    private val discoveryManager = app.discoveryManager
    private val fallbackLadder = app.fallbackLadder
    private val historyRepository = app.historyRepository
    private val database = app.database
    val printerHistoryPrefs = PrinterHistoryPreferences(app)

    val session: StateFlow<ScanSession> = sessionManager.currentSession
    val historyList: Flow<List<ScanHistoryEntity>> = historyRepository.allHistory
    val purgeExpiryDays: StateFlow<Int> = historyRepository.purgeExpiryDays
    val crashLogs: StateFlow<String> = CrashLogger.crashLogsFlow
    val discoveredPrinters: StateFlow<List<PrinterDevice>> = discoveryManager.discoveredPrinters
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isDiscovering
    val isWifiConnected: StateFlow<Boolean> = wifiNetworkManager.isWifiConnected
    val wifiSsid: StateFlow<String?> = wifiNetworkManager.wifiSsid

    private val _activePrinter = MutableStateFlow<PrinterDevice?>(null)
    val activePrinter: StateFlow<PrinterDevice?> = _activePrinter.asStateFlow()

    private val _printerHistory = MutableStateFlow<List<SavedPrinterHistoryItem>>(emptyList())
    val printerHistory: StateFlow<List<SavedPrinterHistoryItem>> = _printerHistory.asStateFlow()

    private val _scanSettings = MutableStateFlow(ScanSettings())
    val scanSettings: StateFlow<ScanSettings> = _scanSettings.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    private val _lastScanError = MutableStateFlow<String?>(null)
    val lastScanError: StateFlow<String?> = _lastScanError.asStateFlow()

    private val _diagnosticResults = MutableStateFlow<List<ProbeResult>>(emptyList())
    val diagnosticResults: StateFlow<List<ProbeResult>> = _diagnosticResults.asStateFlow()

    private val _isRunningDiagnostics = MutableStateFlow(false)
    val isRunningDiagnostics: StateFlow<Boolean> = _isRunningDiagnostics.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.SCAN)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    init {
        // Load initial printer from history on background dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            val history = printerHistoryPrefs.getPrinterHistory()
            _printerHistory.value = history
            val last = printerHistoryPrefs.getLastConnectedPrinter()
            if (last != null) {
                val device = PrinterDevice(
                    model = last.model,
                    ip = last.lastIp,
                    port = last.port,
                    discoveryMethod = "History (Auto-Saved)",
                    isOnline = true
                )
                withContext(Dispatchers.Main) {
                    if (_activePrinter.value == null) {
                        _activePrinter.value = device
                        validateAndAdjustSettings(device)
                    }
                }
            }
        }

        // Start mDNS discovery
        startDiscovery()

        // Observe discovered printers and auto-reconnect to remembered printer model even if IP changed
        viewModelScope.launch(Dispatchers.Default) {
            discoveredPrinters.collectLatest { printers ->
                if (printers.isNotEmpty()) {
                    val matched = printerHistoryPrefs.findMatchingDiscoveredPrinter(printers)
                    if (matched != null) {
                        val current = _activePrinter.value
                        if (current == null || current.ip != matched.ip || current.model != matched.model) {
                            withContext(Dispatchers.Main) {
                                _activePrinter.value = matched
                                validateAndAdjustSettings(matched)
                            }
                            printerHistoryPrefs.saveConnectedPrinter(matched)
                            _printerHistory.value = printerHistoryPrefs.getPrinterHistory()
                        }
                    } else if (_activePrinter.value == null && printers.isNotEmpty()) {
                        val first = printers.first()
                        withContext(Dispatchers.Main) {
                            if (_activePrinter.value == null) {
                                _activePrinter.value = first
                                validateAndAdjustSettings(first)
                            }
                        }
                        printerHistoryPrefs.saveConnectedPrinter(first)
                        _printerHistory.value = printerHistoryPrefs.getPrinterHistory()
                    }
                }
            }
        }
    }

    private fun refreshHistoryList() {
        viewModelScope.launch(Dispatchers.IO) {
            _printerHistory.value = printerHistoryPrefs.getPrinterHistory()
        }
    }

    fun setActiveTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun startDiscovery() {
        discoveryManager.startDiscovery()
    }

    fun stopDiscovery() {
        discoveryManager.stopDiscovery()
    }

    fun selectPrinter(printer: PrinterDevice) {
        _activePrinter.value = printer
        _lastScanError.value = null
        validateAndAdjustSettings(printer)
        printerHistoryPrefs.saveConnectedPrinter(printer)
        refreshHistoryList()
        ProtocolLogger.log("SYS", "Connect", "Selected Printer", "", "OK", 0, "Selected & remembered ${printer.model} (${printer.ip})")
    }

    fun addManualPrinter(ip: String) {
        val printer = discoveryManager.addManualPrinter(ip)
        _activePrinter.value = printer
        _lastScanError.value = null
        validateAndAdjustSettings(printer)
        printerHistoryPrefs.saveConnectedPrinter(printer)
        refreshHistoryList()
        ProtocolLogger.log("SYS", "Connect", "Manual Printer", "", "OK", 0, "Connected & remembered manual printer at ${printer.ip}")
    }

    fun updateSettings(settings: ScanSettings) {
        val printer = _activePrinter.value
        val caps = ScannerModelRegistry.resolveCapabilities(printer?.model)
        val validation = ScannerModelRegistry.validate(settings, caps)
        _scanSettings.value = validation.adjustedSettings
        if (validation.warnings.isNotEmpty()) {
            ProtocolLogger.log(
                "SYS", "ModelValidation", "Settings Adjusted", "", "WARN", 0,
                "Auto-adjusted for ${caps.displayName}: ${validation.warnings.joinToString("; ")}"
            )
        }
    }

    private fun validateAndAdjustSettings(printer: PrinterDevice) {
        val caps = ScannerModelRegistry.resolveCapabilities(printer.model)
        val validation = ScannerModelRegistry.validate(_scanSettings.value, caps)
        if (!validation.isValid) {
            _scanSettings.value = validation.adjustedSettings
            ProtocolLogger.log(
                "SYS", "ModelValidation", "Settings Auto-Adjusted", "", "OK", 0,
                "Adjusted scan options for ${caps.displayName}: ${validation.warnings.joinToString("; ")}"
            )
        }
    }

    fun runDiagnostics(ip: String) {
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            _diagnosticResults.value = emptyList()
            try {
                val results = fallbackLadder.runFullDiagnosticLadder(ip)
                _diagnosticResults.value = results
            } catch (e: Exception) {
                Log.e("MainViewModel", "Diagnostics failed", e)
            } finally {
                _isRunningDiagnostics.value = false
            }
        }
    }

    /**
     * ONE TAP = ONE PAGE.
     * Connects, executes the full CHMP 13-step scan sequence,
     * writes JPEG straight to disk, updates session index before notifying UI.
     */
    fun triggerScan(context: Context) {
        val printer = _activePrinter.value
        if (printer == null) {
            _lastScanError.value = "No printer selected. Please select or enter a printer IP on the Connect screen."
            _activeTab.value = MainTab.CONNECT
            return
        }

        if (_isScanning.value) return
        _isScanning.value = true
        _lastScanError.value = null
        _scanProgress.value = ScanProgress("Starting", 0.01f, 0, 0, "Preparing scanner session...")

        ScanForegroundService.start(
            context,
            step = "Initializing",
            progress = 1,
            detail = "Connecting to ${printer.model} at ${printer.ip}"
        )

        viewModelScope.launch(Dispatchers.IO) {
            val targetFile = sessionManager.createTargetPageFile()
            val currentSettings = _scanSettings.value

            try {
                val result = fallbackLadder.rung1.scanPage(
                    ip = printer.ip,
                    settings = currentSettings,
                    destinationFile = targetFile,
                    progressListener = { progress ->
                        _scanProgress.value = progress
                        ScanForegroundService.update(
                            context,
                            step = progress.stepName,
                            progress = (progress.percentage * 100).toInt(),
                            detail = progress.statusDetail
                        )
                    }
                )

                if (result.success && result.file != null) {
                    // CRASH SAFETY: Page file is already written to disk.
                    // Now persist to session.json before emitting to UI.
                    sessionManager.recordNewPage(
                        file = result.file,
                        widthPx = result.widthPx,
                        heightPx = result.heightPx,
                        deliveredLines = result.deliveredLines,
                        dpi = currentSettings.dpi.value,
                        colorMode = currentSettings.colorMode.label,
                        pageSizeLabel = currentSettings.pageSize.label
                    )
                    _scanProgress.value = ScanProgress("Finished", 1.0f, result.file.length(), result.file.length(), "Page added to session!")
                } else {
                    val errorMsg = result.error ?: "Scan aborted: unknown protocol error"
                    _lastScanError.value = errorMsg
                    ProtocolLogger.log("SYS", printer.ip, "Scan Error", "", "FAIL", 0, errorMsg)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Scan exception", e)
                val errorMsg = "Scan failed: ${e.message ?: e.javaClass.simpleName}"
                _lastScanError.value = errorMsg
                ProtocolLogger.log("SYS", printer.ip, "Scan Exception", "", "EXCEPTION", 0, errorMsg)
            } finally {
                _isScanning.value = false
                ScanForegroundService.stop(context)
            }
        }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.deletePage(pageId) { filePath ->
                // Image can only be removed from user storage once no scan IDs depend on it
                database.scanHistoryDao().countReferencesToImage(filePath) == 0
            }
        }
    }

    fun movePageUp(index: Int) {
        if (index > 0) {
            viewModelScope.launch {
                sessionManager.reorderPages(index, index - 1)
            }
        }
    }

    fun movePageDown(index: Int) {
        val total = session.value.pages.size
        if (index < total - 1) {
            viewModelScope.launch {
                sessionManager.reorderPages(index, index + 1)
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.clearSession { filePath ->
                // Image can only be removed from user storage once no scan IDs depend on it
                database.scanHistoryDao().countReferencesToImage(filePath) == 0
            }
        }
    }

    fun setPurgeExpiryDays(days: Int) {
        historyRepository.setPurgeExpiryDays(days)
    }

    fun loadHistoryScanToCurrentSession(scanId: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pages = historyRepository.getPagesForScan(scanId)
                sessionManager.loadPagesFromHistory(pages)
                withContext(Dispatchers.Main) {
                    setActiveTab(MainTab.SCAN)
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed restoring scan from history", e)
                CrashLogger.logNonFatal("MainViewModel", "Failed restoring scan $scanId", e)
            }
        }
    }

    fun deleteHistoryScan(scanId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.deleteScan(scanId, sessionManager.getActivePagePaths())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed deleting history scan", e)
                CrashLogger.logNonFatal("MainViewModel", "Failed deleting scan $scanId", e)
            }
        }
    }

    fun exportToPdf(context: Context, pages: List<ScannedPage>, targetUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val os = context.contentResolver.openOutputStream(targetUri)
                if (os == null) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }
                val success = DocumentExporter.exportToCombinedPdf(context, pages, os)
                if (success) {
                    // Record in history: automatic deduplication if identical collection of images
                    historyRepository.recordExport(pages)
                }
                withContext(Dispatchers.Main) { onComplete(success) }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "PDF export error: ${t.message}", t)
                CrashLogger.logNonFatal("MainViewModel", "PDF export error", t)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun exportToPng(context: Context, page: ScannedPage, targetUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val os = context.contentResolver.openOutputStream(targetUri)
                if (os == null) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }
                val success = DocumentExporter.exportPageToPng(page, os)
                if (success) {
                    historyRepository.recordExport(listOf(page))
                }
                withContext(Dispatchers.Main) { onComplete(success) }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "PNG export error: ${t.message}", t)
                CrashLogger.logNonFatal("MainViewModel", "PNG export error", t)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun exportPagesToDirectory(
        context: Context,
        pages: List<ScannedPage>,
        treeUri: Uri,
        onComplete: (exportedCount: Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = DocumentExporter.exportPagesToTreeDirectory(context, pages, treeUri)
                if (count > 0) {
                    val exportedPages = pages.take(count)
                    historyRepository.recordExport(exportedPages)
                }
                withContext(Dispatchers.Main) { onComplete(count) }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "Directory PNG export error: ${t.message}", t)
                CrashLogger.logNonFatal("MainViewModel", "Directory PNG export error", t)
                withContext(Dispatchers.Main) { onComplete(0) }
            }
        }
    }

    fun clearCrashLogs() {
        CrashLogger.clearLogs()
    }

    fun clearError() {
        _lastScanError.value = null
    }
}
