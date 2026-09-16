package com.example.protocol.fallback

import com.example.data.model.ScanSettings
import com.example.protocol.ProbeResult
import com.example.protocol.ProtocolLogger
import com.example.protocol.ScanPageResult
import com.example.protocol.ScanProgress
import com.example.protocol.ScannerTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class Rung4BjnpProbe : ScannerTransport {

    override val name: String = "Rung 4: BJNP Protocol Probe (UDP 8612/8610)"

    override suspend fun probe(ip: String): ProbeResult = withContext(Dispatchers.IO) {
        val endpoint = "UDP $ip:8612/8610"
        ProtocolLogger.log("SYS", endpoint, "Rung 4 BJNP Probe", "", "UDP", 16, "Sending BJNP discovery broadcast to $ip")
        var socket: DatagramSocket? = null
        try {
            // BJNP 16-byte header:
            // "BJNP" (4 bytes)
            // byte[4] = 0x02 (Scan)
            // byte[5] = 0x01 (Discover)
            // byte[6..7] = session id / seq (zeros)
            // byte[8..11] = payload size (zeros)
            // byte[12..15] = zeros
            val packetData = byteArrayOf(
                'B'.code.toByte(), 'J'.code.toByte(), 'N'.code.toByte(), 'P'.code.toByte(),
                0x02, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            )

            socket = DatagramSocket()
            socket.soTimeout = 2500

            val targetAddr = InetAddress.getByName(ip)
            val sendPacket = DatagramPacket(packetData, packetData.size, targetAddr, 8612)
            socket.send(sendPacket)

            val recvBuf = ByteArray(512)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)

            val recvHex = ProtocolLogger.formatHex(recvBuf.copyOf(recvPacket.length), 32)
            ProtocolLogger.log("RX", endpoint, "Rung 4 BJNP Reply", recvHex, "REPLY", recvPacket.length)

            if (recvPacket.length >= 4 &&
                recvBuf[0] == 'B'.code.toByte() && recvBuf[1] == 'J'.code.toByte() &&
                recvBuf[2] == 'N'.code.toByte() && recvBuf[3] == 'P'.code.toByte()
            ) {
                ProbeResult(
                    success = true,
                    rungName = name,
                    message = "BJNP response received from $ip (${recvPacket.length} bytes)",
                    details = "Payload: $recvHex"
                )
            } else {
                ProbeResult(
                    success = false,
                    rungName = name,
                    message = "Non-BJNP UDP packet received",
                    details = "Payload: $recvHex"
                )
            }
        } catch (e: SocketTimeoutException) {
            ProtocolLogger.log("SYS", endpoint, "Rung 4 BJNP Probe", "", "TIMEOUT", 0, "No BJNP answer over Wi-Fi (expected for G3010)")
            ProbeResult(
                success = false,
                rungName = name,
                message = "BJNP probe timed out (G3010 does not respond to BJNP over Wi-Fi)",
                details = "SocketTimeoutException: No response received on UDP 8612 within 2500ms."
            )
        } catch (e: Exception) {
            ProtocolLogger.log("SYS", endpoint, "Rung 4 BJNP Error", "", "FAIL", 0, e.message ?: "")
            ProbeResult(
                success = false,
                rungName = name,
                message = "BJNP error: ${e.message ?: e.javaClass.simpleName}",
                details = e.stackTraceToString()
            )
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
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
            error = "BJNP scanning is unsupported on G3010 Wi-Fi interface."
        )
    }
}
