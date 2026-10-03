package com.example

import com.example.util.AppUpdater
import com.example.util.UpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {

    @Test
    fun testIsNewerVersion_recognizesNewerReleases() {
        assertTrue(AppUpdater.isNewerVersion(current = "1.0", candidate = "v1.0.1"))
        assertTrue(AppUpdater.isNewerVersion(current = "1.0", candidate = "1.1"))
        assertTrue(AppUpdater.isNewerVersion(current = "1.0.0", candidate = "v1.0.1"))
        assertTrue(AppUpdater.isNewerVersion(current = "1.9.9", candidate = "v2.0"))
        assertTrue(AppUpdater.isNewerVersion(current = "1.0.5", candidate = "v1.0.6-alpha"))
    }

    @Test
    fun testIsNewerVersion_rejectsEqualOrOlderReleases() {
        assertFalse(AppUpdater.isNewerVersion(current = "1.0", candidate = "1.0"))
        assertFalse(AppUpdater.isNewerVersion(current = "1.0", candidate = "v1.0"))
        assertFalse(AppUpdater.isNewerVersion(current = "1.0.0", candidate = "v1.0"))
        assertFalse(AppUpdater.isNewerVersion(current = "1.5", candidate = "1.4.9"))
        assertFalse(AppUpdater.isNewerVersion(current = "2.0", candidate = "v1.9.9"))
    }

    @Test
    fun testUpdateInfo_sizeFormatting() {
        val info = UpdateInfo(
            currentVersion = "1.0",
            latestVersion = "v1.1",
            releaseName = "v1.1",
            releaseNotes = "Improvements",
            publishedAt = "2026-10-03T12:00:00Z",
            apkDownloadUrl = "https://example.com/app.apk",
            apkFileName = "app-release.apk",
            apkSizeBytes = 15 * 1024 * 1024 + 512 * 1024, // ~15.5 MB
            htmlUrl = "https://github.com/EthanLied/Scanner-App"
        )

        assertEquals("15.5 MB", info.formattedApkSize)
    }
}
