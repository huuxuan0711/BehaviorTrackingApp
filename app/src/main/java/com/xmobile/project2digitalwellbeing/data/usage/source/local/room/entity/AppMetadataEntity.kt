package com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_metadata",
    indices = [
        Index(value = ["reportingCategory"]),
        Index(value = ["classificationSource"])
    ]
)
data class AppMetadataEntity(
    @PrimaryKey val packageName: String,
    val appName: String?,
    val sourceCategory: String,
    val reportingCategory: String,
    val classificationSource: String,
    val confidence: Float,
    val updatedAtMillis: Long
)
