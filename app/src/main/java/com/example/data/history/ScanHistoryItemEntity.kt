package com.example.data.history

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_history_items",
    indices = [
        Index(value = ["scanId"]),
        Index(value = ["imageFilePath"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ScanHistoryEntity::class,
            parentColumns = ["scanId"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScanHistoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanId: String,
    val pageNumber: Int,
    val imageFilePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int,
    val colorMode: String,
    val pageSizeLabel: String,
    val fileSizeBytes: Long
)
