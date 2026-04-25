package com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insights",
    indices = [
        Index(value = ["type"]),
        Index(value = ["windowStartMillis"]),
        Index(value = ["windowEndMillis"])
    ]
)
data class InsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val score: Int,
    val confidence: Float,
    val evidenceJson: String,
    val relatedPackagesCsv: String,
    val windowStartMillis: Long,
    val windowEndMillis: Long
)
