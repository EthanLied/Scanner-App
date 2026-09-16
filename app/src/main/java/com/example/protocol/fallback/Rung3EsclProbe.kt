package com.example.protocol.fallback

import com.example.data.model.ScanSettings
import com.example.network.WifiNetworkManager
import com.example.protocol.ProbeResult
import com.example.protocol.ProtocolLogger
import com.example.protocol.ScanPageResult
import com.example.protocol.ScanProgress
import com.example.protocol.ScannerTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class Rung3EsclProbe(
    private val wifiNetworkManager: WifiNetworkManager
) : ScannerTransport {

    override val name: String = "Rung 3: eSCL / AirScan Probe (/eSCL/ScannerCapabilities)"

    override suspend fun probe(ip: String): ProbeResult = withContext(Dispatchers.IO) {
        val endpoint = "/eSCL/ScannerCapabilities"
        ProtocolLogger.log("SYS", endpoint, "Rung 3 eSCL Probe", "", "GET", 0, "Checking eSCL capability on $ip")
        var socket: Socket? = null
        try {
            socket = wifiNetworkManager.createBoundSocket()
            socket.tcpNoDelay = true
            socket.soTimeout = 5000
            socket.connect(InetSocketAddress(ip, 80), 3000)

            val out = BufferedOutputStream(socket.getOutputStream())
            val `in` = BufferedInputStream(socket.getInputStream())

            val req = "GET $endpoint HTTP/1.1\r\n" +
                    "Host: $ip\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(req.toByteArray(StandardCharsets.US_ASCII))
            out.flush()

            val statusLine = readLine(`in`) ?: "EOF"
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(`in`) ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }

            val body = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            var read: Int
            while (`in`.read(buf).also { read = it } != -1) {
                body.write(buf, 0, read)
            }
            val respText = body.toString(StandardCharsets.UTF_8.name())
            ProtocolLogger.log("RX", endpoint, "Rung 3 eSCL Reply", "", statusLine, body.size(), respText.take(120))

            if (statusLine.contains("200") && respText.contains("<scan:ScannerCapabilities", ignoreCase = true)) {
                ProbeResult(
                    success = true,
                    rungName = name,
                    message = "eSCL ScannerCapabilities found! Device supports AirScan/eSCL.",
                    details = respText
                )
            } else {
                ProbeResult(
                    success = false,
                    rungName = name,
                    message = "eSCL not supported: HTTP $statusLine (expected on Canon G3010)",
                    details = respText
                )
            }
        } catch (e: Exception) {
            ProtocolLogger.log("SYS", endpoint, "Rung 3 eSCL Probe Error", "", "FAIL", 0, e.message ?: "")
            ProbeResult(
                success = false,
                rungName = name,
                message = "eSCL unreachable: ${e.message ?: e.javaClass.simpleName} (G3010 does not support eSCL)",
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
            error = "eSCL is not supported by Canon PIXMA G3010 hardware."
        )
    }

    private fun readLine(`in`: BufferedInputStream): String? {
        val baos = ByteArrayOutputStream()
        var c: Int
        while (`in`.read().also { c = it } != -1) {
            if (c == '\r'.code) {
                val next = `in`.read()
                if (next == '\n'.code) break
                if (next != -1) { baos.write(c); baos.write(next) }
            } else if (c == '\n'.code) {
                break
            } else {
                baos.write(c)
            }
        }
        if (c == -1 && baos.size() == 0) return null
        return baos.toString(StandardCharsets.US_ASCII.name())
    }
}
