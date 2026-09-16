package com.example.data.history

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_history",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["updatedAt"])
    ]
)
data class ScanHistoryEntity(
    @PrimaryKey
    val scanId: String,
    val fingerprint: String,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int,
    val latestImageFilePath: String,
    val totalSizeBytes: Long
)
