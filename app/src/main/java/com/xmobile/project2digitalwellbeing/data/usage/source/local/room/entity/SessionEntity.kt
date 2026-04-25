package com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["startTimeMillis"]),
        Index(value = ["endTimeMillis"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long
)
