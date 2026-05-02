package com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastProcessedTimestampMillis: Long?,
    val lastSeenEventTimestampMillis: Long?,
    val lastSuccessfulRefreshTimestampMillis: Long?,
    val isInitialSyncCompleted: Boolean
)
