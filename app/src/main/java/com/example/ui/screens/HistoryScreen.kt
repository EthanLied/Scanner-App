package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.history.ScanHistoryEntity
import com.example.util.CrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<ScanHistoryEntity>,
    purgeExpiryDays: Int,
    onSetPurgeExpiryDays: (Int) -> Unit,
    onRestoreScan: (String) -> Unit,
    onDeleteScan: (String) -> Unit
) {
    var scanToRestore by remember { mutableStateOf<ScanHistoryEntity?>(null) }
    var scanToDelete by remember { mutableStateOf<ScanHistoryEntity?>(null) }
    var showExpiryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved Scans", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${historyList.size} scan(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showExpiryDialog = true },
                        label = {
                            Text(
                                if (purgeExpiryDays > 0) "Keep: ${purgeExpiryDays} days" else "Keep forever",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoDelete, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("configure_purge_expiry_chip")
                    )
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isWide = maxWidth >= 600.dp
            val horizontalPadding = if (maxWidth < 360.dp) 10.dp else if (isWide) 24.dp else 16.dp

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No Scans Saved Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "When you save or share a scan, a copy will appear here so you can view it anytime.\n\nOld scans are automatically cleaned up after $purgeExpiryDays days to save space.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList, key = { it.scanId }) { item ->
                        HistoryItemCard(
                            history = item,
                            onClick = { scanToRestore = item },
                            onDelete = { scanToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog before overriding current scan session
    scanToRestore?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Open This Saved Scan?") },
            text = {
                Text(
                    "This will load the ${scan.itemCount} pages from this scan onto your main screen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = scan.scanId
                        scanToRestore = null
                        onRestoreScan(id)
                    },
                    modifier = Modifier.testTag("confirm_override_session_button")
                ) {
                    Text("Open Scan")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    scanToDelete?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Saved Scan?") },
            text = {
                Text(
                    "Are you sure you want to delete this saved scan?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = scan.scanId
                        scanToDelete = null
                        onDeleteScan(id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_history_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { scanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Purge expiry configuration dialog
    if (showExpiryDialog) {
        PurgeExpiryDialog(
            currentDays = purgeExpiryDays,
            onDismiss = { showExpiryDialog = false },
            onSelectDays = { days ->
                onSetPurgeExpiryDays(days)
                showExpiryDialog = false
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    history: ScanHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    LaunchedEffect(history.latestImageFilePath) {
        thumbnailBitmap = withContext(Dispatchers.IO) {
            val file = File(history.latestImageFilePath)
            if (file.exists()) {
                CrashLogger.decodeSampledBitmap(file.absolutePath, 160, 160)
            } else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${history.scanId}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Thumbnail for ${history.scanId}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    history.scanId,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "${history.itemCount} page${if (history.itemCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val kb = history.totalSizeBytes / 1024
                    val sizeLabel = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
                    Text(
                        sizeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "Created: ${dateFormat.format(Date(history.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (history.updatedAt != history.createdAt) {
                    Text(
                        "Updated: ${dateFormat.format(Date(history.updatedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_history_${history.scanId}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete from history",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PurgeExpiryDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onSelectDays: (Int) -> Unit
) {
    val options = listOf(
        7 to "7 Days",
        14 to "14 Days (Default)",
        30 to "30 Days",
        60 to "60 Days",
        90 to "90 Days",
        0 to "Never Purge"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How Long to Keep Scans") },
        text = {
            Column {
                Text(
                    "Old scans will be automatically deleted after the chosen time to save storage space on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { (days, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDays(days) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentDays == days),
                            onClick = { onSelectDays(days) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
