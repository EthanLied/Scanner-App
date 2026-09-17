package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScannedPage
import com.example.util.CrashLogger
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 2-Step Full-screen Save & Export flow:
 * - Screen 1: Select Format (PDF or Images) via 2 big buttons, plus select page numbers.
 * - Screen 2: Choose destination (Share or Save to Local Device) via 2 big buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveExportScreen(
    allPages: List<ScannedPage>,
    onNavigateBack: () -> Unit,
    onSaveToDevice: (format: ExportFormat, selectedPages: List<ScannedPage>) -> Unit,
    onShare: (format: ExportFormat, selectedPages: List<ScannedPage>) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.COMBINED_PDF) }
    val selectedPageIds = remember { mutableStateListOf<String>().apply { addAll(allPages.map { it.id }) } }

    // Intercept back navigation
    BackHandler {
        if (currentStep == 2) {
            currentStep = 1
        } else {
            onNavigateBack()
        }
    }

    val selectedPagesList = remember(selectedPageIds.toList(), allPages) {
        allPages.filter { it.id in selectedPageIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentStep == 1) "Save & Share" else "Choose Destination",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentStep == 1) "Step 1 of 2: Format & Pages" else "Step 2 of 2: Save or Share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) {
                                currentStep = 1
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("save_export_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (currentStep == 1) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("cancel_export_button")
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { currentStep = 2 },
                            enabled = selectedPageIds.isNotEmpty(),
                            modifier = Modifier.testTag("continue_to_step2_button")
                        ) {
                            Text("Next Step")
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (currentStep == 1) {
                    // ----------------------------------------------------
                    // STEP 1: Format & Pages Selection
                    // ----------------------------------------------------

                    // 1. Choose Format
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "1. Choose File Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // 2 Big Buttons for Format Selection (No descriptions)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BigFormatCard(
                                title = "PDF Document",
                                icon = Icons.Default.PictureAsPdf,
                                iconColor = Color(0xFFD32F2F),
                                isSelected = selectedFormat == ExportFormat.COMBINED_PDF,
                                onClick = { selectedFormat = ExportFormat.COMBINED_PDF },
                                modifier = Modifier.weight(1f),
                                testTag = "format_button_pdf"
                            )

                            BigFormatCard(
                                title = "Picture Files (PNG)",
                                icon = Icons.Default.Image,
                                iconColor = Color(0xFF1976D2),
                                isSelected = selectedFormat == ExportFormat.SEPARATE_PNG,
                                onClick = { selectedFormat = ExportFormat.SEPARATE_PNG },
                                modifier = Modifier.weight(1f),
                                testTag = "format_button_png"
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 2. Select Pages
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Select Pages (${selectedPageIds.size} of ${allPages.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            TextButton(
                                onClick = {
                                    if (selectedPageIds.size == allPages.size) {
                                        selectedPageIds.clear()
                                    } else {
                                        selectedPageIds.clear()
                                        selectedPageIds.addAll(allPages.map { it.id })
                                    }
                                },
                                modifier = Modifier.testTag("toggle_select_all_pages")
                            ) {
                                Text(
                                    if (selectedPageIds.size == allPages.size) "Deselect All" else "Select All",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Thumbnail Cards Grid / Flow for Page Selection
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allPages.forEach { page ->
                                val isChecked = page.id in selectedPageIds
                                PageSelectableThumbnailCard(
                                    page = page,
                                    isSelected = isChecked,
                                    onToggle = {
                                        if (isChecked) {
                                            selectedPageIds.remove(page.id)
                                        } else {
                                            selectedPageIds.add(page.id)
                                        }
                                    }
                                )
                            }
                        }

                        if (selectedPageIds.isEmpty()) {
                            Text(
                                text = "⚠️ Please select at least one page to proceed.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    // ----------------------------------------------------
                    // STEP 2: Destination Selection (Share or Save to Device)
                    // ----------------------------------------------------

                    // Summary Badge Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedFormat == ExportFormat.COMBINED_PDF) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (selectedFormat == ExportFormat.COMBINED_PDF) "Combined PDF" else "Separate PNG Pictures",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedPagesList.size} page(s) ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Where would you like to save or send?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 2 FULL-WIDTH BIG ACTION CARDS (No descriptions, using requested logos)
                    // Option 1: Save to Local Device with Folder icon
                    BigActionOptionCard(
                        title = "Save to Local Device",
                        mainIconContent = {
                            FolderBadge()
                        },
                        logosContent = {
                            FolderBadge()
                        },
                        onClick = { onSaveToDevice(selectedFormat, selectedPagesList) },
                        testTag = "save_to_local_device_button"
                    )

                    // Option 2: Share with Apps (WhatsApp, Google Drive, Facebook logos)
                    BigActionOptionCard(
                        title = "Share",
                        mainIconContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1565C0).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        logosContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WhatsAppBadge()
                                GoogleDriveBadge()
                                FacebookBadge()
                            }
                        },
                        onClick = { onShare(selectedFormat, selectedPagesList) },
                        testTag = "share_to_apps_button"
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("back_to_step1_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Back to Step 1 (Change Format or Pages)")
                    }
                }
            }
        }
    }
}

/**
 * Selectable Page Card with Thumbnail Image.
 */
@Composable
private fun PageSelectableThumbnailCard(
    page: ScannedPage,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    var thumbnailBitmap by remember(page.filePath) {
        mutableStateOf(CrashLogger.getCachedBitmap(page.filePath, 160, 220))
    }

    LaunchedEffect(page.filePath) {
        if (thumbnailBitmap == null) {
            thumbnailBitmap = withContext(Dispatchers.IO) {
                val file = File(page.filePath)
                if (file.exists()) {
                    CrashLogger.decodeSampledBitmap(file.absolutePath, 160, 220)
                } else null
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .width(135.dp)
            .clickable { onToggle() }
            .testTag("page_checkbox_${page.pageNumber}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbnailBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page ${page.pageNumber} Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Checkbox top right overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Page ${page.pageNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Format selection button (Clean, without verbose descriptions).
 */
@Composable
private fun BigFormatCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Big Action Option Card taking up the whole area without descriptions, with prominent logos.
 */
@Composable
private fun BigActionOptionCard(
    title: String,
    mainIconContent: @Composable () -> Unit,
    logosContent: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                mainIconContent()

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Logos Area
            logosContent()
        }
    }
}

@Composable
fun FolderBadge() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3E0)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(28.dp)) {
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.1f, h * 0.3f)
                lineTo(w * 0.42f, h * 0.3f)
                lineTo(w * 0.52f, h * 0.42f)
                lineTo(w * 0.9f, h * 0.42f)
                quadraticBezierTo(w * 0.95f, h * 0.42f, w * 0.95f, h * 0.5f)
                lineTo(w * 0.95f, h * 0.8f)
                quadraticBezierTo(w * 0.95f, h * 0.88f, w * 0.88f, h * 0.88f)
                lineTo(w * 0.12f, h * 0.88f)
                quadraticBezierTo(w * 0.05f, h * 0.88f, w * 0.05f, h * 0.8f)
                lineTo(w * 0.05f, h * 0.38f)
                quadraticBezierTo(w * 0.05f, h * 0.3f, w * 0.1f, h * 0.3f)
                close()
            }
            drawPath(path, color = Color(0xFFF57C00))
        }
    }
}

@Composable
fun WhatsAppBadge() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF25D366)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "WA",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun GoogleDriveBadge() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F3F4)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val pYellow = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.35f, h * 0.12f)
                lineTo(w * 0.65f, h * 0.12f)
                lineTo(w * 0.95f, h * 0.65f)
                lineTo(w * 0.65f, h * 0.65f)
                close()
            }
            drawPath(pYellow, color = Color(0xFFFFBA00))

            val pGreen = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.12f, h * 0.48f)
                lineTo(w * 0.35f, h * 0.12f)
                lineTo(w * 0.65f, h * 0.65f)
                lineTo(w * 0.42f, h * 0.88f)
                close()
            }
            drawPath(pGreen, color = Color(0xFF0F9D58))

            val pBlue = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.08f, h * 0.88f)
                lineTo(w * 0.72f, h * 0.88f)
                lineTo(w * 0.95f, h * 0.65f)
                lineTo(w * 0.25f, h * 0.65f)
                close()
            }
            drawPath(pBlue, color = Color(0xFF4285F4))
        }
    }
}

@Composable
fun FacebookBadge() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}
