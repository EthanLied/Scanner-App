package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Information about an available app release from GitHub.
 */
data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val htmlUrl: String
) {
    val formattedApkSize: String
        get() {
            if (apkSizeBytes <= 0) return "Unknown size"
            val mb = apkSizeBytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }

    val formattedDate: String
        get() {
            return try {
                if (publishedAt.isBlank()) return ""
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                val date = isoFormat.parse(publishedAt) ?: return publishedAt
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            } catch (_: Exception) {
                publishedAt
            }
        }
}

/**
 * State representing the in-app update status.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class Downloading(
        val info: UpdateInfo,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class Downloaded(val info: UpdateInfo, val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * Helper singleton that checks for new releases on GitHub, downloads the APK,
 * handles Unknown Sources permissions, and triggers Android's PackageInstaller.
 */
object AppUpdater {
    private const val TAG = "AppUpdater"

    // Default repository configuration
    const val GITHUB_OWNER = "EthanLied"
    const val GITHUB_REPO = "Scanner-App"
    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Checks if a candidate version string (e.g., "v1.0.1" or "1.2.0")
     * is newer than the current version string (e.g., "1.0").
     */
    fun isNewerVersion(current: String, candidate: String): Boolean {
        val currentParts = extractVersionParts(current)
        val candidateParts = extractVersionParts(candidate)
        val maxLen = maxOf(currentParts.size, candidateParts.size)

        for (i in 0 until maxLen) {
            val curr = currentParts.getOrElse(i) { 0 }
            val cand = candidateParts.getOrElse(i) { 0 }
            if (cand > curr) return true
            if (cand < curr) return false
        }
        return false
    }

    private fun extractVersionParts(version: String): List<Int> {
        // Strip leading non-digits (e.g. 'v', 'V', 'release-')
        val clean = version.trim().replace(Regex("^[a-zA-Z_\\-]+"), "")
        return clean.split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }

    /**
     * Checks GitHub for the latest release asynchronously.
     * Returns UpdateInfo if a newer release exists, or null if up to date or an error occurs.
     */
    suspend fun checkForUpdate(
        owner: String = GITHUB_OWNER,
        repo: String = GITHUB_REPO,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "PixmaScanner-App")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "GitHub API responded with HTTP ${response.code}"
                Log.w(TAG, errorMsg)
                _updateState.value = UpdateState.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            val bodyString = response.body?.string()
            if (bodyString.isNullOrBlank()) {
                val errorMsg = "Empty response from GitHub API"
                _updateState.value = UpdateState.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            val json = JSONObject(bodyString)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "No changelog provided.")
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases")

            // Locate .apk asset in "assets" array
            var apkDownloadUrl: String? = null
            var apkFileName: String? = null
            var apkSizeBytes: Long = 0L

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val contentType = asset.optString("content_type", "")
                    if (name.endsWith(".apk", ignoreCase = true) ||
                        contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
                    ) {
                        apkFileName = name
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val updateAvailable = isNewerVersion(current = currentVersion, candidate = tagName)

            if (updateAvailable) {
                val info = UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName,
                    apkSizeBytes = apkSizeBytes,
                    htmlUrl = htmlUrl
                )
                _updateState.value = UpdateState.UpdateAvailable(info)
                Result.success(info)
            } else {
                _updateState.value = UpdateState.UpToDate(currentVersion)
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed checking for update", e)
            val msg = e.localizedMessage ?: "Network error while checking updates"
            _updateState.value = UpdateState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK file to the app's external files directory with real-time progress callbacks.
     */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: ((percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = info.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            val err = "No APK download URL available for release ${info.latestVersion}"
            _updateState.value = UpdateState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            _updateState.value = UpdateState.Downloading(
                info = info,
                progressPercent = 0,
                bytesDownloaded = 0,
                totalBytes = info.apkSizeBytes
            )

            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.cacheDir, "updates").apply { mkdirs() }
            val cleanVersion = info.latestVersion.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFileName = info.apkFileName ?: "scanner_app_$cleanVersion.apk"
            val targetFile = File(destDir, destFileName)

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "PixmaScanner-App")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Failed downloading APK: HTTP ${response.code}"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val body = response.body ?: throw IllegalStateException("Empty HTTP response body")
            val contentLength = if (body.contentLength() > 0) body.contentLength() else info.apkSizeBytes
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalDownloaded = 0L
            var lastPercent = -1

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val percent = if (contentLength > 0) {
                            ((totalDownloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        if (percent != lastPercent) {
                            lastPercent = percent
                            _updateState.value = UpdateState.Downloading(
                                info = info,
                                progressPercent = percent,
                                bytesDownloaded = totalDownloaded,
                                totalBytes = contentLength
                            )
                            onProgress?.invoke(percent, totalDownloaded, contentLength)
                        }
                    }
                    output.flush()
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                val err = "Downloaded APK file is empty or missing"
                _updateState.value = UpdateState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            _updateState.value = UpdateState.Downloaded(info, targetFile)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            val msg = e.localizedMessage ?: "Download failed"
            _updateState.value = UpdateState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Enqueues download using Android's native DownloadManager.
     * Shows system download notification and saves to external downloads directory.
     */
    fun downloadViaDownloadManager(context: Context, info: UpdateInfo): Long? {
        val downloadUrl = info.apkDownloadUrl ?: return null
        return try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading ${info.releaseName}")
                setDescription("Scanner App update ${info.latestVersion}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/vnd.android.package-archive")
                val fileName = info.apkFileName ?: "scanner_app_${info.latestVersion}.apk"
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed", e)
            null
        }
    }

    /**
     * Checks if the app has permission to install packages on Android 8.0+ (API 26+).
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system Settings screen to allow installing unknown apps.
     */
    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open unknown sources settings", e)
            }
        }
    }

    /**
     * Triggers Android's Package Installer to install the downloaded APK.
     * Checks for unknown sources permission on Android 8.0+.
     * Returns true if installer intent was launched, false if permission was needed or an error occurred.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "Cannot install non-existent or empty APK file: ${apkFile.absolutePath}")
            return false
        }

        // Android 8.0+ Unknown sources check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Log.i(TAG, "Unknown app sources permission not granted; prompting user")
                openUnknownSourcesSettings(context)
                return false
            }
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed launching package installer", e)
            false
        }
    }
}
