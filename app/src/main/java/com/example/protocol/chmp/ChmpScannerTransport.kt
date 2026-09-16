package com.example.protocol.chmp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.model.ScanSettings
import com.example.network.WifiNetworkManager
import com.example.protocol.ProbeResult
import com.example.protocol.ProtocolLogger
import com.example.protocol.ScanPageResult
import com.example.protocol.ScanProgress
import com.example.protocol.ScannerTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class ChmpScannerTransport(
    private val wifiNetworkManager: WifiNetworkManager
) : ScannerTransport {

    override val name: String = "CHMP (Port 80 /canon/ij/command2/port3)"

    override suspend fun probe(ip: String): ProbeResult = withContext(Dispatchers.IO) {
        val endpoint = ChmpConstants.SCANNER_ENDPOINT_PORT3
        ProtocolLogger.log("SYS", endpoint, "Probe", "", "Starting", 0, "Probing CHMP Rung 1 at $ip")
        var socket: Socket? = null
        try {
            socket = wifiNetworkManager.createBoundSocket()
            socket.tcpNoDelay = true
            socket.soTimeout = 10000
            socket.connect(InetSocketAddress(ip, 80), 5000)

            val out = BufferedOutputStream(socket.getOutputStream())
            val `in` = BufferedInputStream(socket.getInputStream())

            // Step 1 of probe: Pre-session ModeShift
            val postBody = ChmpConstants.XML_MODESHIFT_PRE.toByteArray(StandardCharsets.UTF_8)
            sendHttpRequest(out, "POST", endpoint, ip, postBody)
            val postResp = readHttpResponse(`in`)
            if (postResp.statusCode != 200) {
                return@withContext ProbeResult(
                    success = false,
                    rungName = name,
                    message = "POST ModeShift failed with HTTP ${postResp.statusCode}",
                    details = "Response headers: ${postResp.headers}"
                )
            }

            sendHttpRequest(out, "GET", endpoint, ip, null)
            val getResp = readHttpResponse(`in`)
            val xmlText = String(getResp.body, StandardCharsets.UTF_8)
            ProtocolLogger.log("RX", endpoint, "Probe ModeShift Reply", "", "HTTP ${getResp.statusCode}", getResp.body.size, xmlText)

            if (!xmlText.contains("<ivec:response>OK</ivec:response>")) {
                return@withContext ProbeResult(
                    success = false,
                    rungName = name,
                    message = "ModeShift XML rejected: no <ivec:response>OK</ivec:response>",
                    details = xmlText
                )
            }

            ProbeResult(
                success = true,
                rungName = name,
                message = "CHMP scanner responsive at $endpoint",
                details = "HTTP 200 OK received with valid XML response."
            )
        } catch (e: Exception) {
            Log.e(TAG, "CHMP Probe failed on $ip", e)
            ProtocolLogger.log("SYS", endpoint, "Probe Error", "", "Exception", 0, e.message ?: e.javaClass.simpleName)
            ProbeResult(
                success = false,
                rungName = name,
                message = "Connection failed: ${e.message ?: e.javaClass.simpleName}",
                details = e.stackTraceToString()
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    override suspend fun scanPage(
        ip: String,
        settings: ScanSettings,
        destinationFile: File,
        progressListener: (ScanProgress) -> Unit
    ): ScanPageResult = withContext(Dispatchers.IO) {
        val endpoint = ChmpConstants.SCANNER_ENDPOINT_PORT3
        var socket: Socket? = null
        var fileOutputStream: FileOutputStream? = null

        try {
            progressListener(ScanProgress("Connecting", 0.02f, 0, 0, "Connecting to printer at $ip:80..."))
            socket = wifiNetworkManager.createBoundSocket()
            socket.tcpNoDelay = true
            socket.soTimeout = ChmpConstants.SOCKET_TIMEOUT_MS
            socket.connect(InetSocketAddress(ip, 80), 10000)

            val out = BufferedOutputStream(socket.getOutputStream())
            val `in` = BufferedInputStream(socket.getInputStream())

            // 1. XML VendorCmd ModeShift, jobID = a single space " "
            progressListener(ScanProgress("Pre-Session ModeShift", 0.05f, 0, 0, "Initiating pre-session ModeShift..."))
            executeXmlCommand(out, `in`, endpoint, ip, "ModeShift (pre-session)", ChmpConstants.XML_MODESHIFT_PRE)

            // 2. BIN 0xf320 CapabilityQuery -> 24 bytes
            progressListener(ScanProgress("CapabilityQuery", 0.10f, 0, 0, "Querying scanner capabilities..."))
            val cap1 = executeBinaryCommand(out, `in`, endpoint, ip, "CapabilityQuery (pre)", ChmpConstants.CMD_CAPABILITY_QUERY)
            if (cap1.size < 24) {
                throw ProtocolException("CapabilityQuery expected at least 24 bytes, got ${cap1.size}")
            }

            // 3. XML StartJob, jobID = "00000001", bidi = 1
            progressListener(ScanProgress("StartJob", 0.15f, 0, 0, "Starting scan job 00000001..."))
            executeXmlCommand(out, `in`, endpoint, ip, "StartJob", ChmpConstants.XML_STARTJOB)

            // 4. XML VendorCmd ModeShift, jobID = "00000001"
            progressListener(ScanProgress("In-Session ModeShift", 0.20f, 0, 0, "ModeShift in-session..."))
            executeXmlCommand(out, `in`, endpoint, ip, "ModeShift (in-session)", ChmpConstants.XML_MODESHIFT_IN)

            // 5. BIN 0xdb20 StartSession
            progressListener(ScanProgress("StartSession", 0.25f, 0, 0, "Starting binary session (0xdb20)..."))
            executeBinaryCommand(out, `in`, endpoint, ip, "StartSession", ChmpConstants.CMD_START_SESSION)

            // 6. BIN 0xf320 CapabilityQuery -> 24 bytes
            progressListener(ScanProgress("CapabilityQuery (in-session)", 0.30f, 0, 0, "Querying session capabilities..."))
            val cap2 = executeBinaryCommand(out, `in`, endpoint, ip, "CapabilityQuery (session)", ChmpConstants.CMD_CAPABILITY_QUERY)
            if (cap2.size < 24) {
                throw ProtocolException("In-session CapabilityQuery expected at least 24 bytes, got ${cap2.size}")
            }

            // 7. BIN 0xd820 ScanParam3 (72 bytes)
            progressListener(ScanProgress("ScanParam3", 0.35f, 0, 0, "Configuring scan parameters (${settings.dpi.value} DPI, ${settings.colorMode.label})..."))
            val scanParam3Bytes = buildScanParam3(settings)
            executeBinaryCommand(out, `in`, endpoint, ip, "ScanParam3", scanParam3Bytes)

            // 8. BIN 0xd920 ScanStart3
            progressListener(ScanProgress("ScanStart3", 0.40f, 0, 0, "Triggering scan motor and lamp (0xd920)..."))
            executeBinaryCommand(out, `in`, endpoint, ip, "ScanStart3", ChmpConstants.CMD_SCAN_START3)

            // 9. BIN 0xda20 Status3, poll every 500 ms until byte[8] == 0x03 (120s timeout)
            progressListener(ScanProgress("Status3 Polling", 0.45f, 0, 0, "Warming lamp and positioning sensor..."))
            pollStatus3(out, `in`, endpoint, ip) { statusByte, pollCount, elapsedSec ->
                val stateDesc = when (statusByte) {
                    0x00 -> "idle / initializing"
                    0x01 -> "intermediate preparation"
                    0x02 -> "scanning / lamp warming"
                    0x03 -> "data ready"
                    else -> "state 0x%02x".format(statusByte)
                }
                progressListener(
                    ScanProgress(
                        stepName = "Status3 Polling",
                        percentage = 0.45f + Math.min(0.20f, elapsedSec * 0.01f),
                        statusDetail = "Scanner state: $stateDesc (poll #$pollCount, ${elapsedSec}s)"
                    )
                )
            }

            // 10. BIN 0xdc20 GetDimensions -> actual line count
            progressListener(ScanProgress("GetDimensions", 0.65f, 0, 0, "Retrieving delivered image dimensions..."))
            val dimResp = executeBinaryCommand(out, `in`, endpoint, ip, "GetDimensions", ChmpConstants.CMD_GET_DIMENSIONS)
            if (dimResp.size < 12) {
                throw ProtocolException("GetDimensions reply too short: ${dimResp.size} bytes")
            }
            val actualLines = ((dimResp[10].toInt() and 0xFF) shl 8) or (dimResp[11].toInt() and 0xFF)
            Log.d(TAG, "GetDimensions actual lines from hardware: $actualLines (requested: ${settings.heightPx})")
            ProtocolLogger.log("RX", endpoint, "GetDimensions Parsed", "", "OK", dimResp.size, "Delivered lines: $actualLines, Requested: ${settings.heightPx}")

            // 11. BIN 0xd420 ReadImage, loop until end-of-data flag (bit 0x20 set)
            progressListener(ScanProgress("ReadImage", 0.70f, 0, 0, "Reading image data chunks directly to disk..."))
            destinationFile.parentFile?.mkdirs()
            fileOutputStream = FileOutputStream(destinationFile)

            readImageData(out, `in`, endpoint, ip, fileOutputStream) { bytesReceived, chunkIndex ->
                progressListener(
                    ScanProgress(
                        stepName = "Receiving Image",
                        percentage = 0.70f + Math.min(0.24f, bytesReceived / 5000000f),
                        bytesReceived = bytesReceived,
                        statusDetail = "Chunk #$chunkIndex received ($bytesReceived bytes streamed to disk)"
                    )
                )
            }

            fileOutputStream.flush()
            fileOutputStream.close()
            fileOutputStream = null

            // 12. BIN 0xef20 AbortSession
            progressListener(ScanProgress("AbortSession", 0.95f, 0, 0, "Closing session on hardware (0xef20)..."))
            try {
                executeBinaryCommand(out, `in`, endpoint, ip, "AbortSession", ChmpConstants.CMD_ABORT_SESSION)
            } catch (e: Exception) {
                Log.w(TAG, "AbortSession error (non-fatal)", e)
            }

            // 13. XML EndJob, jobID = "00000001"
            progressListener(ScanProgress("EndJob", 0.98f, 0, 0, "Ending job 00000001..."))
            try {
                executeXmlCommand(out, `in`, endpoint, ip, "EndJob", ChmpConstants.XML_ENDJOB)
            } catch (e: Exception) {
                Log.w(TAG, "EndJob error (non-fatal)", e)
            }

            // Verify file integrity on disk: SOI ff d8 check
            val fileLength = destinationFile.length()
            if (fileLength < 4) {
                throw ProtocolException("Scanned image file is empty or too short ($fileLength bytes)")
            }
            val headerCheck = ByteArray(16)
            destinationFile.inputStream().use { input ->
                input.read(headerCheck)
            }
            if ((headerCheck[0].toInt() and 0xFF) != 0xFF || (headerCheck[1].toInt() and 0xFF) != 0xD8) {
                val hexHeader = ProtocolLogger.formatHex(headerCheck, 16)
                throw ProtocolException("Scanned file failed JPEG SOI validation! Expected ff d8, but got: $hexHeader")
            }

            // Crop overshoot lines if necessary to match requested dimensions
            if (actualLines > settings.heightPx) {
                cropImageToRequestedHeight(destinationFile, settings.widthPx, settings.heightPx)
            }

            progressListener(ScanProgress("Complete", 1.0f, destinationFile.length(), destinationFile.length(), "Page scan complete!"))

            ScanPageResult(
                success = true,
                file = destinationFile,
                widthPx = settings.widthPx,
                heightPx = settings.heightPx,
                deliveredLines = actualLines
            )
        } catch (e: Exception) {
            Log.e(TAG, "Scan page failed on $ip", e)
            ProtocolLogger.log("SYS", endpoint, "Scan Error", "", "FAILED", 0, e.message ?: e.javaClass.simpleName)
            // Attempt hardware cleanup on failure
            cleanupSessionQuietly(socket, endpoint, ip)
            ScanPageResult(
                success = false,
                file = null,
                widthPx = settings.widthPx,
                heightPx = settings.heightPx,
                deliveredLines = 0,
                error = e.message ?: e.javaClass.simpleName
            )
        } finally {
            try {
                fileOutputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun buildScanParam3(settings: ScanSettings): ByteArray {
        return ChmpConstants.buildScanParam3(settings)
    }

    private suspend fun executeXmlCommand(
        out: BufferedOutputStream,
        `in`: BufferedInputStream,
        endpoint: String,
        ip: String,
        cmdName: String,
        xmlBody: String
    ): String {
        val bodyBytes = xmlBody.toByteArray(StandardCharsets.UTF_8)
        ProtocolLogger.log("TX", endpoint, cmdName, ProtocolLogger.formatHex(bodyBytes, 24), "POST", bodyBytes.size, xmlBody)
        sendHttpRequest(out, "POST", endpoint, ip, bodyBytes)
        val postResp = readHttpResponse(`in`)
        if (postResp.statusCode != 200) {
            val err = "$cmdName POST rejected: HTTP ${postResp.statusCode}"
            ProtocolLogger.log("RX", endpoint, cmdName, "", "HTTP ${postResp.statusCode}", 0, err)
            throw ProtocolException(err)
        }

        ProtocolLogger.log("TX", endpoint, cmdName, "", "GET", 0, "Awaiting XML response")
        sendHttpRequest(out, "GET", endpoint, ip, null)
        val getResp = readHttpResponse(`in`)
        val xmlReply = String(getResp.body, StandardCharsets.UTF_8)
        ProtocolLogger.log("RX", endpoint, cmdName, ProtocolLogger.formatHex(getResp.body, 24), "HTTP ${getResp.statusCode}", getResp.body.size, xmlReply)

        if (!xmlReply.contains("<ivec:response>OK</ivec:response>")) {
            val err = "$cmdName XML rejected: expected OK but got:\n$xmlReply"
            throw ProtocolException(err)
        }
        return xmlReply
    }

    private suspend fun executeBinaryCommand(
        out: BufferedOutputStream,
        `in`: BufferedInputStream,
        endpoint: String,
        ip: String,
        cmdName: String,
        commandBytes: ByteArray
    ): ByteArray {
        ProtocolLogger.log("TX", endpoint, cmdName, ProtocolLogger.formatHex(commandBytes, 32), "POST", commandBytes.size)
        sendHttpRequest(out, "POST", endpoint, ip, commandBytes)
        val postResp = readHttpResponse(`in`)
        if (postResp.statusCode != 200) {
            val err = "$cmdName POST rejected: HTTP ${postResp.statusCode}"
            ProtocolLogger.log("RX", endpoint, cmdName, "", "HTTP ${postResp.statusCode}", 0, err)
            throw ProtocolException(err)
        }

        ProtocolLogger.log("TX", endpoint, cmdName, "", "GET", 0, "Awaiting binary response")
        sendHttpRequest(out, "GET", endpoint, ip, null)
        val getResp = readHttpResponse(`in`)
        val respBody = getResp.body

        if (respBody.size < 2) {
            throw ProtocolException("$cmdName binary response too short (${respBody.size} bytes)")
        }

        val status = ((respBody[0].toInt() and 0xFF) shl 8) or (respBody[1].toInt() and 0xFF)
        val statusHex = String.format("0x%04x", status)
        ProtocolLogger.log("RX", endpoint, cmdName, ProtocolLogger.formatHex(respBody, 32), statusHex, respBody.size)

        if (status != ChmpConstants.STATUS_OK) {
            val statusDesc = when (status) {
                ChmpConstants.STATUS_BUSY -> "Busy (0x1414)"
                ChmpConstants.STATUS_FAILED -> "Failed (0x1515)"
                else -> "Error ($statusHex)"
            }
            throw ProtocolException("$cmdName rejected: status $statusDesc")
        }

        return respBody
    }

    private suspend fun pollStatus3(
        out: BufferedOutputStream,
        `in`: BufferedInputStream,
        endpoint: String,
        ip: String,
        onProgress: (statusByte: Int, pollCount: Int, elapsedSec: Long) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        var pollCount = 0

        while (true) {
            pollCount++
            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
            if (elapsedSec > 120) {
                throw ProtocolException("Status3 polling timed out after 120 seconds. Scanner did not become ready.")
            }

            val resp = executeBinaryCommand(out, `in`, endpoint, ip, "Status3", ChmpConstants.CMD_STATUS3)
            if (resp.size < 16) {
                throw ProtocolException("Status3 reply too short: ${resp.size} bytes (expected 16)")
            }

            val stateByte = resp[8].toInt() and 0xFF
            onProgress(stateByte, pollCount, elapsedSec)

            when (stateByte) {
                0x03 -> {
                    // Data is ready!
                    ProtocolLogger.log("RX", endpoint, "Status3", ProtocolLogger.formatHex(resp, 16), "0x03 DATA READY", resp.size, "Scan data is ready to read")
                    return
                }
                0x00, 0x01, 0x02 -> {
                    // 0x00: idle / initializing
                    // 0x01: transient intermediate state — KEEP POLLING, this is not an error
                    // 0x02: scanning, lamp warming up and head moving
                    delay(500)
                }
                else -> {
                    // Any unrecognised value must be treated as in-progress and polled again, never as a failure
                    Log.d(TAG, "Status3 unknown state 0x%02x, continuing poll".format(stateByte))
                    delay(500)
                }
            }
        }
    }

    private suspend fun readImageData(
        out: BufferedOutputStream,
        `in`: BufferedInputStream,
        endpoint: String,
        ip: String,
        fileOut: FileOutputStream,
        onChunkReceived: (totalBytes: Long, chunkIndex: Int) -> Unit
    ) {
        var totalBytesReceived = 0L
        var chunkIndex = 0
        var isFirstNonEmpty = true

        while (true) {
            chunkIndex++
            sendHttpRequest(out, "POST", endpoint, ip, ChmpConstants.CMD_READ_IMAGE)
            val postResp = readHttpResponse(`in`)
            if (postResp.statusCode != 200) {
                throw ProtocolException("ReadImage POST rejected: HTTP ${postResp.statusCode}")
            }

            sendHttpRequest(out, "GET", endpoint, ip, null)
            val getResp = readHttpResponse(`in`)
            val respBody = getResp.body

            if (respBody.size < 16) {
                throw ProtocolException("ReadImage reply header too short: ${respBody.size} bytes")
            }

            val status = ((respBody[0].toInt() and 0xFF) shl 8) or (respBody[1].toInt() and 0xFF)
            if (status != ChmpConstants.STATUS_OK) {
                throw ProtocolException("ReadImage rejected with status 0x%04x".format(status))
            }

            val flags = respBody[8].toInt() and 0xFF
            val dataLen = ((respBody[12].toInt() and 0xFF) shl 24) or
                    ((respBody[13].toInt() and 0xFF) shl 16) or
                    ((respBody[14].toInt() and 0xFF) shl 8) or
                    (respBody[15].toInt() and 0xFF)

            val endOfData = (flags and 0x20) != 0

            ProtocolLogger.log(
                "RX",
                endpoint,
                "ReadImage Chunk #$chunkIndex",
                ProtocolLogger.formatHex(respBody, 20),
                "Flags: 0x%02x, Data: $dataLen bytes".format(flags),
                respBody.size,
                "End-of-data bit (0x20): $endOfData"
            )

            // The first few ReadImage replies commonly return data length 0 while the printer buffers.
            // That is normal — keep looping, do not treat a zero-length block as the end.
            if (dataLen == 0) {
                if (endOfData) {
                    break
                }
                delay(100)
                continue
            }

            if (respBody.size < 16 + dataLen) {
                throw ProtocolException("Truncated ReadImage payload: expected $dataLen bytes, got ${respBody.size - 16}")
            }

            // Verify JPEG SOI marker on first non-empty chunk
            if (isFirstNonEmpty) {
                isFirstNonEmpty = false
                val b0 = respBody[16].toInt() and 0xFF
                val b1 = respBody[17].toInt() and 0xFF
                if (b0 != 0xFF || b1 != 0xD8) {
                    val preview = ProtocolLogger.formatHex(respBody.copyOfRange(16, Math.min(respBody.size, 32)), 16)
                    throw ProtocolException("First ReadImage chunk missing JPEG SOI marker ff d8! Found: $preview")
                }
            }

            // Stream directly to file on disk
            fileOut.write(respBody, 16, dataLen)
            totalBytesReceived += dataLen
            onChunkReceived(totalBytesReceived, chunkIndex)

            // End-of-data test MUST be (flags and 0x20) != 0. Do NOT test for == 0x28.
            if (endOfData) {
                Log.d(TAG, "ReadImage reached end-of-data flag (0x20). Total bytes: $totalBytesReceived")
                break
            }
        }
    }

    private fun cropImageToRequestedHeight(file: File, widthPx: Int, heightPx: Int) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            if (bitmap.height > heightPx) {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, Math.min(widthPx, bitmap.width), heightPx)
                val tempFile = File(file.parentFile, "${file.name}.crop.tmp")
                FileOutputStream(tempFile).use { out ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.renameTo(file)
                }
                cropped.recycle()
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to crop overshoot lines: ${e.message}", e)
        }
    }

    private fun cleanupSessionQuietly(socket: Socket?, endpoint: String, ip: String) {
        if (socket == null || socket.isClosed) return
        try {
            val out = BufferedOutputStream(socket.getOutputStream())
            val `in` = BufferedInputStream(socket.getInputStream())
            sendHttpRequest(out, "POST", endpoint, ip, ChmpConstants.CMD_ABORT_SESSION)
            readHttpResponse(`in`)
            sendHttpRequest(out, "POST", endpoint, ip, ChmpConstants.XML_ENDJOB.toByteArray(StandardCharsets.UTF_8))
            readHttpResponse(`in`)
        } catch (_: Exception) {}
    }

    private fun sendHttpRequest(
        out: BufferedOutputStream,
        method: String,
        endpoint: String,
        host: String,
        body: ByteArray?
    ) {
        val sb = StringBuilder()
        sb.append("$method $endpoint HTTP/1.1\r\n")
        sb.append("Host: $host\r\n")
        if (method == "POST") {
            sb.append("X-CHMP-Version: ${ChmpConstants.CHMP_VERSION_REQUEST}\r\n")
            sb.append("X-CHMP-Timeout: ${ChmpConstants.CHMP_TIMEOUT_HEADER}\r\n")
            sb.append("Content-Type: application/octet-stream\r\n")
            sb.append("Content-Length: ${body?.size ?: 0}\r\n")
            sb.append("Connection: Keep-Alive\r\n")
        } else {
            sb.append("Content-Type: application/octet-stream\r\n")
            sb.append("Connection: Keep-Alive\r\n")
            sb.append("X-CHMP-Version: ${ChmpConstants.CHMP_VERSION_REQUEST}\r\n")
        }
        sb.append("\r\n")

        val headerBytes = sb.toString().toByteArray(StandardCharsets.US_ASCII)
        out.write(headerBytes)
        if (body != null && body.isNotEmpty()) {
            out.write(body)
        }
        out.flush()
    }

    private fun readHttpResponse(`in`: BufferedInputStream): HttpResponse {
        // Read Status-Line
        val statusLine = readLine(`in`)
            ?: throw ProtocolException("Connection closed by peer before receiving HTTP status line")

        val statusParts = statusLine.split(" ", limit = 3)
        if (statusParts.size < 2) {
            throw ProtocolException("Malformed HTTP status line: '$statusLine'")
        }
        val statusCode = statusParts[1].toIntOrNull()
            ?: throw ProtocolException("Invalid HTTP status code: '${statusParts[1]}'")

        // Read Headers
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(`in`) ?: break
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }

        // Read Body
        val isChunked = headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true
        val contentLength = headers["content-length"]?.toIntOrNull()

        val body: ByteArray = when {
            isChunked -> readChunkedBody(`in`)
            contentLength != null && contentLength > 0 -> {
                val buf = ByteArray(contentLength)
                var bytesRead = 0
                while (bytesRead < contentLength) {
                    val read = `in`.read(buf, bytesRead, contentLength - bytesRead)
                    if (read == -1) {
                        throw ProtocolException("Premature EOF reading Content-Length: expected $contentLength, got $bytesRead")
                    }
                    bytesRead += read
                }
                buf
            }
            else -> ByteArray(0)
        }

        return HttpResponse(statusCode, headers, body)
    }

    private fun readChunkedBody(`in`: BufferedInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val line = readLine(`in`) ?: throw ProtocolException("Unexpected EOF while reading chunk size")
            val sizeStr = line.split(";")[0].trim()
            if (sizeStr.isEmpty()) continue
            val chunkSize = sizeStr.toIntOrNull(16)
                ?: throw ProtocolException("Invalid chunk size hex string: '$sizeStr'")

            if (chunkSize == 0) {
                // Read trailing empty line
                readLine(`in`)
                break
            }

            val chunkBuf = ByteArray(chunkSize)
            var bytesRead = 0
            while (bytesRead < chunkSize) {
                val read = `in`.read(chunkBuf, bytesRead, chunkSize - bytesRead)
                if (read == -1) throw ProtocolException("Unexpected EOF inside chunk data")
                bytesRead += read
            }
            out.write(chunkBuf)

            // Read trailing CRLF after chunk data
            val cr = `in`.read()
            val lf = `in`.read()
            if (cr != '\r'.code || lf != '\n'.code) {
                // Some implementations might send only LF
            }
        }
        return out.toByteArray()
    }

    private fun readLine(`in`: BufferedInputStream): String? {
        val baos = ByteArrayOutputStream()
        var c: Int
        while (`in`.read().also { c = it } != -1) {
            if (c == '\r'.code) {
                val next = `in`.read()
                if (next == '\n'.code) {
                    break
                } else if (next != -1) {
                    baos.write(c)
                    baos.write(next)
                }
            } else if (c == '\n'.code) {
                break
            } else {
                baos.write(c)
            }
        }
        if (c == -1 && baos.size() == 0) return null
        return baos.toString(StandardCharsets.US_ASCII.name())
    }

    private data class HttpResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: ByteArray
    )

    companion object {
        private const val TAG = "ChmpScannerTransport"
    }
}

class ProtocolException(message: String) : Exception(message)
