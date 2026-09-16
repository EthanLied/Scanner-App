package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ScanForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        val step = intent?.getStringExtra(EXTRA_STEP) ?: "Scanning in progress..."
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val detail = intent?.getStringExtra(EXTRA_DETAIL) ?: "Connecting to Canon G3010..."

        val notification = buildNotification(step, progress, detail)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PixmaScanner:ScanWakeLock").apply {
                acquire(180000) // 3 minutes max
            }
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PixmaScanner:ScanWifiLock").apply {
                acquire()
            }
            Log.d(TAG, "Acquired WakeLock and WifiLock for scan session")
        } catch (e: Exception) {
            Log.w(TAG, "Failed acquiring locks", e)
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            Log.d(TAG, "Released WakeLock and WifiLock")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing locks", e)
        }
    }

    private fun buildNotification(step: String, progress: Int, detail: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Canon PIXMA G3010: $step")
            .setContentText(detail)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scan Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live scan status for Canon PIXMA G3010 over Wi-Fi"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ScanForegroundService"
        const val CHANNEL_ID = "pixma_scan_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_SCAN"
        const val ACTION_UPDATE = "com.example.action.UPDATE_SCAN"
        const val ACTION_STOP = "com.example.action.STOP_SCAN"

        const val EXTRA_STEP = "extra_step"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_DETAIL = "extra_detail"

        fun start(context: Context, step: String, progress: Int, detail: String) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STEP, step)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_DETAIL, detail)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun update(context: Context, step: String, progress: Int, detail: String) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STEP, step)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_DETAIL, detail)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
