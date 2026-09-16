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

class Rung2ChmpPort1Probe(
    private val wifiNetworkManager: WifiNetworkManager
) : ScannerTransport {

    override val name: String = "Rung 2: CHMP Endpoint Discovery (/canon/ij/command2/port1)"

    private val getStatusXml =
        """<?xml version="1.0" encoding="utf-8" ?><cmd xmlns:ivec="http://www.canon.com/ns/cmd/2008/07/common/"><ivec:contents><ivec:operation>GetStatus</ivec:operation><ivec:param_set servicetype="device"></ivec:param_set></ivec:contents></cmd>"""

    override suspend fun probe(ip: String): ProbeResult = withContext(Dispatchers.IO) {
        val endpoint = "/canon/ij/command2/port1"
        ProtocolLogger.log("SYS", endpoint, "Rung 2 Probe", "", "Starting", 0, "Testing port1 device status on $ip")
        var socket: Socket? = null
        try {
            socket = wifiNetworkManager.createBoundSocket()
            socket.tcpNoDelay = true
            socket.soTimeout = 8000
            socket.connect(InetSocketAddress(ip, 80), 4000)

            val out = BufferedOutputStream(socket.getOutputStream())
            val `in` = BufferedInputStream(socket.getInputStream())

            val bodyBytes = getStatusXml.toByteArray(StandardCharsets.UTF_8)
            val req = "POST $endpoint HTTP/1.1\r\n" +
                    "Host: $ip\r\n" +
                    "X-CHMP-Version: 1.4.0\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: ${bodyBytes.size}\r\n" +
                    "Connection: Keep-Alive\r\n\r\n"

            out.write(req.toByteArray(StandardCharsets.US_ASCII))
            out.write(bodyBytes)
            out.flush()

            val postStatus = readLine(`in`) ?: "EOF"
            // Drain POST headers
            while (true) {
                val line = readLine(`in`) ?: break
                if (line.isEmpty()) break
            }

            val getReq = "GET $endpoint HTTP/1.1\r\n" +
                    "Host: $ip\r\n" +
                    "Connection: Keep-Alive\r\n" +
                    "X-CHMP-Version: 1.4.0\r\n\r\n"
            out.write(getReq.toByteArray(StandardCharsets.US_ASCII))
            out.flush()

            val getStatus = readLine(`in`) ?: "EOF"
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(`in`) ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }

            val body = ByteArrayOutputStream()
            val cl = headers["content-length"]?.toIntOrNull() ?: 512
            val buf = ByteArray(1024)
            val read = `in`.read(buf, 0, Math.min(buf.size, cl))
            if (read > 0) body.write(buf, 0, read)
            val respText = body.toString(StandardCharsets.UTF_8.name())

            ProtocolLogger.log("RX", endpoint, "Rung 2 Status Reply", "", getStatus, read, respText)

            if (getStatus.contains("200") && respText.contains("<ivec:response>OK</ivec:response>")) {
                ProbeResult(
                    success = true,
                    rungName = name,
                    message = "CHMP port1 is alive! Printer confirmed running CHMP protocol.",
                    details = respText
                )
            } else {
                ProbeResult(
                    success = false,
                    rungName = name,
                    message = "Port 1 responded with status: $getStatus",
                    details = respText
                )
            }
        } catch (e: Exception) {
            ProtocolLogger.log("SYS", endpoint, "Rung 2 Probe Error", "", "FAIL", 0, e.message ?: "")
            ProbeResult(
                success = false,
                rungName = name,
                message = "Port 1 unreachable: ${e.message ?: e.javaClass.simpleName}",
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
            error = "Port 1 is a device management query port and does not support scan acquisition. Scan requests return HTTP 409."
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
