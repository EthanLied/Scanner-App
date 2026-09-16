package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PrinterDevice
import com.example.data.model.ScanSettings
import com.example.data.model.ScannedPage
import com.example.protocol.ScanProgress
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed
import com.example.util.CrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ScanScreen(
    activePrinter: PrinterDevice?,
    scanSettings: ScanSettings,
    isScanning: Boolean,
    scanProgress: ScanProgress?,
    lastError: String?,
    pages: List<ScannedPage>,
    onTriggerScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit,
    onPageClick: (ScannedPage) -> Unit,
    onDeletePage: (String) -> Unit,
    onMovePageUp: (Int) -> Unit,
    onMovePageDown: (Int) -> Unit,
    onClearSession: () -> Unit,
    onDismissError: () -> Unit
) {
    var showReorderDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Settings bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Resolution",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${scanSettings.dpi.value} DPI",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            "Color Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            scanSettings.colorMode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            "Page Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            scanSettings.pageSize.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    enabled = !isScanning,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("scan_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Scan Settings")
                }
            }
        }

        // Live Scanning Progress / Error Banner
        AnimatedVisibility(visible = lastError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastError ?: "",
                        color = StatusRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = StatusRed)
                    }
                }
            }
        }

        AnimatedVisibility(visible = isScanning) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            scanProgress?.stepName ?: "Scanning...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        scanProgress?.let {
                            Text(
                                "${(it.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { scanProgress?.percentage ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    scanProgress?.let {
                        if (it.totalBytesExpected > 0) {
                            Text(
                                "${it.bytesReceived / 1024} KB / ${it.totalBytesExpected / 1024} KB transferred",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Action Trigger Button: "Scan Page"
        Button(
            onClick = onTriggerScan,
            enabled = !isScanning && activePrinter != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("scan_page_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Column {
                    Text(
                        if (isScanning) "Scanning Page..." else "Scan Page",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        if (activePrinter != null) "Target: ${activePrinter.model} (${activePrinter.ip})" else "Tap to scan flatbed page",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Session Page Header and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Accumulated Pages (${pages.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (pages.size > 1) {
                    OutlinedButton(
                        onClick = { showReorderDialog = true },
                        enabled = !isScanning,
                        modifier = Modifier.testTag("reorder_pages_button")
                    ) {
                        Icon(Icons.Default.Reorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reorder")
                    }
                }

                if (pages.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOpenExport,
                        enabled = !isScanning,
                        modifier = Modifier.testTag("export_session_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export")
                    }

                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        enabled = !isScanning,
                        modifier = Modifier.testTag("clear_session_button")
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        // Thumbnail Grid of Scanned Pages
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No pages scanned yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Place document on the flatbed, close the lid, and tap 'Scan Page'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pages, key = { _, item -> item.id }) { index, page ->
                    PageThumbnailCard(
                        page = page,
                        index = index,
                        totalCount = pages.size,
                        onClick = { onPageClick(page) },
                        onDelete = { onDeletePage(page.id) },
                        onMoveUp = { onMovePageUp(index) },
                        onMoveDown = { onMovePageDown(index) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Current Session?") },
            text = { Text("This will remove all ${pages.size} accumulated pages from the current scan session.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearSession()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReorderDialog) {
        ReorderPagesDialog(
            pages = pages,
            onMoveUp = onMovePageUp,
            onMoveDown = onMovePageDown,
            onDeletePage = onDeletePage,
            onDismiss = { showReorderDialog = false }
        )
    }
}

@Composable
fun PageThumbnailCard(
    page: ScannedPage,
    index: Int,
    totalCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(page.filePath) {
        thumbnailBitmap = withContext(Dispatchers.IO) {
            val file = File(page.filePath)
            if (file.exists()) {
                // Safe sampled bitmap decoding to prevent OOM
                CrashLogger.decodeSampledBitmap(file.absolutePath, 200, 280)
            } else null
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("page_card_${page.pageNumber}")
    ) {
        Column {
            // Image Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f) // A4 ratio ~ 1:1.41
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Thumbnail Page ${page.pageNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }

                // Page Number Badge
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "Page ${page.pageNumber}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Info and controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
                    Text(
                        "${page.widthPx} × ${page.heightPx}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${page.fileSizeBytes / 1024} KB • ${page.dpi} DPI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("move_up_page_${page.pageNumber}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left/Up", modifier = Modifier.size(20.dp))
                        }
                    }

                    if (index < totalCount - 1) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("move_down_page_${page.pageNumber}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right/Down", modifier = Modifier.size(20.dp))
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("delete_page_${page.pageNumber}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = StatusRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Page ${page.pageNumber}?") },
            text = { Text("Are you sure you want to delete this page from the scan session?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReorderPagesDialog(
    pages: List<ScannedPage>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeletePage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Reorder Pages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages, key = { _, item -> item.id }) { index, page ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Page ${page.pageNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${page.widthPx} × ${page.heightPx} • ${page.dpi} DPI • ${page.fileSizeBytes / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onMoveUp(index) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                    }

                                    IconButton(
                                        onClick = { onMoveDown(index) },
                                        enabled = index < pages.size - 1,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                    }

                                    IconButton(
                                        onClick = { onDeletePage(page.id) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = StatusRed)
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
