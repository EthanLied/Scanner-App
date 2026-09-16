package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PrinterDevice
import com.example.data.model.ScanColorMode
import com.example.data.model.ScanDpi
import com.example.data.model.ScanEnhancement
import com.example.data.model.ScanJpegQuality
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings
import com.example.data.model.ScanSource
import com.example.data.model.ScannerHardwareCapabilities
import com.example.data.model.ScannerModelRegistry

private enum class SettingsTab(val title: String, val icon: ImageVector) {
    CORE("Quality & Color", Icons.Default.Tune),
    AREA_CROP("Page Size", Icons.Default.Crop),
    QUALITY("Picture Fixes", Icons.Default.AutoAwesome)
}

/**
 * Full-screen simplified Hardware Scan Settings.
 * Completely removes technical metric summaries, protocol frames, and redundant source blocks
 * so that any 12-year-old can easily configure their scan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareSettingsScreen(
    currentSettings: ScanSettings,
    activePrinter: PrinterDevice? = null,
    onNavigateBack: () -> Unit,
    onSave: (ScanSettings) -> Unit
) {
    // Hardware back press handling
    BackHandler(onBack = onNavigateBack)

    val selectedProfile = remember(activePrinter) {
        ScannerModelRegistry.resolveCapabilities(activePrinter?.model)
    }

    // Default to Flatbed source or first supported source without cluttering the UI
    val selectedSource = remember(currentSettings.source, selectedProfile) {
        if (currentSettings.source in selectedProfile.supportedSources) {
            currentSettings.source
        } else {
            selectedProfile.supportedSources.firstOrNull() ?: ScanSource.FLATBED
        }
    }

    var selectedDpi by remember(currentSettings.dpi, selectedProfile) {
        val validDpi = if (currentSettings.dpi in selectedProfile.supportedDpis) {
            currentSettings.dpi
        } else {
            selectedProfile.supportedDpis.maxByOrNull { it.value } ?: ScanDpi.DPI_300
        }
        mutableStateOf(validDpi)
    }

    var selectedColor by remember { mutableStateOf(currentSettings.colorMode) }

    var selectedSize by remember(currentSettings.pageSize, selectedSource, selectedProfile) {
        val isLegalExceedingFlatbed = currentSettings.pageSize == ScanPageSize.US_LEGAL &&
                selectedSource == ScanSource.FLATBED &&
                ScanPageSize.US_LEGAL.heightMm > selectedProfile.maxPlatenHeightMm
        val validSize = if (isLegalExceedingFlatbed) {
            ScanPageSize.A4
        } else {
            currentSettings.pageSize
        }
        mutableStateOf(validSize)
    }

    var customWidthMm by remember { mutableStateOf(currentSettings.customWidthMm) }
    var customHeightMm by remember { mutableStateOf(currentSettings.customHeightMm) }
    var xOffsetMm by remember { mutableStateOf(currentSettings.xOffsetMm) }
    var yOffsetMm by remember { mutableStateOf(currentSettings.yOffsetMm) }

    var selectedQuality by remember { mutableStateOf(currentSettings.jpegQuality) }
    var brightness by remember { mutableStateOf(currentSettings.enhancement.brightness) }
    var contrast by remember { mutableStateOf(currentSettings.enhancement.contrast) }
    var autoDeskew by remember { mutableStateOf(currentSettings.enhancement.autoDeskew) }
    var invertColors by remember { mutableStateOf(currentSettings.enhancement.invertColors) }

    var activeTab by remember { mutableStateOf(SettingsTab.CORE) }

    // Assemble current working settings
    val workingSettings = remember(
        selectedSource, selectedDpi, selectedColor, selectedSize,
        customWidthMm, customHeightMm, xOffsetMm, yOffsetMm,
        selectedQuality, brightness, contrast, autoDeskew, invertColors
    ) {
        ScanSettings(
            source = selectedSource,
            dpi = selectedDpi,
            colorMode = selectedColor,
            pageSize = selectedSize,
            customWidthMm = customWidthMm,
            customHeightMm = customHeightMm,
            xOffsetMm = xOffsetMm,
            yOffsetMm = yOffsetMm,
            jpegQuality = selectedQuality,
            enhancement = ScanEnhancement(
                brightness = brightness,
                contrast = contrast,
                autoDeskew = autoDeskew,
                invertColors = invertColors
            )
        )
    }

    val validatedSettings = remember(workingSettings, selectedProfile) {
        val result = ScannerModelRegistry.validate(workingSettings, selectedProfile)
        result.adjustedSettings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Scan Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                selectedProfile.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onSave(validatedSettings) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
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
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(validatedSettings) },
                        modifier = Modifier.testTag("bottom_save_settings_button")
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Simplified Category Tabs
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsTab.values().forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Scrollable Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (activeTab) {
                        SettingsTab.CORE -> {
                            CoreSettingsContent(
                                capabilities = selectedProfile,
                                selectedDpi = selectedDpi,
                                onDpiSelect = { selectedDpi = it },
                                selectedColor = selectedColor,
                                onColorSelect = { selectedColor = it }
                            )
                        }

                        SettingsTab.AREA_CROP -> {
                            AreaCropContent(
                                capabilities = selectedProfile,
                                selectedSize = selectedSize,
                                onSizeSelect = { selectedSize = it },
                                customWidthMm = customWidthMm,
                                onWidthChange = { customWidthMm = it },
                                customHeightMm = customHeightMm,
                                onHeightChange = { customHeightMm = it },
                                xOffsetMm = xOffsetMm,
                                onXOffsetChange = { xOffsetMm = it },
                                yOffsetMm = yOffsetMm,
                                onYOffsetChange = { yOffsetMm = it }
                            )
                        }

                        SettingsTab.QUALITY -> {
                            EnhancementQualityContent(
                                selectedQuality = selectedQuality,
                                onQualitySelect = { selectedQuality = it },
                                brightness = brightness,
                                onBrightnessChange = { brightness = it },
                                contrast = contrast,
                                onContrastChange = { contrast = it },
                                autoDeskew = autoDeskew,
                                onAutoDeskewChange = { autoDeskew = it },
                                invertColors = invertColors,
                                onInvertChange = { invertColors = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Quality & Color options: Sharpness/DPI and Color mode.
 * Input source is hidden because flatbed scanners use their glass automatically.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreSettingsContent(
    capabilities: ScannerHardwareCapabilities,
    selectedDpi: ScanDpi,
    onDpiSelect: (ScanDpi) -> Unit,
    selectedColor: ScanColorMode,
    onColorSelect: (ScanColorMode) -> Unit
) {
    val supportedDpis = remember(capabilities) {
        ScanDpi.values().filter { it in capabilities.supportedDpis }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sharpness / Detail (DPI)
            Column {
                Text(
                    "Picture Sharpness",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Higher numbers make text and drawings look clearer and sharper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    supportedDpis.forEach { dpi ->
                        val isSelected = selectedDpi == dpi
                        val label = when (dpi) {
                            ScanDpi.DPI_75 -> "75 DPI (Draft)"
                            ScanDpi.DPI_150 -> "150 DPI (Quick)"
                            ScanDpi.DPI_300 -> "300 DPI (Best for Homework & Photos)"
                            ScanDpi.DPI_600 -> "600 DPI (Extra Sharp)"
                            ScanDpi.DPI_1200 -> "1200 DPI (Super High Detail)"
                            ScanDpi.DPI_2400 -> "2400 DPI (Ultra Detail)"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .clickable { onDpiSelect(dpi) }
                                .testTag("dpi_option_${dpi.value}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (dpi.value == 300) {
                                    Text(
                                        "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // 2. Color Mode
            Column {
                Text(
                    "Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose whether you want full color or black and white.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanColorMode.values().forEach { mode ->
                        val isSelected = selectedColor == mode
                        val (title, description) = when (mode) {
                            ScanColorMode.COLOR -> "Full Color" to "For photos, artwork, and colored documents"
                            ScanColorMode.GRAYSCALE -> "Grayscale" to "For black and white photos or notes with pencil shading"
                            ScanColorMode.LINE_ART -> "Black & White Only" to "For crisp typed text, forms, or ink drawings"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onColorSelect(mode) }
                                .testTag("color_option_${mode.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onColorSelect(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
 * Page Size & Crop Margins Content.
 * Simple presets and easy margin controls for scanning documents or small cards.
 */
@Composable
private fun AreaCropContent(
    capabilities: ScannerHardwareCapabilities,
    selectedSize: ScanPageSize,
    onSizeSelect: (ScanPageSize) -> Unit,
    customWidthMm: Double,
    onWidthChange: (Double) -> Unit,
    customHeightMm: Double,
    onHeightChange: (Double) -> Unit,
    xOffsetMm: Double,
    onXOffsetChange: (Double) -> Unit,
    yOffsetMm: Double,
    onYOffsetChange: (Double) -> Unit
) {
    val supportedSizes = remember(capabilities) {
        ScanPageSize.values().filter { size ->
            if (size == ScanPageSize.CUSTOM) {
                false
            } else {
                size.heightMm <= capabilities.maxPlatenHeightMm && size.widthMm <= capabilities.maxPlatenWidthMm
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paper Size Presets
            Column {
                Text(
                    "Paper Size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Pick the size of the paper on the scanner glass.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    supportedSizes.forEach { size ->
                        val isSelected = selectedSize == size
                        val friendlyLabel = when (size) {
                            ScanPageSize.A4 -> "A4 (Standard Paper)"
                            ScanPageSize.US_LETTER -> "Letter (Standard Paper)"
                            ScanPageSize.PHOTO_4X6 -> "4 × 6 Photo"
                            ScanPageSize.PHOTO_5X7 -> "5 × 7 Photo"
                            ScanPageSize.BUSINESS_CARD -> "Small Card / ID Card"
                            else -> size.label
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSizeSelect(size) }
                                .testTag("size_option_${size.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSizeSelect(size) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        friendlyLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        "${size.widthMm.toInt()} × ${size.heightMm.toInt()} mm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // Custom Size Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedSize == ScanPageSize.CUSTOM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSizeSelect(if (selectedSize == ScanPageSize.CUSTOM) ScanPageSize.A4 else ScanPageSize.CUSTOM)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Custom Page Size",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Set your own width and height",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = selectedSize == ScanPageSize.CUSTOM,
                        onCheckedChange = { checked ->
                            onSizeSelect(if (checked) ScanPageSize.CUSTOM else ScanPageSize.A4)
                        }
                    )
                }
            }

            if (selectedSize == ScanPageSize.CUSTOM) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Width", style = MaterialTheme.typography.bodySmall)
                        Text("${customWidthMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = customWidthMm.toFloat(),
                        onValueChange = { onWidthChange(it.toDouble()) },
                        valueRange = 20f..capabilities.maxPlatenWidthMm.toFloat()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Height", style = MaterialTheme.typography.bodySmall)
                        Text("${customHeightMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = customHeightMm.toFloat(),
                        onValueChange = { onHeightChange(it.toDouble()) },
                        valueRange = 20f..capabilities.maxPlatenHeightMm.toFloat()
                    )
                }
            }

            Divider()

            // Margins / Offsets
            Column {
                Text(
                    "Trim Margins",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Leave at 0 to scan from the corner of the glass, or slide to skip margins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Left margin", style = MaterialTheme.typography.bodySmall)
                    Text("${xOffsetMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = xOffsetMm.toFloat(),
                    onValueChange = { onXOffsetChange(it.toDouble()) },
                    valueRange = 0f..50f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top margin", style = MaterialTheme.typography.bodySmall)
                    Text("${yOffsetMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = yOffsetMm.toFloat(),
                    onValueChange = { onYOffsetChange(it.toDouble()) },
                    valueRange = 0f..50f
                )

                if (xOffsetMm > 0.0 || yOffsetMm > 0.0) {
                    OutlinedButton(
                        onClick = {
                            onXOffsetChange(0.0)
                            onYOffsetChange(0.0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Margins to 0")
                    }
                }
            }
        }
    }
}

/**
 * Picture Fixes & Save Quality Content.
 * Simple sliders and switches to adjust lighting, straightness, and file quality.
 */
@Composable
private fun EnhancementQualityContent(
    selectedQuality: ScanJpegQuality,
    onQualitySelect: (ScanJpegQuality) -> Unit,
    brightness: Int,
    onBrightnessChange: (Int) -> Unit,
    contrast: Int,
    onContrastChange: (Int) -> Unit,
    autoDeskew: Boolean,
    onAutoDeskewChange: (Boolean) -> Unit,
    invertColors: Boolean,
    onInvertChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Save Quality
            Column {
                Text(
                    "Save Quality",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose how high quality to save your scanned pictures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanJpegQuality.values().forEach { quality ->
                        val isSelected = selectedQuality == quality
                        val (title, description) = when (quality) {
                            ScanJpegQuality.COMPACT -> "Compact" to "Smaller file size, quicker to send or share"
                            ScanJpegQuality.BALANCED -> "Balanced (Recommended)" to "Great balance of quality and small file size"
                            ScanJpegQuality.HIGH -> "Best Quality" to "Clear photos and sharp text, slightly larger file size"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelect(quality) }
                                .testTag("quality_option_${quality.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onQualitySelect(quality) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // 2. Picture Adjustments
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Picture Adjustments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Brightness
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lighter or Darker", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when {
                                brightness > 0 -> "Brighter (+$brightness)"
                                brightness < 0 -> "Darker ($brightness)"
                                else -> "Normal (0)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { onBrightnessChange(it.toInt()) },
                        valueRange = -5f..5f,
                        steps = 9
                    )
                }

                // Contrast
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Text Darkness (Contrast)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when {
                                contrast > 0 -> "Darker (+$contrast)"
                                contrast < 0 -> "Softer ($contrast)"
                                else -> "Normal (0)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = contrast.toFloat(),
                        onValueChange = { onContrastChange(it.toInt()) },
                        valueRange = -5f..5f,
                        steps = 9
                    )
                }

                // Auto-straighten
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Straighten Page", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Automatically straightens pages if placed slightly crooked on the glass.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoDeskew,
                        onCheckedChange = onAutoDeskewChange,
                        modifier = Modifier.testTag("switch_auto_deskew")
                    )
                }

                // Invert colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invert Colors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Swap light and dark colors (like dark mode).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = invertColors,
                        onCheckedChange = onInvertChange,
                        modifier = Modifier.testTag("switch_invert_colors")
                    )
                }
            }
        }
    }
}
