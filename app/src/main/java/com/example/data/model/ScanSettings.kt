package com.example.data.model

enum class ScanDpi(val value: Int) {
    DPI_75(75),
    DPI_150(150),
    DPI_300(300),
    DPI_600(600),
    DPI_1200(1200);

    override fun toString(): String = "$value DPI"
}

enum class ScanColorMode(val code: Byte, val bpp: Byte, val label: String) {
    COLOR(0x08.toByte(), 0x18.toByte(), "Color (24-bit)"),
    GRAYSCALE(0x04.toByte(), 0x08.toByte(), "Grayscale (8-bit)")
}

enum class ScanPageSize(val label: String, val widthMm: Double, val heightMm: Double) {
    A4("A4 (210 × 297 mm)", 210.0, 297.0),
    US_LETTER("US Letter (8.5 × 11 in)", 215.9, 279.4)
}

data class ScanSettings(
    val dpi: ScanDpi = ScanDpi.DPI_300,
    val colorMode: ScanColorMode = ScanColorMode.COLOR,
    val pageSize: ScanPageSize = ScanPageSize.A4
) {
    /**
     * Compute pixel dimensions strictly according to formula:
     * width_px  = round(page_width_mm  / 25.4 * dpi)
     * height_px = round(page_height_mm / 25.4 * dpi)
     * NOTE: Width is NOT rounded up to a multiple of 32 in JPEG mode.
     */
    val widthPx: Int
        get() = Math.round(pageSize.widthMm / 25.4 * dpi.value).toInt()

    val heightPx: Int
        get() = Math.round(pageSize.heightMm / 25.4 * dpi.value).toInt()
}
