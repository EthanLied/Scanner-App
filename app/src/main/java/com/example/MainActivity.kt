package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.export.DocumentExporter
import com.example.data.model.ScannedPage
import com.example.protocol.ProtocolLogger
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.ExportDialog
import com.example.ui.screens.ExportFormat
import com.example.ui.screens.HardwareSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PageViewerDialog
import com.example.ui.screens.ProtocolLogScreen
import com.example.ui.screens.SaveExportScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // SAF Document creation contracts
    private var pendingPdfPages: List<ScannedPage> = emptyList()
    private val createPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && pendingPdfPages.isNotEmpty()) {
            viewModel.exportToPdf(this, pendingPdfPages, uri) { success ->
                if (success) {
                    Toast.makeText(this, "Combined PDF saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var pendingPngPage: ScannedPage? = null
    private val createPngLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        val page = pendingPngPage
        if (uri != null && page != null) {
            viewModel.exportToPng(this, page, uri) { success ->
                if (success) {
                    Toast.makeText(this, "PNG image saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to save PNG", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var pendingDirectoryPages: List<ScannedPage> = emptyList()
    private val openDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && pendingDirectoryPages.isNotEmpty()) {
            val pagesToExport = pendingDirectoryPages
            viewModel.exportPagesToDirectory(this, pagesToExport, uri) { count ->
                if (count > 0) {
                    Toast.makeText(this, "$count PNG images saved to folder!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to save PNG images", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    private fun sharePdf(pages: List<ScannedPage>) {
        if (pages.isEmpty()) return
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Preparing PDF to share...", Toast.LENGTH_SHORT).show()
            val uri = DocumentExporter.prepareSharePdf(this@MainActivity, pages)
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Scanned PDF"))
            } else {
                Toast.makeText(this@MainActivity, "Failed to prepare PDF for sharing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sharePngs(pages: List<ScannedPage>) {
        if (pages.isEmpty()) return
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Preparing pictures to share...", Toast.LENGTH_SHORT).show()
            val uris = DocumentExporter.prepareSharePngs(this@MainActivity, pages)
            if (uris.size == 1) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Scanned Picture"))
            } else if (uris.size > 1) {
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/png"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Scanned Pictures"))
            } else {
                Toast.makeText(this@MainActivity, "Failed to prepare pictures for sharing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide the system navigation bar (tab, home, back buttons) in transient swipe mode so it doesn't block the bottom
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    viewModel = viewModel,
                    onExportPdf = { pages ->
                        pendingPdfPages = pages
                        val defaultFilename = "Scan_${System.currentTimeMillis()}.pdf"
                        createPdfLauncher.launch(defaultFilename)
                    },
                    onExportPng = { pages ->
                        if (pages.size == 1) {
                            val page = pages.first()
                            pendingPngPage = page
                            val defaultFilename = "Scan_Page_${page.pageNumber}_${System.currentTimeMillis()}.png"
                            createPngLauncher.launch(defaultFilename)
                        } else if (pages.size > 1) {
                            pendingDirectoryPages = pages
                            Toast.makeText(this, "Select a folder to save ${pages.size} PNG images", Toast.LENGTH_SHORT).show()
                            openDirectoryLauncher.launch(null)
                        }
                    },
                    onSharePdf = { pages -> sharePdf(pages) },
                    onSharePng = { pages -> sharePngs(pages) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onExportPdf: (List<ScannedPage>) -> Unit,
    onExportPng: (List<ScannedPage>) -> Unit,
    onSharePdf: (List<ScannedPage>) -> Unit,
    onSharePng: (List<ScannedPage>) -> Unit
) {
    val context = LocalContext.current

    val activeTab by viewModel.activeTab.collectAsState()
    val activePrinter by viewModel.activePrinter.collectAsState()
    val scanSettings by viewModel.scanSettings.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val lastError by viewModel.lastScanError.collectAsState()
    val session by viewModel.session.collectAsState()
    val historyList by viewModel.historyList.collectAsState(initial = emptyList())
    val purgeExpiryDays by viewModel.purgeExpiryDays.collectAsState()
    val discoveredPrinters by viewModel.discoveredPrinters.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSsid by viewModel.wifiSsid.collectAsState()
    val diagnosticResults by viewModel.diagnosticResults.collectAsState()
    val isRunningDiagnostics by viewModel.isRunningDiagnostics.collectAsState()
    val printerHistory by viewModel.printerHistory.collectAsState()

    var isSettingsScreenOpen by rememberSaveable { mutableStateOf(false) }
    var isSaveScreenOpen by rememberSaveable { mutableStateOf(false) }
    var viewingPage by remember { mutableStateOf<ScannedPage?>(null) }

    val logCount by ProtocolLogger.logsFlow.collectAsState()

    if (isSettingsScreenOpen) {
        HardwareSettingsScreen(
            currentSettings = scanSettings,
            activePrinter = activePrinter,
            onNavigateBack = { isSettingsScreenOpen = false },
            onSave = { newSettings ->
                viewModel.updateSettings(newSettings)
                isSettingsScreenOpen = false
            }
        )
    } else if (isSaveScreenOpen) {
        SaveExportScreen(
            allPages = session.pages,
            onNavigateBack = { isSaveScreenOpen = false },
            onSaveToDevice = { format, selectedPages ->
                isSaveScreenOpen = false
                when (format) {
                    ExportFormat.COMBINED_PDF -> onExportPdf(selectedPages)
                    ExportFormat.SEPARATE_PNG -> onExportPng(selectedPages)
                }
            },
            onShare = { format, selectedPages ->
                isSaveScreenOpen = false
                when (format) {
                    ExportFormat.COMBINED_PDF -> onSharePdf(selectedPages)
                    ExportFormat.SEPARATE_PNG -> onSharePng(selectedPages)
                }
            }
        )
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Canon PIXMA G3010",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Quick printer status badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (activePrinter != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { viewModel.setActiveTab(MainTab.CONNECT) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activePrinter != null) StatusGreen else StatusRed)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = activePrinter?.let { "${it.ip}" } ?: "Not Connected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                NavigationBarItem(
                    selected = activeTab == MainTab.SCAN,
                    onClick = { viewModel.setActiveTab(MainTab.SCAN) },
                    icon = {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan")
                    },
                    label = { Text("Scan") },
                    modifier = Modifier.testTag("nav_tab_scan")
                )

                NavigationBarItem(
                    selected = activeTab == MainTab.HISTORY,
                    onClick = { viewModel.setActiveTab(MainTab.HISTORY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (historyList.isNotEmpty()) {
                                    Badge { Text("${historyList.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_tab_history")
                )

                NavigationBarItem(
                    selected = activeTab == MainTab.CONNECT,
                    onClick = { viewModel.setActiveTab(MainTab.CONNECT) },
                    icon = {
                        Icon(Icons.Default.Print, contentDescription = "Connect")
                    },
                    label = { Text("Connect") },
                    modifier = Modifier.testTag("nav_tab_connect")
                )

                NavigationBarItem(
                    selected = activeTab == MainTab.LOGS,
                    onClick = { viewModel.setActiveTab(MainTab.LOGS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (logCount.isNotEmpty()) {
                                    Badge { Text("${logCount.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "Protocol Log")
                        }
                    },
                    label = { Text("Protocol Log") },
                    modifier = Modifier.testTag("nav_tab_logs")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                MainTab.SCAN -> {
                    ScanScreen(
                        activePrinter = activePrinter,
                        scanSettings = scanSettings,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        lastError = lastError,
                        pages = session.pages,
                        onTriggerScan = { viewModel.triggerScan(context) },
                        onOpenSettings = { isSettingsScreenOpen = true },
                        onOpenExport = { isSaveScreenOpen = true },
                        onPageClick = { viewingPage = it },
                        onDeletePage = { pageId -> viewModel.deletePage(pageId) },
                        onMovePageUp = { index -> viewModel.movePageUp(index) },
                        onMovePageDown = { index -> viewModel.movePageDown(index) },
                        onClearSession = { viewModel.clearSession() },
                        onDismissError = { viewModel.clearError() }
                    )
                }

                MainTab.HISTORY -> {
                    HistoryScreen(
                        historyList = historyList,
                        purgeExpiryDays = purgeExpiryDays,
                        onSetPurgeExpiryDays = { days -> viewModel.setPurgeExpiryDays(days) },
                        onRestoreScan = { scanId ->
                            viewModel.loadHistoryScanToCurrentSession(scanId) {
                                Toast.makeText(context, "Loaded $scanId into scan session", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteScan = { scanId ->
                            viewModel.deleteHistoryScan(scanId)
                        }
                    )
                }

                MainTab.CONNECT -> {
                    ConnectScreen(
                        discoveredPrinters = discoveredPrinters,
                        activePrinter = activePrinter,
                        savedPrinters = printerHistory,
                        isDiscovering = isDiscovering,
                        isWifiConnected = isWifiConnected,
                        wifiSsid = wifiSsid,
                        diagnosticResults = diagnosticResults,
                        isRunningDiagnostics = isRunningDiagnostics,
                        onSelectPrinter = { printer ->
                            viewModel.selectPrinter(printer)
                            viewModel.setActiveTab(MainTab.SCAN)
                        },
                        onAddManualIp = { ip ->
                            viewModel.addManualPrinter(ip)
                            viewModel.setActiveTab(MainTab.SCAN)
                        },
                        onRefreshDiscovery = { viewModel.startDiscovery() },
                        onRunDiagnostics = { ip -> viewModel.runDiagnostics(ip) }
                    )
                }

                MainTab.LOGS -> {
                    ProtocolLogScreen()
                }
            }
        }
    }
    }

    viewingPage?.let { page ->
        PageViewerDialog(
            page = page,
            onDismiss = { viewingPage = null },
            onDelete = {
                viewModel.deletePage(page.id)
                viewingPage = null
            }
        )
    }
}

