package com.example.protocol.fallback

import com.example.network.WifiNetworkManager
import com.example.protocol.ProbeResult
import com.example.protocol.ScannerTransport
import com.example.protocol.chmp.ChmpScannerTransport

class FallbackLadder(
    private val wifiNetworkManager: WifiNetworkManager
) {
    val rung1 = ChmpScannerTransport(wifiNetworkManager)
    val rung2 = Rung2ChmpPort1Probe(wifiNetworkManager)
    val rung3 = Rung3EsclProbe(wifiNetworkManager)
    val rung4 = Rung4BjnpProbe()
    val rung5 = Rung5UsbOtgStub()

    val allTransports: List<ScannerTransport> = listOf(rung1, rung2, rung3, rung4, rung5)

    suspend fun runFullDiagnosticLadder(ip: String): List<ProbeResult> {
        val results = mutableListOf<ProbeResult>()
        for (transport in allTransports) {
            val result = transport.probe(ip)
            results.add(result)
        }
        return results
    }

    suspend fun findBestWorkingTransport(ip: String): Pair<ScannerTransport, ProbeResult>? {
        // Try Rung 1 first
        val rung1Result = rung1.probe(ip)
        if (rung1Result.success) {
            return Pair(rung1, rung1Result)
        }

        // Try Rung 2
        val rung2Result = rung2.probe(ip)
        if (rung2Result.success) {
            // Port 1 alive means CHMP is running; probe port3 again or return rung1
            return Pair(rung1, rung2Result)
        }

        // Try Rung 3 (eSCL)
        val rung3Result = rung3.probe(ip)
        if (rung3Result.success) {
            return Pair(rung3, rung3Result)
        }

        return null
    }
}
