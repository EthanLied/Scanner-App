package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.history.AppDatabase
import com.example.data.history.HistoryRepository
import com.example.data.model.ScanColorMode
import com.example.data.model.ScanDpi
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings
import com.example.data.model.ScannedPage
import com.example.protocol.chmp.ChmpConstants
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: HistoryRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HistoryRepository(context, database.scanHistoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("PIXMA G3010 Scanner", appName)
    }

    @Test
    fun `test scan param checksum calculation`() {
        val settings = ScanSettings(
            dpi = ScanDpi.DPI_300,
            colorMode = ScanColorMode.COLOR,
            pageSize = ScanPageSize.A4
        )
        val params = ChmpConstants.buildScanParam3Payload(settings)
        assertEquals(56, params.size)

        var sum = 0
        for (i in 0 until params.size - 1) {
            sum += (params[i].toInt() and 0xFF)
        }
        val expectedChecksum = ((0 - sum) and 0xFF).toByte()
        assertEquals(expectedChecksum, params.last())
        assertEquals(0x82.toByte(), params[0x21]) // JPEG
    }

    @Test
    fun `test export deduplication prevents duplicate scan IDs`() = runBlocking {
        val now = System.currentTimeMillis()
        val page1 = ScannedPage(
            id = "p1",
            pageNumber = 1,
            filePath = "/fake/page1.jpg",
            widthPx = 2480,
            heightPx = 3508,
            actualDeliveredLines = 3508,
            timestamp = now,
            dpi = 300,
            colorMode = "Color",
            pageSizeLabel = "A4",
            fileSizeBytes = 10240
        )
        val page2 = ScannedPage(
            id = "p2",
            pageNumber = 2,
            filePath = "/fake/page2.jpg",
            widthPx = 2480,
            heightPx = 3508,
            actualDeliveredLines = 3508,
            timestamp = now + 1000L,
            dpi = 300,
            colorMode = "Color",
            pageSizeLabel = "A4",
            fileSizeBytes = 20480
        )

        // First export
        val scanId1 = repository.recordExport(listOf(page1, page2))
        assertTrue(scanId1.startsWith("SCAN-"))

        // Second export with same pages -> must return identical scan ID
        val scanId2 = repository.recordExport(listOf(page1, page2))
        assertEquals("Exporting identical pages must not create a duplicate scan ID", scanId1, scanId2)

        // Different collection -> new scan ID
        val scanId3 = repository.recordExport(listOf(page1))
        assertNotEquals("Exporting different pages must create a new scan ID", scanId1, scanId3)

        // Reference count verification
        val refsPage1 = database.scanHistoryDao().countReferencesToImage("/fake/page1.jpg")
        assertEquals(2, refsPage1) // referenced in both scanId1 and scanId3

        val refsPage2 = database.scanHistoryDao().countReferencesToImage("/fake/page2.jpg")
        assertEquals(1, refsPage2) // only referenced in scanId1
    }

    @Test
    fun `test purge expiry setting persists`() {
        repository.setPurgeExpiryDays(30)
        assertEquals(30, repository.purgeExpiryDays.value)

        repository.setPurgeExpiryDays(14)
        assertEquals(14, repository.purgeExpiryDays.value)
    }
}
