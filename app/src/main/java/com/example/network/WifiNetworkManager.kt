package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Socket
import javax.net.SocketFactory

class WifiNetworkManager(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiNetwork: Network? = null

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _wifiSsid = MutableStateFlow<String?>(null)
    val wifiSsid: StateFlow<String?> = _wifiSsid.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiNetwork = network
                _isWifiConnected.value = true
                updateSsid()
                Log.d(TAG, "Wi-Fi network acquired: $network")
            }
        }

        override fun onLost(network: Network) {
            if (wifiNetwork == network) {
                wifiNetwork = null
                _isWifiConnected.value = false
                _wifiSsid.value = null
                Log.d(TAG, "Wi-Fi network lost: $network")
            }
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiNetwork = network
                _isWifiConnected.value = true
                updateSsid()
            }
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)

            // Initial check
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiNetwork = activeNetwork
                    _isWifiConnected.value = true
                    updateSsid()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun updateSsid() {
        try {
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "")
            if (!ssid.isNullOrEmpty() && ssid != "<unknown ssid>") {
                _wifiSsid.value = ssid
            } else {
                _wifiSsid.value = "Wi-Fi Connected"
            }
        } catch (e: Exception) {
            _wifiSsid.value = "Wi-Fi Connected"
        }
    }

    /**
     * Creates a socket bound specifically to the active Wi-Fi Network.
     * Prevents cellular routing when Wi-Fi has no public internet access.
     */
    fun createBoundSocket(): Socket {
        val currentWifiNetwork = wifiNetwork
        if (currentWifiNetwork != null) {
            Log.d(TAG, "Creating socket bound to Wi-Fi network: $currentWifiNetwork")
            return currentWifiNetwork.socketFactory.createSocket()
        }
        // Fallback if network callback hasn't fired yet
        for (network in connectivityManager.allNetworks) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiNetwork = network
                _isWifiConnected.value = true
                Log.d(TAG, "Found Wi-Fi network in allNetworks: $network")
                return network.socketFactory.createSocket()
            }
        }
        Log.w(TAG, "No active Wi-Fi network found, using default SocketFactory")
        return SocketFactory.getDefault().createSocket()
    }

    fun getWifiNetwork(): Network? = wifiNetwork

    @Synchronized
    fun acquireMulticastLock(): WifiManager.MulticastLock {
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("PixmaScannerMulticastLock").apply {
                setReferenceCounted(true)
            }
        }
        multicastLock?.let {
            if (!it.isHeld) {
                it.acquire()
                Log.d(TAG, "MulticastLock acquired for mDNS")
            }
        }
        return multicastLock!!
    }

    @Synchronized
    fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "MulticastLock released")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MulticastLock", e)
        }
    }

    companion object {
        private const val TAG = "WifiNetworkManager"
    }
}
