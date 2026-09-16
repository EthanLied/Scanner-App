package com.example.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.model.PrinterDevice
import com.example.network.WifiNetworkManager
import com.example.protocol.ProtocolLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.nio.charset.StandardCharsets

class PrinterDiscoveryManager(
    private val context: Context,
    private val wifiNetworkManager: WifiNetworkManager,
    private val scope: CoroutineScope
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val discoveredPrinters: StateFlow<List<PrinterDevice>> = _discoveredPrinters.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var ippListener: NsdManager.DiscoveryListener? = null
    private var chmpListener: NsdManager.DiscoveryListener? = null

    private val discoveredMap = mutableMapOf<String, PrinterDevice>()

    fun startDiscovery() {
        if (_isDiscovering.value) return
        _isDiscovering.value = true

        wifiNetworkManager.acquireMulticastLock()
        ProtocolLogger.log("SYS", "mDNS", "Discovery", "", "START", 0, "Starting mDNS discovery for _ipp._tcp and _canon-chmp._tcp")

        startIppDiscovery()
        startChmpDiscovery()
    }

    fun stopDiscovery() {
        if (!_isDiscovering.value) return
        _isDiscovering.value = false

        try {
            ippListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping IPP discovery", e)
        }
        ippListener = null

        try {
            chmpListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping CHMP discovery", e)
        }
        chmpListener = null

        wifiNetworkManager.releaseMulticastLock()
        ProtocolLogger.log("SYS", "mDNS", "Discovery", "", "STOP", 0, "Stopped mDNS discovery")
    }

    private fun startIppDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "IPP discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "IPP discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "IPP discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "IPP discovery stopped for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d(TAG, "IPP Service found: ${serviceInfo.serviceName}")
                resolveService(serviceInfo, isIpp = true)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "IPP Service lost: ${serviceInfo?.serviceName}")
            }
        }
        ippListener = listener
        try {
            nsdManager.discoverServices("_ipp._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call discoverServices for _ipp._tcp.", e)
        }
    }

    private fun startChmpDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d(TAG, "CHMP Service found: ${serviceInfo.serviceName}")
                resolveService(serviceInfo, isIpp = false)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
        }
        chmpListener = listener
        try {
            nsdManager.discoverServices("_canon-chmp._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call discoverServices for _canon-chmp._tcp.", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo, isIpp: Boolean) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo?.serviceName}: error $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                if (resolvedInfo == null) return
                val host = resolvedInfo.host
                val ip = host?.hostAddress ?: return

                // Extract TXT records
                val attributes = resolvedInfo.attributes
                val ty = attributes["ty"]?.let { String(it, StandardCharsets.UTF_8) } ?: ""
                val usbMdl = attributes["usb_MDL"]?.let { String(it, StandardCharsets.UTF_8) } ?: ""
                val scanCap = attributes["Scan"]?.let { String(it, StandardCharsets.UTF_8) } ?: ""

                val serviceName = resolvedInfo.serviceName ?: ""
                Log.d(TAG, "Resolved service: $serviceName at $ip: ty='$ty', usb_MDL='$usbMdl', Scan='$scanCap'")

                val isCanonG3010 = ty.contains("G3010", ignoreCase = true) ||
                        usbMdl.contains("G3010", ignoreCase = true) ||
                        serviceName.contains("G3010", ignoreCase = true) ||
                        serviceName.contains("Canon", ignoreCase = true)

                val modelName = when {
                    ty.isNotEmpty() -> ty
                    usbMdl.isNotEmpty() -> "Canon $usbMdl"
                    serviceName.isNotEmpty() -> serviceName
                    else -> "Canon PIXMA G3010"
                }

                val method = if (isIpp) "mDNS _ipp._tcp" else "mDNS _canon-chmp._tcp"
                val device = PrinterDevice(
                    model = modelName,
                    ip = ip,
                    port = 80,
                    discoveryMethod = method,
                    isOnline = true,
                    details = "ScanCap: $scanCap | MDL: $usbMdl"
                )

                scope.launch(Dispatchers.Main) {
                    discoveredMap[ip] = device
                    _discoveredPrinters.value = discoveredMap.values.toList()
                    ProtocolLogger.log(
                        "RX", "mDNS", "Discovered", "", "OK", 0,
                        "Found $modelName at $ip via $method"
                    )
                }
            }
        })
    }

    fun addManualPrinter(ip: String, model: String = "Canon PIXMA G3010"): PrinterDevice {
        val device = PrinterDevice(
            model = model,
            ip = ip.trim(),
            port = 80,
            discoveryMethod = "Manual IP",
            isOnline = true
        )
        discoveredMap[device.ip] = device
        _discoveredPrinters.value = discoveredMap.values.toList()
        ProtocolLogger.log("SYS", "Manual", "Add Printer", "", "OK", 0, "Added manual printer at ${device.ip}")
        return device
    }

    companion object {
        private const val TAG = "PrinterDiscovery"
    }
}
