package com.example

import com.example.data.model.ScanColorMode
import com.example.data.model.ScanDpi
import com.example.data.model.ScanEnhancement
import com.example.data.model.ScanJpegQuality
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings
import com.example.data.model.ScanSource
import com.example.data.model.ScannerModelRegistry
import com.example.protocol.chmp.ChmpConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerModelValidationTest {

    @Test
    fun testResolveModelCapabilities() {
        val g3010Caps = ScannerModelRegistry.resolveCapabilities("Canon G3010 series")
        assertEquals("Canon PIXMA G3010", g3010Caps.displayName)
        assertFalse(g3010Caps.hasAdf)
        assertEquals(600, g3010Caps.maxOpticalDpi)

        val g4010Caps = ScannerModelRegistry.resolveCapabilities("Canon G4010 series")
        assertEquals("Canon PIXMA G4010 (with ADF)", g4010Caps.displayName)
        assertTrue(g4010Caps.hasAdf)

        val tsCaps = ScannerModelRegistry.resolveCapabilities("Canon TS8300 series")
        assertEquals(2400, tsCaps.maxOpticalDpi)

        val maxifyCaps = ScannerModelRegistry.resolveCapabilities("Canon MB5420")
        assertTrue(maxifyCaps.hasAdfDuplex)

        val unknownCaps = ScannerModelRegistry.resolveCapabilities(null)
        assertEquals("Canon PIXMA G3010", unknownCaps.displayName)
    }

    @Test
    fun testAdfValidationOnFlatbedDevice() {
        val g3010 = ScannerModelRegistry.PIXMA_G3010_PROFILE

        // User attempts to select ADF simplex on G3010
        val requested = ScanSettings(
            source = ScanSource.ADF_SIMPLEX,
            dpi = ScanDpi.DPI_300,
            pageSize = ScanPageSize.A4
        )

        val result = ScannerModelRegistry.validate(requested, g3010)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("does not support") })
        assertEquals(ScanSource.FLATBED, result.adjustedSettings.source)
    }

    @Test
    fun testDpiValidationAgainstHardwareLimit() {
        val g3010 = ScannerModelRegistry.PIXMA_G3010_PROFILE // Max 600 DPI

        // User attempts to scan at 2400 DPI
        val requested = ScanSettings(
            source = ScanSource.FLATBED,
            dpi = ScanDpi.DPI_2400,
            pageSize = ScanPageSize.A4
        )

        val result = ScannerModelRegistry.validate(requested, g3010)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("exceeds the hardware capability") })
        assertEquals(ScanDpi.DPI_600, result.adjustedSettings.dpi)
    }

    @Test
    fun testPlatenSizeBoundaryValidation() {
        val g3010 = ScannerModelRegistry.PIXMA_G3010_PROFILE

        // US Legal is 355.6mm, but G3010 flatbed glass is only 297mm
        val requested = ScanSettings(
            source = ScanSource.FLATBED,
            dpi = ScanDpi.DPI_300,
            pageSize = ScanPageSize.US_LEGAL
        )

        val result = ScannerModelRegistry.validate(requested, g3010)
        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("exceeds the physical flatbed glass") })
        assertEquals(ScanPageSize.A4, result.adjustedSettings.pageSize)
    }

    @Test
    fun testValidSettingsPassCleanly() {
        val g3010 = ScannerModelRegistry.PIXMA_G3010_PROFILE

        val valid = ScanSettings(
            source = ScanSource.FLATBED,
            dpi = ScanDpi.DPI_300,
            colorMode = ScanColorMode.COLOR,
            pageSize = ScanPageSize.A4,
            jpegQuality = ScanJpegQuality.HIGH,
            enhancement = ScanEnhancement(brightness = 1, contrast = 1)
        )

        val result = ScannerModelRegistry.validate(valid, g3010)
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
        assertEquals(valid, result.adjustedSettings)
    }

    @Test
    fun testScanParam3PayloadSerializationAndChecksum() {
        val settings = ScanSettings(
            source = ScanSource.FLATBED,
            dpi = ScanDpi.DPI_300,
            colorMode = ScanColorMode.COLOR,
            pageSize = ScanPageSize.A4,
            jpegQuality = ScanJpegQuality.BALANCED
        )

        val payload = ChmpConstants.buildScanParam3Payload(settings)
        assertEquals(56, payload.size)

        // Byte 0: source (1 = flatbed)
        assertEquals(0x01.toByte(), payload[0])

        // Byte 8-9: encoded DPI (300 or 0x8000 = 0x812C -> 0x81, 0x2C)
        val expectedHigh = ((300 or 0x8000) shr 8).toByte()
        val expectedLow = ((300 or 0x8000) and 0xFF).toByte()
        assertEquals(expectedHigh, payload[0x08])
        assertEquals(expectedLow, payload[0x09])

        // Byte 0x1C: color mode (0x08 for color)
        assertEquals(0x08.toByte(), payload[0x1C])

        // Byte 0x1D: bits per pixel (0x18 = 24 for color)
        assertEquals(0x18.toByte(), payload[0x1D])

        // Byte 0x20: JPEG compression quality (0xC8 for Standard)
        assertEquals(0xC8.toByte(), payload[0x20])

        // Byte 0x21: JPEG format (0x82)
        assertEquals(0x82.toByte(), payload[0x21])

        // Byte 0x37: Modulo 256 checksum: sum of all 56 bytes must equal 0 mod 256
        var sum = 0
        for (b in payload) {
            sum = (sum + (b.toInt() and 0xFF)) and 0xFF
        }
        assertEquals("Modulo 256 sum of payload must be 0", 0, sum)
    }

    @Test
    fun testUnsupportedOptionsAreFilteredOutToSaveSpace() {
        val g3010 = ScannerModelRegistry.PIXMA_G3010_PROFILE

        // 1. Scan source filtering: Flatbed only, ADF options hidden
        val visibleSources = ScanSource.values().filter { it in g3010.supportedSources }
        assertEquals(listOf(ScanSource.FLATBED), visibleSources)
        assertFalse("ADF Simplex must be hidden on flatbed devices", visibleSources.contains(ScanSource.ADF_SIMPLEX))
        assertFalse("ADF Duplex must be hidden on flatbed devices", visibleSources.contains(ScanSource.ADF_DUPLEX))

        // 2. Optical DPI filtering: DPIs exceeding hardware limit (600) are hidden
        val visibleDpis = ScanDpi.values().filter { it in g3010.supportedDpis }
        assertEquals(listOf(ScanDpi.DPI_75, ScanDpi.DPI_150, ScanDpi.DPI_300, ScanDpi.DPI_600), visibleDpis)
        assertFalse("1200 DPI must be hidden on 600 DPI scanner", visibleDpis.contains(ScanDpi.DPI_1200))
        assertFalse("2400 DPI must be hidden on 600 DPI scanner", visibleDpis.contains(ScanDpi.DPI_2400))

        // 3. Flatbed glass size filtering: Sizes exceeding 297mm height (US Legal = 355.6mm) are hidden
        val visibleFlatbedSizes = ScanPageSize.values().filter { size ->
            size != ScanPageSize.CUSTOM &&
                    size.heightMm <= g3010.maxPlatenHeightMm &&
                    size.widthMm <= g3010.maxPlatenWidthMm
        }
        assertFalse("US Legal must be hidden when scanning from Flatbed glass", visibleFlatbedSizes.contains(ScanPageSize.US_LEGAL))
        assertTrue("A4 must be visible on Flatbed", visibleFlatbedSizes.contains(ScanPageSize.A4))
        assertTrue("US Letter must be visible on Flatbed", visibleFlatbedSizes.contains(ScanPageSize.US_LETTER))
    }
}

