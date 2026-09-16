package com.example

import android.app.Application
import com.example.data.history.AppDatabase
import com.example.data.history.HistoryRepository
import com.example.data.session.SessionManager
import com.example.discovery.PrinterDiscoveryManager
import com.example.network.WifiNetworkManager
import com.example.protocol.fallback.FallbackLadder
import com.example.util.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    lateinit var database: AppDatabase
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Install persistent crash logging immediately
        CrashLogger.install(this)

        wifiNetworkManager = WifiNetworkManager(this)
        sessionManager = SessionManager(this)
        discoveryManager = PrinterDiscoveryManager(this, wifiNetworkManager, applicationScope)
        fallbackLadder = FallbackLadder(wifiNetworkManager)

        database = AppDatabase.getInstance(this)
        historyRepository = HistoryRepository(this, database.scanHistoryDao())

        // Purge expired history on app startup without needing background jobs
        applicationScope.launch(Dispatchers.IO) {
            try {
                historyRepository.purgeExpiredOnStartup(sessionManager.getActivePagePaths())
            } catch (e: Exception) {
                CrashLogger.logNonFatal("StartupPurge", "Failed checking/purging expired scan history", e)
            }
        }
    }

    companion object {
        lateinit var instance: PixmaScannerApp
            private set
    }
}

