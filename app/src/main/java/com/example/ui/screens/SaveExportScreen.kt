package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScannedPage

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
                        Text(
                            text = "Select how you would like to save your scanned pages:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 2 Big Buttons for Format Selection
                        BigFormatCard(
                            title = "PDF Document",
                            subtitle = "Combines all selected pages into a single neat PDF file. Recommended for homework, bills, and multi-page documents.",
                            icon = Icons.Default.PictureAsPdf,
                            iconColor = Color(0xFFD32F2F),
                            isSelected = selectedFormat == ExportFormat.COMBINED_PDF,
                            onClick = { selectedFormat = ExportFormat.COMBINED_PDF },
                            testTag = "format_button_pdf"
                        )

                        BigFormatCard(
                            title = "Picture Files (PNG)",
                            subtitle = "Saves each page as a separate high-resolution picture file on your device. Great for photos and artwork.",
                            icon = Icons.Default.Image,
                            iconColor = Color(0xFF1976D2),
                            isSelected = selectedFormat == ExportFormat.SEPARATE_PNG,
                            onClick = { selectedFormat = ExportFormat.SEPARATE_PNG },
                            testTag = "format_button_png"
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // 2. Select Pages
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "2. Select Pages (${selectedPageIds.size} of ${allPages.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Check the pages you want to include:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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

                        // Flow Row of Page Cards/Chips
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allPages.forEach { page ->
                                val isChecked = page.id in selectedPageIds
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        width = if (isChecked) 2.dp else 1.dp,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            if (isChecked) {
                                                selectedPageIds.remove(page.id)
                                            } else {
                                                selectedPageIds.add(page.id)
                                            }
                                        }
                                        .testTag("page_checkbox_${page.pageNumber}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    selectedPageIds.add(page.id)
                                                } else {
                                                    selectedPageIds.remove(page.id)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Page ${page.pageNumber}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${page.dpi} DPI",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
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
                        text = "How would you like to save or send your scan?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 2 BIG ACTION BUTTONS / CARDS
                    BigActionCard(
                        title = "Save to Local Device",
                        description = "Choose a folder on your phone or tablet (Downloads, Documents, etc.) to save your files safely.",
                        buttonText = "Save to Device Storage",
                        icon = Icons.Default.Download,
                        iconBgColor = Color(0xFF2E7D32),
                        buttonColor = Color(0xFF2E7D32),
                        onClick = {
                            onSaveToDevice(selectedFormat, selectedPagesList)
                        },
                        testTag = "save_to_local_device_button"
                    )

                    BigActionCard(
                        title = "Share with Other Apps",
                        description = "Quickly send your scan to Email, WhatsApp, Google Drive, Messages, or Nearby Share without saving first.",
                        buttonText = "Share with Apps",
                        icon = Icons.Default.Share,
                        iconBgColor = Color(0xFF1565C0),
                        buttonColor = Color(0xFF1565C0),
                        onClick = {
                            onShare(selectedFormat, selectedPagesList)
                        },
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
 * Large format selection button / card.
 */
@Composable
private fun BigFormatCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
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

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(10.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Large action destination card (Save to Device or Share with Apps).
 */
@Composable
private fun BigActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: ImageVector,
    iconBgColor: Color,
    buttonColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, buttonColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBgColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
