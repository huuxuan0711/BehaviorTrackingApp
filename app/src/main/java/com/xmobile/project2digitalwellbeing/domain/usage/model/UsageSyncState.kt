package com.xmobile.project2digitalwellbeing.domain.usage.model

data class UsageSyncState(
    val lastProcessedTimestampMillis: Long?,
    val lastSeenEventTimestampMillis: Long?,
    val lastSuccessfulRefreshTimestampMillis: Long?,
    val isInitialSyncCompleted: Boolean
)
