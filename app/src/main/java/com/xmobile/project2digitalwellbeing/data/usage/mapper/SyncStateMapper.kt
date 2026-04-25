package com.xmobile.project2digitalwellbeing.data.usage.mapper

import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity.SyncStateEntity
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState

fun SyncStateEntity.toDomain(): UsageSyncState {
    return UsageSyncState(
        lastProcessedTimestampMillis = lastProcessedTimestampMillis,
        lastSeenEventTimestampMillis = lastSeenEventTimestampMillis,
        lastSuccessfulRefreshTimestampMillis = lastSuccessfulRefreshTimestampMillis,
        isInitialSyncCompleted = isInitialSyncCompleted
    )
}

fun UsageSyncState.toEntity(): SyncStateEntity {
    return SyncStateEntity(
        lastProcessedTimestampMillis = lastProcessedTimestampMillis,
        lastSeenEventTimestampMillis = lastSeenEventTimestampMillis,
        lastSuccessfulRefreshTimestampMillis = lastSuccessfulRefreshTimestampMillis,
        isInitialSyncCompleted = isInitialSyncCompleted
    )
}
