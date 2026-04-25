package com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_usage_events",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["timestampMillis"]),
        Index(value = ["eventType"]),
        Index(value = ["packageName", "timestampMillis", "eventType"], unique = true)
    ]
)
data class RawUsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val timestampMillis: Long,
    val eventType: String
)
