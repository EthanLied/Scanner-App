package com.example

import com.example.data.export.ExportProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportProgressTest {

    @Test
    fun testProgressFraction_computesAccuratePercentage() {
        val progress1 = ExportProgress(currentPage = 50, totalPages = 100, statusText = "Exporting page 50 of 100...")
        assertEquals(0.5f, progress1.progressFraction, 0.001f)
        assertEquals(50, progress1.percent)

        val progress2 = ExportProgress(currentPage = 42, totalPages = 150, statusText = "Exporting page 42 of 150...")
        assertEquals(0.28f, progress2.progressFraction, 0.001f)
        assertEquals(28, progress2.percent)

        val progressZero = ExportProgress(currentPage = 0, totalPages = 0, statusText = "Starting...")
        assertEquals(0f, progressZero.progressFraction, 0.001f)
        assertEquals(0, progressZero.percent)

        val progressDone = ExportProgress(currentPage = 250, totalPages = 250, statusText = "Complete")
        assertEquals(1.0f, progressDone.progressFraction, 0.001f)
        assertEquals(100, progressDone.percent)
    }
}
