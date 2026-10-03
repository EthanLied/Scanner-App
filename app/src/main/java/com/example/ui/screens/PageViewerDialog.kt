package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ScannedPage
import com.example.util.CrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PageViewerDialog(
    page: ScannedPage,
    allPages: List<ScannedPage> = listOf(page),
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val pageList = if (allPages.isNotEmpty()) allPages else listOf(page)
    val initialIndex = pageList.indexOfFirst { it.id == page.id }.coerceAtLeast(0)
    var currentIndex by remember(page.id) { mutableIntStateOf(initialIndex) }
    val currentPage = pageList.getOrElse(currentIndex) { page }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Pinch-to-zoom and Pan state
    var scale by remember(currentPage.filePath) { mutableFloatStateOf(1f) }
    var offset by remember(currentPage.filePath) { mutableStateOf(Offset.Zero) }

    // Hint banner state
    var showHint by remember(currentPage.filePath) { mutableStateOf(true) }
    LaunchedEffect(currentPage.filePath) {
        delay(3500)
        showHint = false
    }

    // Load preview bitmap safely without cache pollution or recycling bugs
    LaunchedEffect(currentPage.filePath) {
        isLoading = true
        errorMessage = null
        scale = 1f
        offset = Offset.Zero
        try {
            val decoded = withContext(Dispatchers.IO) {
                val file = File(currentPage.filePath)
                if (!file.exists()) {
                    null
                } else {
                    CrashLogger.decodeSampledBitmap(
                        filePath = file.absolutePath,
                        reqWidth = 1800,
                        reqHeight = 2400,
                        preferredConfig = Bitmap.Config.RGB_565,
                        useCache = false
                    )
                }
            }
            if (decoded != null) {
                bitmap = decoded
            } else {
                errorMessage = "Could not render image. The file may be missing or damaged."
            }
        } catch (t: Throwable) {
            CrashLogger.logNonFatal("PageViewerDialog", "Error decoding page ${currentPage.pageNumber}", t)
            errorMessage = "Memory error while rendering page preview."
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_viewer_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (pageList.size > 1) "Page ${currentIndex + 1} of ${pageList.size}" else "Page ${currentPage.pageNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (scale > 1.05f) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        "${(scale * 100).roundToInt()}%",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "${currentPage.widthPx} × ${currentPage.heightPx} px • ${currentPage.dpi} DPI • ${currentPage.colorMode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    // Reset zoom icon button when zoomed
                    if (scale > 1.05f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reset_zoom_button")
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_page_in_viewer")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color(0xFFFF6B6B))
                    }
                }

                // Main Interactive Preview Viewport with Pinch-to-Zoom & Pan
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()

                    when {
                        isLoading -> {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                        bitmap != null -> {
                            val bmp = bitmap!!
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Pointer input: Double-tap to toggle zoom
                                    .pointerInput(currentPage.filePath) {
                                        detectTapGestures(
                                            onDoubleTap = { tapCenter ->
                                                if (scale > 1.15f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 2.5f
                                                    // Center pan towards tap point
                                                    val focusX = (containerWidth / 2f - tapCenter.x) * 1.5f
                                                    val focusY = (containerHeight / 2f - tapCenter.y) * 1.5f
                                                    val maxPanX = (containerWidth * (2.5f - 1f) / 2f).coerceAtLeast(0f)
                                                    val maxPanY = (containerHeight * (2.5f - 1f) / 2f).coerceAtLeast(0f)
                                                    offset = Offset(
                                                        x = focusX.coerceIn(-maxPanX, maxPanX),
                                                        y = focusY.coerceIn(-maxPanY, maxPanY)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    // Pointer input: Multi-touch pinch-to-zoom & two-axis pan
                                    .pointerInput(currentPage.filePath) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                                            scale = newScale
                                            if (newScale > 1f) {
                                                val maxPanX = (containerWidth * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                                val maxPanY = (containerHeight * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                                offset = Offset(
                                                    x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                                    y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                                )
                                            } else {
                                                offset = Offset.Zero
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Scanned Page ${currentPage.pageNumber}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            translationX = offset.x
                                            translationY = offset.y
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    errorMessage ?: "Image preview unavailable",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Scanned image is safely saved on disk and can still be exported.",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Multi-page navigation arrows (floating left and right)
                    if (pageList.size > 1 && scale <= 1.05f) {
                        // Previous Page
                        if (currentIndex > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp)
                            ) {
                                IconButton(
                                    onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("preview_prev_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Page",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Next Page
                        if (currentIndex < pageList.size - 1) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 12.dp)
                            ) {
                                IconButton(
                                    onClick = { currentIndex = (currentIndex + 1).coerceAtMost(pageList.size - 1) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("preview_next_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Page",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Floating Zoom Controls Toolbar (Bottom Right)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E1E1E).copy(alpha = 0.9f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Zoom Out
                            IconButton(
                                onClick = {
                                    val newScale = (scale - 0.5f).coerceIn(1f, 6f)
                                    scale = newScale
                                    if (newScale <= 1f) offset = Offset.Zero
                                },
                                enabled = scale > 1f,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("zoom_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = if (scale > 1f) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Zoom Percentage badge (tap to reset zoom)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (scale > 1.05f) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .testTag("zoom_badge")
                            ) {
                                TextButton(
                                    onClick = {
                                        scale = if (scale > 1.05f) 1f else 2.5f
                                        offset = Offset.Zero
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        "${(scale * 100).roundToInt()}%",
                                        color = if (scale > 1.05f) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Zoom In
                            IconButton(
                                onClick = {
                                    val newScale = (scale + 0.5f).coerceIn(1f, 6f)
                                    scale = newScale
                                },
                                enabled = scale < 6f,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("zoom_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = if (scale < 6f) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Temporary Helpful Hint Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHint && scale <= 1.05f,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 70.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Pinch or double-tap to zoom into preview",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Footer with metadata
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "File Size: ${currentPage.fileSizeBytes / 1024} KB  |  Lines: ${currentPage.actualDeliveredLines}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (pageList.size > 1) {
                            Text(
                                "Page ${currentIndex + 1}/${pageList.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Page ${currentPage.pageNumber}?") },
            text = { Text("Are you sure you want to remove this page from the current scan session?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
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
