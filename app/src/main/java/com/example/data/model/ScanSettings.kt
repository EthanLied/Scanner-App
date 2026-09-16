package com.example.data.model

enum class ScanSource(val code: Byte, val label: String, val description: String) {
    FLATBED(0x01.toByte(), "Flatbed Platen", "Glass flatbed scanner"),
    ADF_SIMPLEX(0x02.toByte(), "ADF (1-Sided)", "Automatic document feeder simplex"),
    ADF_DUPLEX(0x04.toByte(), "ADF (2-Sided)", "Automatic document feeder duplex")
}

enum class ScanDpi(val value: Int) {
    DPI_75(75),
    DPI_150(150),
    DPI_300(300),
    DPI_600(600),
    DPI_1200(1200),
    DPI_2400(2400);

    override fun toString(): String = "$value DPI"
}

enum class ScanColorMode(val code: Byte, val bpp: Byte, val label: String) {
    COLOR(0x08.toByte(), 0x18.toByte(), "Color (24-bit RGB)"),
    GRAYSCALE(0x04.toByte(), 0x08.toByte(), "Grayscale (8-bit Gray)"),
    LINE_ART(0x02.toByte(), 0x01.toByte(), "Black & White (1-bit Line Art)")
}

enum class ScanPageSize(
    val label: String,
    val widthMm: Double,
    val heightMm: Double,
    val isAdfOnly: Boolean = false
) {
    A4("A4 (210 × 297 mm)", 210.0, 297.0),
    US_LETTER("US Letter (8.5 × 11 in)", 215.9, 279.4),
    US_LEGAL("US Legal (8.5 × 14 in)", 215.9, 355.6, isAdfOnly = true),
    PHOTO_4X6("Photo 4 × 6 in (10 × 15 cm)", 101.6, 152.4),
    PHOTO_5X7("Photo 5 × 7 in (13 × 18 cm)", 127.0, 177.8),
    BUSINESS_CARD("Business Card (54 × 86 mm)", 54.0, 86.0),
    CUSTOM("Custom Crop Area", 0.0, 0.0)
}

enum class ScanJpegQuality(val code: Byte, val label: String, val qualityPercent: Int) {
    HIGH(0xFF.toByte(), "High (100% - Archival)", 100),
    BALANCED(0xC8.toByte(), "Standard (85% - Recommended)", 85),
    COMPACT(0x80.toByte(), "Compact (50% - Fast Transfer)", 50)
}

data class ScanEnhancement(
    val brightness: Int = 0, // -5 to +5
    val contrast: Int = 0,   // -5 to +5
    val autoDeskew: Boolean = false,
    val invertColors: Boolean = false
) {
    val hasEnhancements: Boolean
        get() = brightness != 0 || contrast != 0 || autoDeskew || invertColors
}

data class ScanSettings(
    val source: ScanSource = ScanSource.FLATBED,
    val dpi: ScanDpi = ScanDpi.DPI_300,
    val colorMode: ScanColorMode = ScanColorMode.COLOR,
    val pageSize: ScanPageSize = ScanPageSize.A4,
    val customWidthMm: Double = 210.0,
    val customHeightMm: Double = 297.0,
    val xOffsetMm: Double = 0.0,
    val yOffsetMm: Double = 0.0,
    val jpegQuality: ScanJpegQuality = ScanJpegQuality.HIGH,
    val enhancement: ScanEnhancement = ScanEnhancement()
) {
    val effectiveWidthMm: Double
        get() = if (pageSize == ScanPageSize.CUSTOM) customWidthMm else pageSize.widthMm

    val effectiveHeightMm: Double
        get() = if (pageSize == ScanPageSize.CUSTOM) customHeightMm else pageSize.heightMm

    /**
     * Compute pixel dimensions strictly according to formula:
     * width_px  = round(page_width_mm  / 25.4 * dpi)
     * height_px = round(page_height_mm / 25.4 * dpi)
     * NOTE: Width is NOT rounded up to a multiple of 32 in JPEG mode.
     */
    val widthPx: Int
        get() = Math.round(effectiveWidthMm / 25.4 * dpi.value).toInt()

    val heightPx: Int
        get() = Math.round(effectiveHeightMm / 25.4 * dpi.value).toInt()

    val xOffsetPx: Int
        get() = Math.round(xOffsetMm / 25.4 * dpi.value).toInt()

    val yOffsetPx: Int
        get() = Math.round(yOffsetMm / 25.4 * dpi.value).toInt()
}
