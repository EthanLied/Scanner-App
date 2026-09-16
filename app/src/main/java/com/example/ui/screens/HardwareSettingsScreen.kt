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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.example.protocol.chmp.ChmpConstants

private enum class SettingsTab(val title: String, val icon: ImageVector) {
    CORE("Core Scan", Icons.Default.Tune),
    AREA_CROP("Area & Crop", Icons.Default.Crop),
    QUALITY("Enhance & Quality", Icons.Default.AutoAwesome),
    SPECS("Protocol & Specs", Icons.Default.Memory)
}

/**
 * Full-screen dedicated Hardware Scan Settings destination.
 * Replaces the cramped modal pop-up with a spacious, dedicated screen.
 * All disabled and unsupported options with red warnings are hidden to save space.
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

    var selectedProfile by remember(activePrinter) {
        val detected = ScannerModelRegistry.resolveCapabilities(activePrinter?.model)
        mutableStateOf(detected)
    }

    var selectedSource by remember(currentSettings.source, selectedProfile) {
        val validSource = if (currentSettings.source in selectedProfile.supportedSources) {
            currentSettings.source
        } else {
            selectedProfile.supportedSources.firstOrNull() ?: ScanSource.FLATBED
        }
        mutableStateOf(validSource)
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
                            "Hardware Scan Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${selectedProfile.displayName} • ${selectedProfile.hardwareType}",
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
                            contentDescription = "Back to Scan Screen"
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${validatedSettings.widthPx} × ${validatedSettings.heightPx} px (${validatedSettings.dpi.value} DPI)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${validatedSettings.source.label} • ${validatedSettings.colorMode.label} • JPEG ${validatedSettings.jpegQuality.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onSave(validatedSettings) },
                            modifier = Modifier.testTag("bottom_save_settings_button")
                        ) {
                            Text("Apply & Save")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Navigation Row across the full width
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
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

            // Scrollable Content Area with max-width limit for tablets/desktop responsiveness
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
                    // Summary metrics card at the top of every tab
                    HardwareSummaryCard(
                        settings = validatedSettings,
                        capabilities = selectedProfile
                    )

                    when (activeTab) {
                        SettingsTab.CORE -> {
                            CoreSettingsContent(
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
                                onYOffsetChange = { yOffsetMm = it },
                                effectiveWidth = validatedSettings.effectiveWidthMm,
                                effectiveHeight = validatedSettings.effectiveHeightMm
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

                        SettingsTab.SPECS -> {
                            ProtocolSpecsContent(
                                capabilities = selectedProfile,
                                onSelectProfile = { newProfile ->
                                    selectedProfile = newProfile
                                    // Auto-adjust selections if unsupported in the newly selected profile
                                    if (selectedSource !in newProfile.supportedSources) {
                                        selectedSource = newProfile.supportedSources.firstOrNull() ?: ScanSource.FLATBED
                                    }
                                    if (selectedDpi !in newProfile.supportedDpis) {
                                        selectedDpi = newProfile.supportedDpis.maxByOrNull { it.value } ?: ScanDpi.DPI_300
                                    }
                                    if (selectedSize == ScanPageSize.US_LEGAL &&
                                        selectedSource == ScanSource.FLATBED &&
                                        ScanPageSize.US_LEGAL.heightMm > newProfile.maxPlatenHeightMm
                                    ) {
                                        selectedSize = ScanPageSize.A4
                                    }
                                },
                                settings = validatedSettings
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
 * Core scan options: source, DPI, color mode, page size.
 * Hides all unsupported / disabled options to maximize usable space and remove red warnings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreSettingsContent(
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
    // Only supported sources are displayed. Unsupported sources (e.g. ADF on flatbed) are hidden.
    val supportedSources = remember(capabilities) {
        ScanSource.values().filter { it in capabilities.supportedSources }
    }

    // Only supported DPIs are displayed. DPIs exceeding hardware capability are hidden.
    val supportedDpis = remember(capabilities) {
        ScanDpi.values().filter { it in capabilities.supportedDpis }
    }

    // Only page sizes that physically fit the hardware platen glass are displayed.
    // US Legal (355.6 mm) is hidden when using Flatbed (297 mm glass limit).
    val supportedSizes = remember(capabilities, selectedSource) {
        ScanPageSize.values().filter { size ->
            if (size == ScanPageSize.CUSTOM) {
                false
            } else if (selectedSource == ScanSource.FLATBED) {
                size.heightMm <= capabilities.maxPlatenHeightMm && size.widthMm <= capabilities.maxPlatenWidthMm
            } else {
                true
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
            // 1. Scan Source (Only display when device has multiple sources, or show the single supported one cleanly)
            Column {
                Text(
                    "Scan Input Source",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    supportedSources.forEach { source ->
                        val isSelected = selectedSource == source
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSourceSelect(source) }
                                .testTag("source_option_${source.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val icon = when (source) {
                                    ScanSource.FLATBED -> Icons.Default.Layers
                                    ScanSource.ADF_SIMPLEX -> Icons.Default.Description
                                    ScanSource.ADF_DUPLEX -> Icons.Default.VerticalSplit
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = source.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    source.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // 2. Resolution (DPI)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Optical Resolution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Max Optical: ${capabilities.maxOpticalDpi} DPI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    supportedDpis.forEach { dpi ->
                        val isSelected = selectedDpi == dpi
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
                                    "${dpi.value} DPI",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (dpi.value == 300) {
                                    Text(
                                        "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // 3. Color Mode & Bit Depth
            Column {
                Text(
                    "Color Mode & Bit Depth",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScanColorMode.values().forEach { mode ->
                        val isSelected = selectedColor == mode
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
                                        mode.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    val bppText = when (mode) {
                                        ScanColorMode.COLOR -> "24-bit sRGB (Code 0x08, bpp 0x18)"
                                        ScanColorMode.GRAYSCALE -> "8-bit Monochrome grayscale (Code 0x04, bpp 0x08)"
                                        ScanColorMode.LINE_ART -> "1-bit Binary line art (Code 0x02, bpp 0x01)"
                                    }
                                    Text(
                                        bppText,
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

            // 4. Standard Page Size (All oversized or invalid options are hidden)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Standard Page Size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Platen: ${capabilities.maxPlatenWidthMm.toInt()} × ${capabilities.maxPlatenHeightMm.toInt()} mm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    supportedSizes.forEach { size ->
                        val isSelected = selectedSize == size
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
                                        size.label,
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
        }
    }
}

/**
 * Scan Geometry & Hardware Crop Margins Content.
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
    onYOffsetChange: (Double) -> Unit,
    effectiveWidth: Double,
    effectiveHeight: Double
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
                color = if (selectedSize == ScanPageSize.CUSTOM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
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
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                        Text(
                            "${customWidthMm.toInt()} mm (Max ${capabilities.maxPlatenWidthMm.toInt()} mm)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                        Text(
                            "${customHeightMm.toInt()} mm (Max ${capabilities.maxPlatenHeightMm.toInt()} mm)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
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

                Spacer(modifier = Modifier.height(10.dp))

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
                    OutlinedButton(
                        onClick = {
                            onXOffsetChange(0.0)
                            onYOffsetChange(0.0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset (0,0)")
                    }
                    OutlinedButton(
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
}

/**
 * Image Enhancement & Compression Quality Content.
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
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelect(quality) }
                                .testTag("quality_option_${quality.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onQualitySelect(quality) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        quality.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        "Byte 0x20: 0x%02X | %d%% quality".format(quality.code, quality.qualityPercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
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
                    Text(
                        if (brightness > 0) "+$brightness" else "$brightness",
                        style = MaterialTheme.typography.labelMedium,
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

            // 3. Image Contrast Adjustment
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Image Contrast", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (contrast > 0) "+$contrast" else "$contrast",
                        style = MaterialTheme.typography.labelMedium,
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

            Divider()

            // 4. Invert Colors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Invert Colors", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Inverts RGB channels. Useful for film negatives and dark inverted paper.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
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
                    Text(
                        "Cleans black scanner glass shadow lines and straightens skewed pages.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = autoDeskew,
                    onCheckedChange = onAutoDeskewChange,
                    modifier = Modifier.testTag("switch_auto_deskew")
                )
            }
        }
    }
}

/**
 * Protocol frames, binary registers, and model capability switcher.
 */
@Composable
private fun ProtocolSpecsContent(
    capabilities: ScannerHardwareCapabilities,
    onSelectProfile: (ScannerHardwareCapabilities) -> Unit,
    settings: ScanSettings
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
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProfile(profile) }
                            .testTag("profile_option_${profile.displayName.replace(" ", "_")}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
}

/**
 * Metric summary card displayed at the top of settings.
 */
@Composable
private fun HardwareSummaryCard(
    settings: ScanSettings,
    capabilities: ScannerHardwareCapabilities
) {
    val estimatedMb = (settings.widthPx.toLong() * settings.heightPx.toLong() * (settings.colorMode.bpp.toInt()) / 8) / (1024 * 1024 * 6.0)
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
            Spacer(modifier = Modifier.height(3.dp))
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
            Spacer(modifier = Modifier.height(3.dp))
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
