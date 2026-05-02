package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import javax.inject.Inject

class UsageRefreshPolicyImpl @Inject constructor() : UsageRefreshPolicy {

    override fun resolveWindow(
        params: RefreshUsageDataParams,
        syncState: UsageSyncState
    ): UsageRefreshWindow {
        val endTimeMillis = params.nowMillis
        val fullRefreshStartMillis = (params.nowMillis - FULL_REFRESH_WINDOW_MILLIS).coerceAtLeast(0L)

        if (params.forceFullRefresh || !syncState.isInitialSyncCompleted) {
            return UsageRefreshWindow(
                startTimeMillis = fullRefreshStartMillis,
                endTimeMillis = endTimeMillis,
                refreshMode = RefreshMode.FULL
            )
        }

        val incrementalStartMillis = (syncState.lastProcessedTimestampMillis ?: params.nowMillis) - SAFETY_WINDOW_MILLIS
        return UsageRefreshWindow(
            startTimeMillis = incrementalStartMillis
                .coerceAtLeast(fullRefreshStartMillis)
                .coerceAtLeast(0L),
            endTimeMillis = endTimeMillis,
            refreshMode = RefreshMode.INCREMENTAL
        )
    }

    private companion object {
        private const val FULL_REFRESH_WINDOW_MILLIS = 24L * 60L * 60L * 1000L
        private const val SAFETY_WINDOW_MILLIS = 5L * 60L * 1000L
    }
}
