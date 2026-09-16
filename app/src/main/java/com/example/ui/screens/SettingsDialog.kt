package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.protocol.chmp.ChmpConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: ScanSettings,
    activePrinter: PrinterDevice? = null,
    onDismiss: () -> Unit,
    onSave: (ScanSettings) -> Unit
) {
    // Determine machine capabilities based on active connected printer
    val defaultCapabilities = remember(activePrinter) {
        ScannerModelRegistry.resolveCapabilities(activePrinter?.model)
    }

    // Allow user to toggle between hardware model profiles if desired for testing/validation
    var selectedProfile by remember { mutableStateOf(defaultCapabilities) }

    // Dialog state
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSource by remember { mutableStateOf(currentSettings.source) }
    var selectedDpi by remember { mutableStateOf(currentSettings.dpi) }
    var selectedColor by remember { mutableStateOf(currentSettings.colorMode) }
    var selectedSize by remember { mutableStateOf(currentSettings.pageSize) }
    var customWidthMm by remember { mutableStateOf(currentSettings.customWidthMm) }
    var customHeightMm by remember { mutableStateOf(currentSettings.customHeightMm) }
    var xOffsetMm by remember { mutableStateOf(currentSettings.xOffsetMm) }
    var yOffsetMm by remember { mutableStateOf(currentSettings.yOffsetMm) }
    var selectedQuality by remember { mutableStateOf(currentSettings.jpegQuality) }

    // Enhancement state
    var brightness by remember { mutableIntStateOf(currentSettings.enhancement.brightness) }
    var contrast by remember { mutableIntStateOf(currentSettings.enhancement.contrast) }
    var autoDeskew by remember { mutableStateOf(currentSettings.enhancement.autoDeskew) }
    var invertColors by remember { mutableStateOf(currentSettings.enhancement.invertColors) }

    // Current working settings
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

    // Real-time hardware validation
    val validationResult = remember(workingSettings, selectedProfile) {
        ScannerModelRegistry.validate(workingSettings, selectedProfile)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Hardware Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Hardware Scan Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Active Machine model badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Model",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                selectedProfile.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            selectedProfile.hardwareType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        text = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Real-time Model Validation Alert Banner
                    if (validationResult.warnings.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("model_validation_warning")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Validation Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Hardware Constraint Alert",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                validationResult.warnings.forEach { warning ->
                                    Text(
                                        "• $warning",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("model_validation_success")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Validated",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "All options validated for ${selectedProfile.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Navigation Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Core") },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_core")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Area & Crop") },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_area")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Enhance & Quality") },
                            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_enhancement")
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Protocol & Specs") },
                            icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_specs")
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            // Core Tab: Resolution, Color Mode, Scan Source, Page Size
                            CoreSettingsTab(
                                capabilities = selectedProfile,
                                selectedSource = selectedSource,
                                onSourceSelect = { selectedSource = it },
                                selectedDpi = selectedDpi,
                                onDpiSelect = { selectedDpi = it },
                                selectedColor = selectedColor,
                                onColorSelect = { selectedColor = it },
                                selectedSize = selectedSize,
                                onSizeSelect = { selectedSize = it }
                            )
                        }

                        1 -> {
                            // Area & Crop Tab: Custom geometry and X/Y offset
                            AreaCropTab(
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
                                onYOffsetChange = { yOffsetMm = it },
                                effectiveWidth = workingSettings.effectiveWidthMm,
                                effectiveHeight = workingSettings.effectiveHeightMm
                            )
                        }

                        2 -> {
                            // Enhance & Quality Tab: JPEG Quality, Brightness, Contrast, Invert, Deskew
                            EnhancementQualityTab(
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

                        3 -> {
                            // Protocol & Specs Tab: Hardware profile inspector & raw CHMP registers
                            ProtocolSpecsTab(
                                capabilities = selectedProfile,
                                onSelectProfile = { selectedProfile = it },
                                settings = workingSettings
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Hardware Protocol Summary Box
                    HardwareSummaryCard(
                        settings = workingSettings,
                        capabilities = selectedProfile
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Save validated / auto-adjusted settings
                    onSave(validationResult.adjustedSettings)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text(
                    if (validationResult.isValid) "Save Settings" else "Apply Auto-Adjusted",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreSettingsTab(
    capabilities: ScannerHardwareCapabilities,
    selectedSource: ScanSource,
    onSourceSelect: (ScanSource) -> Unit,
    selectedDpi: ScanDpi,
    onDpiSelect: (ScanDpi) -> Unit,
    selectedColor: ScanColorMode,
    onColorSelect: (ScanColorMode) -> Unit,
    selectedSize: ScanPageSize,
    onSizeSelect: (ScanPageSize) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Scan Source (Validated by Model Hardware)
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Scan Input Source",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (capabilities.hasAdf) "ADF Feeder Available" else "Flatbed Only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanSource.values().forEach { source ->
                    val isSupported = source in capabilities.supportedSources
                    val isSelected = selectedSource == source
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            !isSupported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isSupported) { onSourceSelect(source) }
                            .testTag("source_option_${source.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                source.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            if (!isSupported) {
                                Text(
                                    "No hardware",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider()

        // 2. Resolution (DPI) (Validated against optical sensor limit)
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Resolution (DPI)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Max Optical: ${capabilities.maxOpticalDpi} DPI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScanDpi.values().forEach { dpi ->
                    val isSupported = dpi in capabilities.supportedDpis
                    val isSelected = selectedDpi == dpi
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            !isSupported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clickable(enabled = isSupported) { onDpiSelect(dpi) }
                            .testTag("dpi_option_${dpi.value}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${dpi.value} DPI",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                            if (dpi.value == 300) {
                                Text("Best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            } else if (!isSupported) {
                                Text("Exceeds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Divider()

        // 3. Color Mode
        Column {
            Text(
                "Color Mode & Bit Depth",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScanColorMode.values().forEach { mode ->
                    val isSelected = selectedColor == mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onColorSelect(mode) }
                            .padding(vertical = 4.dp)
                            .testTag("color_option_${mode.name}")
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onColorSelect(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(mode.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            val bppText = when (mode) {
                                ScanColorMode.COLOR -> "24-bit sRGB (Code 0x08, bpp 0x18)"
                                ScanColorMode.GRAYSCALE -> "8-bit Monochrome grayscale (Code 0x04, bpp 0x08)"
                                ScanColorMode.LINE_ART -> "1-bit Binary line art (Code 0x02, bpp 0x01)"
                            }
                            Text(bppText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        Divider()

        // 4. Page Size
        Column {
            Text(
                "Standard Page Size",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScanPageSize.values().filter { it != ScanPageSize.CUSTOM }.forEach { size ->
                    val isLegal = size == ScanPageSize.US_LEGAL
                    val isFlatbed = selectedSource == ScanSource.FLATBED
                    val hasWarning = isLegal && isFlatbed
                    val isSelected = selectedSize == size

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelect(size) }
                            .padding(vertical = 4.dp)
                            .testTag("size_option_${size.name}")
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSizeSelect(size) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                size.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (hasWarning) {
                                Text(
                                    "Note: 355.6 mm exceeds flatbed glass length. Requires ADF or folding.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaCropTab(
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
    onYOffsetChange: (Double) -> Unit,
    effectiveWidth: Double,
    effectiveHeight: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Scan Geometry & Hardware Crop Margins",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "Configure exact hardware crop coordinates on the platen glass to scan receipts, ID cards, photos, or documents without wasting bandwidth scanning empty glass.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Custom Area toggle
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (selectedSize == ScanPageSize.CUSTOM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (selectedSize == ScanPageSize.CUSTOM) {
                        onSizeSelect(ScanPageSize.A4)
                    } else {
                        onSizeSelect(ScanPageSize.CUSTOM)
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Enable Custom Scan Area Dimensions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Manually define width & height instead of standard paper sizes",
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
            // Width Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scan Width (mm)", style = MaterialTheme.typography.bodySmall)
                    Text("${customWidthMm.toInt()} mm (Max ${capabilities.maxPlatenWidthMm.toInt()} mm)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = customWidthMm.toFloat(),
                    onValueChange = { onWidthChange(it.toDouble()) },
                    valueRange = 20f..capabilities.maxPlatenWidthMm.toFloat(),
                    steps = 19
                )
            }

            // Height Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scan Height (mm)", style = MaterialTheme.typography.bodySmall)
                    Text("${customHeightMm.toInt()} mm (Max ${capabilities.maxPlatenHeightMm.toInt()} mm)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = customHeightMm.toFloat(),
                    onValueChange = { onHeightChange(it.toDouble()) },
                    valueRange = 20f..capabilities.maxPlatenHeightMm.toFloat(),
                    steps = 27
                )
            }
        }

        Divider()

        // Hardware X / Y Offsets (Byte 0x0C and 0x10)
        Column {
            Text(
                "Hardware Origin Offsets (Platen Alignment)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Shifts scan sensor starting origin on the scanner rails (bytes 0x0C and 0x10). Useful for margin trimming.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            // X Offset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Left Offset (X)", style = MaterialTheme.typography.bodySmall)
                Text("${xOffsetMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = xOffsetMm.toFloat(),
                onValueChange = { onXOffsetChange(it.toDouble()) },
                valueRange = 0f..50f,
                steps = 10
            )

            // Y Offset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Top Offset (Y)", style = MaterialTheme.typography.bodySmall)
                Text("${yOffsetMm.toInt()} mm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = yOffsetMm.toFloat(),
                onValueChange = { onYOffsetChange(it.toDouble()) },
                valueRange = 0f..50f,
                steps = 10
            )

            // Preset Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onXOffsetChange(0.0)
                        onYOffsetChange(0.0)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset (0,0)")
                }
                TextButton(
                    onClick = {
                        onXOffsetChange(10.0)
                        onYOffsetChange(10.0)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("10 mm Margin")
                }
            }
        }
    }
}

@Composable
private fun EnhancementQualityTab(
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. JPEG Compression Quality (Byte 0x20)
        Column {
            Text(
                "Hardware JPEG Quality (CHMP Byte 0x20)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Controls internal JPEG quantizer tables programmed into the scanner DSP. Affects image quality and transfer speed over Wi-Fi.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScanJpegQuality.values().forEach { quality ->
                    val isSelected = selectedQuality == quality
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Text(quality.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text("Byte 0x20: 0x%02X | %d%% quality".format(quality.code, quality.qualityPercent), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        Divider()

        // 2. Hardware Brightness Adjustment
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Image Brightness", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(if (brightness > 0) "+$brightness" else "$brightness", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brightness.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = -5f..5f,
                steps = 9
            )
        }

        // 3. Image Contrast Adjustment
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Image Contrast", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(if (contrast > 0) "+$contrast" else "$contrast", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = contrast.toFloat(),
                onValueChange = { onContrastChange(it.toInt()) },
                valueRange = -5f..5f,
                steps = 9
            )
        }

        Divider()

        // 4. Invert Colors (Film negative / dark documents)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Invert Colors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Inverts RGB channels. Useful for film negatives and dark inverted paper.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Switch(
                checked = invertColors,
                onCheckedChange = onInvertChange,
                modifier = Modifier.testTag("switch_invert_colors")
            )
        }

        // 5. Auto-Deskew & Edge Cleanup
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-Deskew & Clean Border", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Cleans black scanner glass shadow lines and straightens skewed pages.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Switch(
                checked = autoDeskew,
                onCheckedChange = onAutoDeskewChange,
                modifier = Modifier.testTag("switch_auto_deskew")
            )
        }
    }
}

@Composable
private fun ProtocolSpecsTab(
    capabilities: ScannerHardwareCapabilities,
    onSelectProfile: (ScannerHardwareCapabilities) -> Unit,
    settings: ScanSettings
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Hardware Model Profile Registry",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "Select a target machine profile to test and simulate validation constraints across different Canon PIXMA and MAXIFY scanner architectures:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ScannerModelRegistry.ALL_PROFILES.forEach { profile ->
                val isSelected = capabilities.modelSeries == profile.modelSeries
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfile(profile) }
                        .testTag("profile_option_${profile.displayName.replace(" ", "_")}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                profile.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Max ${profile.maxOpticalDpi} DPI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            profile.hardwareType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            profile.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Divider()

        // Protocol Frame Inspection
        Text(
            "CHMP ScanParam3 Binary Frame (72 bytes)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        val payload = remember(settings) {
            ChmpConstants.buildScanParam3Payload(settings)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Byte 0x00 (Source): 0x%02X (%s)".format(settings.source.code, settings.source.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x08-0x0B (DPI): %d (Encoded: 0x%04X)".format(settings.dpi.value, settings.dpi.value or 0x8000),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x0C-0x13 (Offsets): X=%d px, Y=%d px".format(settings.xOffsetPx, settings.yOffsetPx),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x14-0x1B (Dimensions): %d × %d px".format(settings.widthPx, settings.heightPx),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x1C-0x1D (Color): Mode 0x%02X, bpp 0x%02X".format(settings.colorMode.code, settings.colorMode.bpp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x20 (JPEG Quality): 0x%02X (%d%%)".format(settings.jpegQuality.code, settings.jpegQuality.qualityPercent),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Byte 0x37 (Checksum Mod 256): 0x%02X".format(payload[0x37]),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HardwareSummaryCard(
    settings: ScanSettings,
    capabilities: ScannerHardwareCapabilities
) {
    val estimatedMb = (settings.widthPx.toLong() * settings.heightPx.toLong() * (settings.colorMode.bpp.toInt()) / 8) / (1024 * 1024 * 6.0) // approx JPEG 1:6 ratio
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Delivered Resolution:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${settings.widthPx} × ${settings.heightPx} px",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Estimated Transfer Size:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "~%.1f MB (JPEG %s)".format(Math.max(0.1, estimatedMb), settings.jpegQuality.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Physical Platen Coverage:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "%.1f × %.1f mm (%s)".format(settings.effectiveWidthMm, settings.effectiveHeightMm, settings.source.label),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
