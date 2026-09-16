package com.example

import android.app.Application
import com.example.data.session.SessionManager
import com.example.discovery.PrinterDiscoveryManager
import com.example.network.WifiNetworkManager
import com.example.protocol.fallback.FallbackLadder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PixmaScannerApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var wifiNetworkManager: WifiNetworkManager
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var discoveryManager: PrinterDiscoveryManager
        private set

    lateinit var fallbackLadder: FallbackLadder
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        wifiNetworkManager = WifiNetworkManager(this)
        sessionManager = SessionManager(this)
        discoveryManager = PrinterDiscoveryManager(this, wifiNetworkManager, applicationScope)
        fallbackLadder = FallbackLadder(wifiNetworkManager)
    }

    companion object {
        lateinit var instance: PixmaScannerApp
            private set
    }
}
