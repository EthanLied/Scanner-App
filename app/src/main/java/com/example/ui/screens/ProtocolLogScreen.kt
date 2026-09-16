package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProtocolLogEntry
import com.example.protocol.ProtocolLogger
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.CrashLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProtocolLogScreen() {
    val context = LocalContext.current
    val logs by ProtocolLogger.logsFlow.collectAsState()
    val crashLogs by CrashLogger.crashLogsFlow.collectAsState()
    val isDebugModeEnabled by ProtocolLogger.isDebugModeEnabled.collectAsState()
    val listState = rememberLazyListState()
    var selectedLogTab by remember { mutableIntStateOf(0) }
    
    // Advanced mode toggle: Defaults to false (Simple mode for 60+ seniors and non-technical users)
    var isAdvancedMode by rememberSaveable { mutableStateOf(false) }

    // Auto-scroll to bottom on new log in protocol tab
    LaunchedEffect(logs.size, selectedLogTab) {
        if (selectedLogTab == 0 && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val horizontalPadding = if (maxWidth < 360.dp) 10.dp else if (isWide) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Mode & Tabs Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedLogTab,
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedLogTab == 0,
                        onClick = { selectedLogTab = 0 },
                        text = {
                            Text(
                                if (isAdvancedMode) {
                                    "Wire Log (${logs.size})"
                                } else {
                                    "Activity (${logs.size})"
                                }
                            )
                        },
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedLogTab == 1,
                        onClick = { selectedLogTab = 1 },
                        text = {
                            Text(
                                if (isAdvancedMode) {
                                    "Crash & Error Log"
                                } else {
                                    "App Health"
                                }
                            )
                        },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                    )
                }

                Spacer(Modifier.width(8.dp))

                // The "Advanced" Toggle Button requested by user
                OutlinedButton(
                    onClick = { isAdvancedMode = !isAdvancedMode },
                    colors = if (isAdvancedMode) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAdvancedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("advanced_mode_button")
                ) {
                    Icon(
                        imageVector = if (isAdvancedMode) Icons.Default.Code else Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isAdvancedMode) "Advanced: ON" else "Advanced",
                        fontWeight = if (isAdvancedMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Simple Mode Informational Banner (concise, clear reassurance)
            AnimatedVisibility(visible = !isAdvancedMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Simple mode active: Showing easy-to-read actions. Tap 'Advanced' to see technician data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (selectedLogTab == 0) {
                // Recording / Debug Mode toggle card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDebugModeEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                if (isAdvancedMode) "Debug Wire Capture" else "Record Scanner Activity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isAdvancedMode) {
                                    if (isDebugModeEnabled) {
                                        "Active: capturing raw wire protocol packets on port 80"
                                    } else {
                                        "Off: logging is disabled to conserve battery and memory"
                                    }
                                } else {
                                    if (isDebugModeEnabled) {
                                        "Active: keeping a record of scanner actions"
                                    } else {
                                        "Off: recording is paused to save battery"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isDebugModeEnabled,
                            onCheckedChange = { ProtocolLogger.setDebugMode(it) },
                            modifier = Modifier.testTag("debug_mode_switch")
                        )
                    }
                }

                // Log Tab Header & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (isAdvancedMode) "CHMP Wire Protocol Log" else "Recent Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isAdvancedMode) {
                                "${logs.size} entries recorded (Port 80 HTTP POST/GET)"
                            } else {
                                "${logs.size} recent scanner events"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        Button(
                            onClick = {
                                val text = if (isAdvancedMode) {
                                    ProtocolLogger.getAllLogsAsText()
                                } else {
                                    logs.joinToString("\n") { log ->
                                        val info = getSimpleLogInfo(log)
                                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                        "[$timeStr] ${info.directionLabel}: ${info.title} - ${info.description} (${info.statusText})"
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scanner Activity Log", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Activity copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_logs_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { ProtocolLogger.clear() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_logs_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                        }
                    }
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isAdvancedMode) {
                                if (!isDebugModeEnabled) {
                                    "Protocol logging is currently disabled.\nTurn on 'Debug Wire Capture' above to record and inspect live CHMP wire packets."
                                } else {
                                    "Debug mode active.\nProbe a printer or tap 'Scan' to view live CHMP wire packets."
                                }
                            } else {
                                if (!isDebugModeEnabled) {
                                    "Activity recording is paused.\nTurn on 'Record Scanner Activity' above to see live updates when you scan."
                                } else {
                                    "Ready to scan!\nTap 'Scan' on the main screen to see what the scanner does here."
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            ProtocolLogCard(log = log, isAdvanced = isAdvancedMode)
                        }
                    }
                }
            } else {
                // Persistent Crash & Error Log Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (isAdvancedMode) "Crash & Exception Log" else "App Health History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isAdvancedMode) {
                                "Persistent crash history across app restarts"
                            } else {
                                "Check if the app encountered any technical problems"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Crash Log", crashLogs)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            enabled = crashLogs.isNotEmpty(),
                            modifier = Modifier.testTag("copy_crash_logs_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { CrashLogger.clearLogs() },
                            enabled = crashLogs.isNotEmpty(),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_crash_logs_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear crash logs")
                        }
                    }
                }

                if (crashLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                if (isAdvancedMode) "No Crashes Recorded" else "App is Running Normally",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isAdvancedMode) {
                                    "Crash stack traces, memory statistics, and unhandled errors are automatically logged here to aid debugging."
                                } else {
                                    "No problems or errors have been recorded. Everything is healthy!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    if (isAdvancedMode) {
                        // Technical stack trace terminal card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
                        ) {
                            val scrollState = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = crashLogs,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFFFFB4A2)
                                )
                            }
                        }
                    } else {
                        // Simple, non-intimidating notice for seniors and non-technical users
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    "An Issue Was Recorded",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "The scanner ran into a temporary problem earlier. If someone is helping you fix it, tap 'Advanced' to show the technical details.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isAdvancedMode = true }
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Show Advanced Details")
                                    }
                                    Button(
                                        onClick = { CrashLogger.clearLogs() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("Clear Notice")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data structure holding simple, non-technical plain English explanations.
 */
data class SimpleLogInfo(
    val title: String,
    val description: String,
    val statusText: String,
    val directionLabel: String
)

/**
 * Converts raw protocol events into concise, friendly, plain-English sentences
 * easily understood by seniors (60+) or teenagers (15-year-olds).
 */
fun getSimpleLogInfo(log: ProtocolLogEntry): SimpleLogInfo {
    val dirLabel = when (log.direction) {
        "TX" -> "Sent"
        "RX" -> "Received"
        "SYS" -> "Notice"
        "CRASH", "ERROR" -> "Alert"
        else -> "Notice"
    }

    val statusText = when {
        log.status in listOf("200 OK", "OK", "0x00", "0x00 OK") -> "Success"
        log.status.contains("DATA READY", ignoreCase = true) -> "Page Ready"
        log.status.contains("Starting", ignoreCase = true) || log.status in listOf("POST", "GET", "START") -> "Working"
        log.status.contains("FAIL", ignoreCase = true) || log.status.contains("EXCEPTION", ignoreCase = true) ||
                log.status.contains("CRASH", ignoreCase = true) || log.status.contains("FATAL", ignoreCase = true) ||
                log.status.contains("ERROR", ignoreCase = true) -> "Problem"
        log.status.contains("TIMEOUT", ignoreCase = true) -> "No Answer"
        else -> "Done"
    }

    val (title, description) = when {
        // Discovery / Network
        log.commandName.contains("Discovery", ignoreCase = true) || log.endpoint == "mDNS" -> {
            if (log.detail.contains("Found", ignoreCase = true)) {
                "Printer Found" to "Located your printer on Wi-Fi."
            } else if (log.detail.contains("Stop", ignoreCase = true)) {
                "Search Finished" to "Finished looking for printers."
            } else {
                "Searching Wi-Fi" to "Looking for nearby printers."
            }
        }
        log.commandName.contains("Add Printer", ignoreCase = true) || log.commandName.contains("Select Printer", ignoreCase = true) -> {
            "Printer Connected" to "Connected to the selected scanner."
        }
        // Probe / Connection
        log.commandName.contains("Probe", ignoreCase = true) -> {
            if (statusText == "Success" || log.detail.contains("ready", ignoreCase = true) || log.detail.contains("Shift", ignoreCase = true)) {
                "Printer Ready" to "Scanner is online and ready."
            } else if (statusText == "Problem" || statusText == "No Answer") {
                "Cannot Reach Printer" to "Make sure your printer is powered on and on the same Wi-Fi."
            } else {
                "Checking Scanner" to "Checking if the scanner is ready."
            }
        }
        // Scan Commands
        log.commandName.contains("Scan", ignoreCase = true) && log.direction == "TX" -> {
            "Starting Scan" to "Told the scanner to begin scanning."
        }
        log.commandName.contains("Status", ignoreCase = true) -> {
            if (log.status.contains("DATA READY", ignoreCase = true) || log.detail.contains("ready", ignoreCase = true)) {
                "Page Scanned" to "Scanning is done. Downloading image now."
            } else {
                "Scanning Page" to "The scanner is currently scanning your document."
            }
        }
        log.commandName.contains("ReadImageData", ignoreCase = true) || log.commandName.contains("Image", ignoreCase = true) -> {
            "Downloading Image" to "Receiving scanned page into the app."
        }
        log.commandName.contains("Dimensions", ignoreCase = true) -> {
            "Scan Completed" to "The page was scanned successfully."
        }
        log.commandName.contains("Cancel", ignoreCase = true) -> {
            "Scan Cancelled" to "Stopped the scan operation."
        }
        // Fallback / Other modes
        log.commandName.contains("BJNP", ignoreCase = true) || log.commandName.contains("eSCL", ignoreCase = true) || log.commandName.contains("USB", ignoreCase = true) -> {
            "Scanner Check" to "Verified printer communication method."
        }
        // Error
        statusText == "Problem" -> {
            "Notice" to if (log.detail.isNotBlank() && !log.detail.contains("Exception", ignoreCase = true)) {
                log.detail.take(50)
            } else {
                "Printer needs attention or is busy."
            }
        }
        else -> {
            val simpleTitle = when (log.direction) {
                "TX" -> "Sent Command"
                "RX" -> "Scanner Reply"
                else -> "App Action"
            }
            val simpleDesc = if (log.detail.isNotBlank() && !log.detail.startsWith("<") && !log.detail.startsWith("{")) {
                log.detail.take(50)
            } else {
                "Completed action successfully."
            }
            simpleTitle to simpleDesc
        }
    }

    return SimpleLogInfo(
        title = title,
        description = description,
        statusText = statusText,
        directionLabel = dirLabel
    )
}

@Composable
fun ProtocolLogCard(log: ProtocolLogEntry, isAdvanced: Boolean) {
    if (isAdvanced) {
        // Full Technical View for Technicians/Developers
        val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
        val timeStr = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

        val dirColor = when (log.direction) {
            "TX" -> SoftCyan
            "RX" -> StatusGreen
            "SYS" -> StatusAmber
            else -> MaterialTheme.colorScheme.primary
        }

        val statusColor = when (log.status) {
            "200 OK", "OK" -> StatusGreen
            "FAILED", "EXCEPTION", "CRASH" -> StatusRed
            else -> StatusAmber
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(dirColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                log.direction,
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            log.commandName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            log.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            timeStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    "Endpoint: ${log.endpoint}  |  Payload: ${log.byteCount} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )

                if (log.hexPreview.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Hex: ${log.hexPreview}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                if (log.detail.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        log.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Simple, Concise, Non-Technical View for Seniors (60+) & Teenagers
        val simpleInfo = remember(log) { getSimpleLogInfo(log) }
        val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
        val timeStr = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

        val badgeColor = when (simpleInfo.directionLabel) {
            "Sent" -> SoftCyan
            "Received" -> StatusGreen
            else -> StatusAmber
        }

        val statusColor = when (simpleInfo.statusText) {
            "Success", "Done", "Page Ready" -> StatusGreen
            "Problem" -> StatusRed
            else -> MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small color pill for action direction
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            simpleInfo.directionLabel,
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = simpleInfo.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = simpleInfo.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = simpleInfo.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
