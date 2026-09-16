package com.example.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY updatedAt DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE scanId = :scanId LIMIT 1")
    suspend fun getHistoryById(scanId: String): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getHistoryByFingerprint(fingerprint: String): ScanHistoryEntity?

    @Query("SELECT * FROM scan_history_items WHERE scanId = :scanId ORDER BY pageNumber ASC")
    suspend fun getItemsForScan(scanId: String): List<ScanHistoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ScanHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ScanHistoryItemEntity>)

    @Query("UPDATE scan_history SET updatedAt = :timestamp WHERE scanId = :scanId")
    suspend fun updateTimestamp(scanId: String, timestamp: Long)

    @Query("DELETE FROM scan_history WHERE scanId = :scanId")
    suspend fun deleteHistory(scanId: String)

    @Query("DELETE FROM scan_history_items WHERE scanId = :scanId")
    suspend fun deleteItemsByScanId(scanId: String)

    @Query("SELECT * FROM scan_history WHERE updatedAt < :cutoffTimestamp")
    suspend fun getExpiredHistory(cutoffTimestamp: Long): List<ScanHistoryEntity>

    @Query("SELECT DISTINCT imageFilePath FROM scan_history_items")
    suspend fun getAllReferencedImagePaths(): List<String>

    @Query("SELECT COUNT(*) FROM scan_history_items WHERE imageFilePath = :imagePath")
    suspend fun countReferencesToImage(imagePath: String): Int

    @Transaction
    suspend fun insertScanWithItems(history: ScanHistoryEntity, items: List<ScanHistoryItemEntity>) {
        insertHistory(history)
        deleteItemsByScanId(history.scanId)
        insertItems(items)
    }

    @Transaction
    suspend fun deleteScanWithItems(scanId: String) {
        deleteItemsByScanId(scanId)
        deleteHistory(scanId)
    }
}
