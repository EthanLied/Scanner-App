package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ScanColorMode
import com.example.data.model.ScanDpi
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings
import com.example.protocol.chmp.ChmpConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PIXMA G3010 Scanner", appName)
    }

    @Test
    fun `test scan param checksum calculation`() {
        val settings = ScanSettings(
            dpi = ScanDpi.DPI_300,
            colorMode = ScanColorMode.COLOR,
            pageSize = ScanPageSize.A4
        )
        val params = ChmpConstants.buildScanParam3Payload(settings)
        assertEquals(56, params.size)

        var sum = 0
        for (i in 0 until params.size - 1) {
            sum += (params[i].toInt() and 0xFF)
        }
        val expectedChecksum = ((0 - sum) and 0xFF).toByte()
        assertEquals(expectedChecksum, params.last())
        assertEquals(0x82.toByte(), params[0x21]) // JPEG
    }
}
