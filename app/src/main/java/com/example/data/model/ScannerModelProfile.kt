package com.example.data.model

/**
 * Defines hardware scanning capabilities and limitations for specific scanner models.
 */
data class ScannerHardwareCapabilities(
    val modelSeries: String,
    val displayName: String,
    val hardwareType: String,
    val hasAdf: Boolean,
    val hasAdfDuplex: Boolean,
    val maxOpticalDpi: Int,
    val supportedDpis: List<ScanDpi>,
    val supportedSources: List<ScanSource>,
    val supportedPageSizes: List<ScanPageSize>,
    val supportedColorModes: List<ScanColorMode>,
    val supportedQualities: List<ScanJpegQuality> = ScanJpegQuality.values().toList(),
    val maxPlatenWidthMm: Double = 216.0,
    val maxPlatenHeightMm: Double = 297.0,
    val notes: String
)

data class ValidationResult(
    val isValid: Boolean,
    val warnings: List<String>,
    val adjustedSettings: ScanSettings
)

object ScannerModelRegistry {

    val PIXMA_G3010_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "Canon PIXMA G3000 / G3010 Series",
        displayName = "Canon PIXMA G3010",
        hardwareType = "Flatbed CIS (Contact Image Sensor)",
        hasAdf = false,
        hasAdfDuplex = false,
        maxOpticalDpi = 600,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600
        ),
        supportedSources = listOf(ScanSource.FLATBED),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "Contact Image Sensor (CIS) optical sampling 600×1200 dpi. Platen glass supports up to A4/Letter size. No ADF feeder hardware."
    )

    val PIXMA_G4010_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "Canon PIXMA G4000 / G4010 / G7000 Series",
        displayName = "Canon PIXMA G4010 (with ADF)",
        hardwareType = "Flatbed + ADF Simplex Feeder",
        hasAdf = true,
        hasAdfDuplex = false,
        maxOpticalDpi = 600,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600
        ),
        supportedSources = listOf(ScanSource.FLATBED, ScanSource.ADF_SIMPLEX),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.US_LEGAL,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "Includes 20-sheet ADF supporting US Legal (355.6 mm). Flatbed platen supports up to A4/Letter."
    )

    val MAXIFY_MB_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "Canon MAXIFY MB2100 / MB5100 / MB5400 Series",
        displayName = "Canon MAXIFY MB Series",
        hardwareType = "Flatbed + ADF Duplex (Two-Sided)",
        hasAdf = true,
        hasAdfDuplex = true,
        maxOpticalDpi = 1200,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600,
            ScanDpi.DPI_1200
        ),
        supportedSources = listOf(ScanSource.FLATBED, ScanSource.ADF_SIMPLEX, ScanSource.ADF_DUPLEX),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.US_LEGAL,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "Heavy duty office scanner with single-pass duplex ADF. Up to 1200 DPI optical scanning."
    )

    val PIXMA_TS_PHOTO_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "Canon PIXMA TS Photo Series (TS8000 / TS9000 / TS8300)",
        displayName = "Canon PIXMA TS Photo Series",
        hardwareType = "High-Resolution Photo CIS Flatbed",
        hasAdf = false,
        hasAdfDuplex = false,
        maxOpticalDpi = 2400,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600,
            ScanDpi.DPI_1200,
            ScanDpi.DPI_2400
        ),
        supportedSources = listOf(ScanSource.FLATBED),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "High-resolution photo scanning sensor capable of up to 2400 DPI optical resolution."
    )

    val CANOSCAN_LIDE_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "CanoScan LiDE Series (LiDE 300 / 400)",
        displayName = "CanoScan LiDE Scanner",
        hardwareType = "Ultra-Compact Flatbed Scanner",
        hasAdf = false,
        hasAdfDuplex = false,
        maxOpticalDpi = 2400,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600,
            ScanDpi.DPI_1200,
            ScanDpi.DPI_2400
        ),
        supportedSources = listOf(ScanSource.FLATBED),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "Dedicated flatbed scanner with 3-color (RGB) LED lamp and high-resolution CIS."
    )

    val GENERIC_PROFILE = ScannerHardwareCapabilities(
        modelSeries = "Canon Multi-Function Scanner (Standard CHMP)",
        displayName = "Canon CHMP Scanner",
        hardwareType = "Flatbed All-in-One",
        hasAdf = false,
        hasAdfDuplex = false,
        maxOpticalDpi = 600,
        supportedDpis = listOf(
            ScanDpi.DPI_75,
            ScanDpi.DPI_150,
            ScanDpi.DPI_300,
            ScanDpi.DPI_600
        ),
        supportedSources = listOf(ScanSource.FLATBED),
        supportedPageSizes = listOf(
            ScanPageSize.A4,
            ScanPageSize.US_LETTER,
            ScanPageSize.PHOTO_4X6,
            ScanPageSize.PHOTO_5X7,
            ScanPageSize.BUSINESS_CARD,
            ScanPageSize.CUSTOM
        ),
        supportedColorModes = listOf(
            ScanColorMode.COLOR,
            ScanColorMode.GRAYSCALE,
            ScanColorMode.LINE_ART
        ),
        maxPlatenWidthMm = 216.0,
        maxPlatenHeightMm = 297.0,
        notes = "Standard Canon CHMP binary protocol profile with safe 600 DPI platen limits."
    )

    val ALL_PROFILES = listOf(
        PIXMA_G3010_PROFILE,
        PIXMA_G4010_PROFILE,
        MAXIFY_MB_PROFILE,
        PIXMA_TS_PHOTO_PROFILE,
        CANOSCAN_LIDE_PROFILE,
        GENERIC_PROFILE
    )

    /**
     * Resolves the hardware capabilities profile based on printer model name or identification strings.
     */
    fun resolveCapabilities(modelName: String?): ScannerHardwareCapabilities {
        if (modelName.isNullOrBlank()) return PIXMA_G3010_PROFILE

        val name = modelName.uppercase()
        return when {
            // G3010 / G3000 / G2010 / G3020 / G3060 / G1010
            name.contains("G3010") || name.contains("G3000") || name.contains("G3100") ||
            name.contains("G3200") || name.contains("G3400") || name.contains("G3500") ||
            name.contains("G3600") || name.contains("G2010") || name.contains("G2000") ||
            name.contains("G3020") || name.contains("G3060") || name.contains("G1010") ||
            name.contains("G2020") -> PIXMA_G3010_PROFILE

            // ADF-equipped G series
            name.contains("G4010") || name.contains("G4000") || name.contains("G4210") ||
            name.contains("G7000") || name.contains("G7020") || name.contains("GM4000") ||
            name.contains("GX6000") || name.contains("GX7000") -> PIXMA_G4010_PROFILE

            // MAXIFY MB series (Duplex ADF)
            name.contains("MAXIFY") || name.contains("MB2") || name.contains("MB5") ||
            name.contains("IB4") -> MAXIFY_MB_PROFILE

            // PIXMA TS Photo series
            name.contains("TS8") || name.contains("TS9") || name.contains("TS6") ||
            name.contains("TS5") -> PIXMA_TS_PHOTO_PROFILE

            // CanoScan LiDE
            name.contains("LIDE") || name.contains("CANOSCAN") -> CANOSCAN_LIDE_PROFILE

            // Default to G3010 profile if PIXMA is mentioned, else Generic
            name.contains("PIXMA") -> PIXMA_G3010_PROFILE
            else -> GENERIC_PROFILE
        }
    }

    /**
     * Validates [settings] against the machine's [capabilities].
     * Returns a [ValidationResult] with warnings and auto-corrected settings if constraints are violated.
     */
    fun validate(settings: ScanSettings, capabilities: ScannerHardwareCapabilities): ValidationResult {
        val warnings = mutableListOf<String>()
        var adjusted = settings

        // 1. Validate Scan Source
        if (settings.source !in capabilities.supportedSources) {
            val fallbackSource = capabilities.supportedSources.firstOrNull() ?: ScanSource.FLATBED
            warnings.add("${capabilities.displayName} does not support ${settings.source.label}. Switched to ${fallbackSource.label}.")
            adjusted = adjusted.copy(source = fallbackSource)
        }

        // 2. Validate DPI
        if (settings.dpi !in capabilities.supportedDpis) {
            val maxAllowedDpi = capabilities.supportedDpis.maxByOrNull { it.value } ?: ScanDpi.DPI_600
            warnings.add("${settings.dpi.value} DPI exceeds the hardware capability of ${capabilities.displayName} (Max ${capabilities.maxOpticalDpi} DPI). Adjusted to ${maxAllowedDpi.value} DPI.")
            adjusted = adjusted.copy(dpi = maxAllowedDpi)
        }

        // 3. Validate Page Size vs Scan Source
        if (adjusted.pageSize.isAdfOnly && adjusted.source == ScanSource.FLATBED) {
            warnings.add("${adjusted.pageSize.label} length exceeds the physical flatbed glass (297 mm). Platen supports up to A4 / Letter.")
            if (capabilities.hasAdf) {
                // Machine has ADF, suggest ADF
                warnings.add("Tip: Switch Scan Source to ADF to scan Legal documents.")
            } else {
                // Force to A4/Letter
                adjusted = adjusted.copy(pageSize = ScanPageSize.A4)
            }
        }

        // 4. Validate Custom Area bounds against platen
        if (adjusted.pageSize == ScanPageSize.CUSTOM) {
            val maxWidth = capabilities.maxPlatenWidthMm
            val maxHeight = if (adjusted.source != ScanSource.FLATBED && capabilities.hasAdf) 355.6 else capabilities.maxPlatenHeightMm

            var w = adjusted.customWidthMm
            var h = adjusted.customHeightMm
            var xOff = adjusted.xOffsetMm
            var yOff = adjusted.yOffsetMm

            if (w <= 0.0) w = 210.0
            if (h <= 0.0) h = 297.0
            if (xOff < 0.0) xOff = 0.0
            if (yOff < 0.0) yOff = 0.0

            if (w + xOff > maxWidth) {
                warnings.add("Custom width + X offset (${w + xOff} mm) exceeds maximum bed width (${maxWidth} mm). Cropped to fit.")
                w = Math.max(10.0, maxWidth - xOff)
            }
            if (h + yOff > maxHeight) {
                warnings.add("Custom height + Y offset (${h + yOff} mm) exceeds maximum bed length (${maxHeight} mm). Cropped to fit.")
                h = Math.max(10.0, maxHeight - yOff)
            }

            adjusted = adjusted.copy(
                customWidthMm = w,
                customHeightMm = h,
                xOffsetMm = xOff,
                yOffsetMm = yOff
            )
        }

        // 5. Validate Color Mode
        if (adjusted.colorMode !in capabilities.supportedColorModes) {
            val fallbackMode = capabilities.supportedColorModes.firstOrNull() ?: ScanColorMode.COLOR
            warnings.add("Color mode ${adjusted.colorMode.label} not supported. Switched to ${fallbackMode.label}.")
            adjusted = adjusted.copy(colorMode = fallbackMode)
        }

        val isValid = warnings.isEmpty()
        return ValidationResult(
            isValid = isValid,
            warnings = warnings,
            adjustedSettings = adjusted
        )
    }
}
