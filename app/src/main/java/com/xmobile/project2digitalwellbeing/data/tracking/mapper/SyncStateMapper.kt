package com.xmobile.project2digitalwellbeing.data.tracking.mapper

import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.entity.SyncStateEntity
import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState

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
