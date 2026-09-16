package com.example.protocol.chmp

import com.example.data.model.ScanSettings

object ChmpConstants {
    const val SCANNER_ENDPOINT_PORT3 = "/canon/ij/command2/port3"
    const val CONTROL_ENDPOINT_PORT1 = "/canon/ij/command1/port1"
    const val STATUS_ENDPOINT_PORT1 = "/canon/ij/command2/port1"

    const val CHMP_VERSION_REQUEST = "1.4.0"
    const val CHMP_TIMEOUT_HEADER = "20"
    const val SOCKET_TIMEOUT_MS = 30000

    // XML bodies
    const val XML_MODESHIFT_PRE =
        """<?xml version="1.0" encoding="utf-8" ?><cmd xmlns:ivec="http://www.canon.com/ns/cmd/2008/07/common/" xmlns:vcn="http://www.canon.com/ns/cmd/2008/07/canon/"><ivec:contents><ivec:operation>VendorCmd</ivec:operation><ivec:param_set servicetype="scan"><ivec:jobID> </ivec:jobID><vcn:ijoperation>ModeShift</vcn:ijoperation><vcn:ijmode>1</vcn:ijmode></ivec:param_set></ivec:contents></cmd>"""

    const val XML_STARTJOB =
        """<?xml version="1.0" encoding="utf-8" ?><cmd xmlns:ivec="http://www.canon.com/ns/cmd/2008/07/common/"><ivec:contents><ivec:operation>StartJob</ivec:operation><ivec:param_set servicetype="scan"><ivec:jobID>00000001</ivec:jobID><ivec:bidi>1</ivec:bidi></ivec:param_set></ivec:contents></cmd>"""

    const val XML_MODESHIFT_IN =
        """<?xml version="1.0" encoding="utf-8" ?><cmd xmlns:ivec="http://www.canon.com/ns/cmd/2008/07/common/" xmlns:vcn="http://www.canon.com/ns/cmd/2008/07/canon/"><ivec:contents><ivec:operation>VendorCmd</ivec:operation><ivec:param_set servicetype="scan"><ivec:jobID>00000001</ivec:jobID><vcn:ijoperation>ModeShift</vcn:ijoperation><vcn:ijmode>1</vcn:ijmode></ivec:param_set></ivec:contents></cmd>"""

    const val XML_ENDJOB =
        """<?xml version="1.0" encoding="utf-8" ?><cmd xmlns:ivec="http://www.canon.com/ns/cmd/2008/07/common/"><ivec:contents><ivec:operation>EndJob</ivec:operation><ivec:param_set servicetype="scan"><ivec:jobID>00000001</ivec:jobID></ivec:param_set></ivec:contents></cmd>"""

    // Exact 16-byte binary command frames
    val CMD_CAPABILITY_QUERY = byteArrayOf(
        0xf3.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x10.toByte()
    )

    val CMD_START_SESSION = byteArrayOf(
        0xdb.toByte(), 0x20.toByte(), 0x00, 0x01,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    )

    val CMD_SCAN_START3 = byteArrayOf(
        0xd9.toByte(), 0x20.toByte(), 0x00, 0x01,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    )

    val CMD_STATUS3 = byteArrayOf(
        0xda.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x08
    )

    val CMD_GET_DIMENSIONS = byteArrayOf(
        0xdc.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x08
    )

    val CMD_READ_IMAGE = byteArrayOf(
        0xd4.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x20.toByte(), 0x00, 0x00
    )

    val CMD_ABORT_SESSION = byteArrayOf(
        0xef.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    )

    val HEADER_SCAN_PARAM3 = byteArrayOf(
        0xd8.toByte(), 0x20.toByte(), 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x38.toByte()
    )

    // Response Status Codes (2 bytes big-endian)
    const val STATUS_OK = 0x0606
    const val STATUS_BUSY = 0x1414
    const val STATUS_FAILED = 0x1515

    fun buildScanParam3Payload(settings: ScanSettings): ByteArray {
        val params = ByteArray(56)

        // 0x00 1 source: 1 = flatbed, 2 = ADF simplex, 4 = ADF duplex
        params[0x00] = settings.source.code
        // 0x01 1 0x01 constant
        params[0x01] = 0x01
        // 0x02 1 0x01 constant
        params[0x02] = 0x01

        // 0x08 2 X DPI, big-endian, ENCODED AS (dpi | 0x8000)
        val encodedDpi = (settings.dpi.value or 0x8000)
        params[0x08] = ((encodedDpi shr 8) and 0xFF).toByte()
        params[0x09] = (encodedDpi and 0xFF).toByte()

        // 0x0A 2 Y DPI, same encoding, same value as X
        params[0x0A] = ((encodedDpi shr 8) and 0xFF).toByte()
        params[0x0B] = (encodedDpi and 0xFF).toByte()

        // 0x0C 4 X offset in pixels, big-endian
        val xOffset = settings.xOffsetPx
        params[0x0C] = ((xOffset shr 24) and 0xFF).toByte()
        params[0x0D] = ((xOffset shr 16) and 0xFF).toByte()
        params[0x0E] = ((xOffset shr 8) and 0xFF).toByte()
        params[0x0F] = (xOffset and 0xFF).toByte()

        // 0x10 4 Y offset in pixels, big-endian
        val yOffset = settings.yOffsetPx
        params[0x10] = ((yOffset shr 24) and 0xFF).toByte()
        params[0x11] = ((yOffset shr 16) and 0xFF).toByte()
        params[0x12] = ((yOffset shr 8) and 0xFF).toByte()
        params[0x13] = (yOffset and 0xFF).toByte()

        // 0x14 4 width in pixels, big-endian (NOT rounded to 32)
        val width = settings.widthPx
        params[0x14] = ((width shr 24) and 0xFF).toByte()
        params[0x15] = ((width shr 16) and 0xFF).toByte()
        params[0x16] = ((width shr 8) and 0xFF).toByte()
        params[0x17] = (width and 0xFF).toByte()

        // 0x18 4 height in pixels, big-endian
        val height = settings.heightPx
        params[0x18] = ((height shr 24) and 0xFF).toByte()
        params[0x19] = ((height shr 16) and 0xFF).toByte()
        params[0x1A] = ((height shr 8) and 0xFF).toByte()
        params[0x1B] = (height and 0xFF).toByte()

        // 0x1C 1 colour mode: 0x08 = colour, 0x04 = grayscale, 0x02 = lineart
        params[0x1C] = settings.colorMode.code

        // 0x1D 1 bits per pixel: 0x18 (24) for colour, 0x08 (8) for grayscale, 0x01 for lineart
        params[0x1D] = settings.colorMode.bpp

        // 0x1F 1 0x01
        params[0x1F] = 0x01
        // 0x20 1 JPEG compression / quality parameter (0xFF = High, 0xC8 = Balanced, 0x80 = Compact)
        params[0x20] = settings.jpegQuality.code
        // 0x21 1 0x82 output format: 0x82 = JPEG. MUST be 0x82.
        params[0x21] = 0x82.toByte()
        // 0x23 1 0x02
        params[0x23] = 0x02
        // 0x24 1 0x01
        params[0x24] = 0x01
        // 0x30 1 0x01
        params[0x30] = 0x01

        // 0x37 1 checksum: all parameter bytes sum to 0x00 mod 256
        // checksum = (0 - sum(params[0 .. n-2])) and 0xFF
        var sum = 0
        for (i in 0 until 55) {
            sum += (params[i].toInt() and 0xFF)
        }
        params[0x37] = ((0 - sum) and 0xFF).toByte()

        return params
    }

    fun buildScanParam3(settings: ScanSettings): ByteArray {
        val payload = buildScanParam3Payload(settings)
        val fullCommand = ByteArray(72)
        System.arraycopy(HEADER_SCAN_PARAM3, 0, fullCommand, 0, 16)
        System.arraycopy(payload, 0, fullCommand, 16, 56)
        return fullCommand
    }
}
