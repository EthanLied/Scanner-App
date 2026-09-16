package com.example.protocol.fallback

import com.example.data.model.ScanSettings
import com.example.protocol.ProbeResult
import com.example.protocol.ProtocolLogger
import com.example.protocol.ScanPageResult
import com.example.protocol.ScanProgress
import com.example.protocol.ScannerTransport
import java.io.File

class Rung5UsbOtgStub : ScannerTransport {

    override val name: String = "Rung 5: USB OTG Bulk (VID 0x04A9, PID 0x183B)"

    override suspend fun probe(ip: String): ProbeResult {
        ProtocolLogger.log("SYS", "USB", "Rung 5 Probe", "", "STUB", 0, "USB OTG checked")
        return ProbeResult(
            success = false,
            rungName = name,
            message = "USB OTG transport is not implemented yet.",
            details = """
                To finish USB OTG support:
                1. Request android.hardware.usb.host permission in AndroidManifest.xml.
                2. Use android.hardware.usb.UsbManager to enumerate attached USB devices matching VID 0x04A9 and PID 0x183B.
                3. Request runtime USB device access permission via PendingIntent / UsbManager.requestPermission().
                4. Find the bulk IN and bulk OUT endpoints on the scanner interface.
                5. Open a UsbDeviceConnection and claim the interface.
                6. Transmit the identical CHMP binary command frames (CapabilityQuery, StartSession, ScanParam3, ScanStart3, Status3, GetDimensions, ReadImage, AbortSession) directly via UsbDeviceConnection.bulkTransfer() instead of HTTP POST/GET.
            """.trimIndent()
        )
    }

    override suspend fun scanPage(
        ip: String,
        settings: ScanSettings,
        destinationFile: File,
        progressListener: (ScanProgress) -> Unit
    ): ScanPageResult {
        return ScanPageResult(
            success = false,
            file = null,
            widthPx = settings.widthPx,
            heightPx = settings.heightPx,
            deliveredLines = 0,
            error = "USB OTG transport is not implemented yet. Connect via Wi-Fi using Rung 1 (CHMP)."
        )
    }
}
