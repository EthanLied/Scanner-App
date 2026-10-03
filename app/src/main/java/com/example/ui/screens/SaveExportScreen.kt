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
import androidx.compose.material.icons.filled.Visibility
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
    onShare: (format: ExportFormat, selectedPages: List<ScannedPage>) -> Unit,
    onPreviewPage: ((ScannedPage) -> Unit)? = null
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

    val currentTimestamp = remember {
        java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date())
    }
    val documentFilename = remember(selectedFormat, currentTimestamp) {
        if (selectedFormat == ExportFormat.COMBINED_PDF) "$currentTimestamp.pdf" else "Scan_$currentTimestamp"
    }

    val totalBytes = remember(selectedPagesList) {
        selectedPagesList.sumOf { it.fileSizeBytes }
    }
    val formattedSize = remember(totalBytes) {
        val mb = totalBytes.toDouble() / (1024 * 1024)
        if (mb >= 0.1) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = kotlin.math.max(1, (totalBytes / 1024).toInt())
            "$kb KB"
        }
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
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val hPadding = if (maxWidth < 360.dp) 12.dp else 20.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(hPadding),
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

                        // 2 Big Buttons for Format Selection (No descriptions, clean responsive cards)
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
                                title = "PNG Images",
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

                    // WhatsApp-styled Document Preview Card (as requested: shows document thumbnail & metadata banner before sending)
                    WhatsAppDocumentPreviewCard(
                        fileName = documentFilename,
                        pageCount = selectedPagesList.size,
                        fileSizeFormatted = formattedSize,
                        format = selectedFormat,
                        firstPage = selectedPagesList.firstOrNull(),
                        onClickPreview = {
                            selectedPagesList.firstOrNull()?.let { firstPage ->
                                onPreviewPage?.invoke(firstPage)
                            }
                        }
                    )

                    Text(
                        text = "Where would you like to save or send?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 2 FULL-WIDTH BIG ACTION CARDS (No descriptions, using requested logos)
                    // Option 1: Save to Local Device with Folder icon
                    BigActionOptionCard(
                        title = "Save to Local Device",
                        leadingIcon = Icons.Default.Save,
                        iconTint = Color(0xFFE65100),
                        logosContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FolderBadge()
                            }
                        },
                        onClick = { onSaveToDevice(selectedFormat, selectedPagesList) },
                        testTag = "save_to_local_device_button"
                    )

                    // Option 2: Share with Apps (WhatsApp, Google Drive, Facebook logos)
                    BigActionOptionCard(
                        title = "Share",
                        leadingIcon = Icons.Default.Share,
                        iconTint = Color(0xFF1565C0),
                        logosContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            .width(125.dp)
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
                .padding(vertical = 18.dp, horizontal = 8.dp),
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                softWrap = false,
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
 * Stacks title and logos vertically so long titles and badges never get squashed into 1 letter per line.
 */
@Composable
private fun BigActionOptionCard(
    title: String,
    leadingIcon: ImageVector,
    iconTint: Color,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Logos Area across the full width of the card
            logosContent()
        }
    }
}

/**
 * Document Preview Card matching WhatsApp's rich PDF preview banner:
 * Displays a visual top-crop preview of the first page, document filename,
 * page count, file size, and file type in a sleek dark green document bar.
 */
@Composable
fun WhatsAppDocumentPreviewCard(
    fileName: String,
    pageCount: Int,
    fileSizeFormatted: String,
    format: ExportFormat,
    firstPage: ScannedPage?,
    onClickPreview: () -> Unit
) {
    var thumbnailBitmap by remember(firstPage?.filePath) {
        mutableStateOf(firstPage?.let { CrashLogger.getCachedBitmap(it.filePath, 800, 1000) })
    }

    LaunchedEffect(firstPage?.filePath) {
        if (firstPage != null && thumbnailBitmap == null) {
            thumbnailBitmap = withContext(Dispatchers.IO) {
                val f = File(firstPage.filePath)
                if (f.exists()) {
                    CrashLogger.decodeSampledBitmap(f.absolutePath, 800, 1000)
                } else null
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3E34)),
        border = BorderStroke(1.5.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickPreview() }
            .testTag("pdf_preview_banner_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Preview Area: Document first page crop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFE8EDE9)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumbnailBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Document Preview",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top-right pill badge: "Preview"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "Preview",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Info Bar: Dark green WhatsApp document card style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F3E34))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$pageCount page${if (pageCount > 1) "s" else ""} • $fileSizeFormatted • ${if (format == ExportFormat.COMBINED_PDF) "PDF" else "PNG"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0D5CE),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (format == ExportFormat.COMBINED_PDF) Icons.Default.PictureAsPdf else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
