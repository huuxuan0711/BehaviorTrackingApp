package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class UsageRefreshPolicyImpl @Inject constructor() : UsageRefreshPolicy {

    override fun resolveWindow(
        params: RefreshUsageDataParams,
        syncState: UsageSyncState
    ): UsageRefreshWindow {
        val requestedStart = params.requestedRangeStartMillis
        val requestedEnd = params.requestedRangeEndMillis
        if (requestedStart != null && requestedEnd != null) {
            return UsageRefreshWindow(
                startTimeMillis = requestedStart.coerceAtLeast(0L),
                endTimeMillis = requestedEnd.coerceAtLeast(requestedStart).coerceAtLeast(0L),
                refreshMode = RefreshMode.INCREMENTAL
            )
        }

        val lastRefresh = syncState.lastSuccessfulRefreshTimestampMillis ?: 0L
        val isCoolingDown = !params.forceFullRefresh &&
                syncState.isInitialSyncCompleted &&
                (params.nowMillis - lastRefresh < COOLDOWN_MILLIS)

        if (isCoolingDown) {
            return UsageRefreshWindow(
                startTimeMillis = lastRefresh,
                endTimeMillis = params.nowMillis,
                refreshMode = RefreshMode.SKIPPED
            )
        }

        val endTimeMillis = params.nowMillis
        val fullRefreshStartMillis = resolveFullRefreshStartMillis(params)

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

    private fun resolveFullRefreshStartMillis(params: RefreshUsageDataParams): Long {
        val zoneId = runCatching { ZoneId.of(params.timezoneId) }.getOrNull()
            ?: return (params.nowMillis - FULL_REFRESH_WINDOW_MILLIS).coerceAtLeast(0L)

        val anchorDate = Instant.ofEpochMilli(params.nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .minusDays(FULL_REFRESH_DAYS - 1L)

        return anchorDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
            .coerceAtLeast(0L)
    }

    private companion object {
        private const val FULL_REFRESH_DAYS = 14L
        private const val FULL_REFRESH_WINDOW_MILLIS = FULL_REFRESH_DAYS * 24L * 60L * 60L * 1000L
        private const val SAFETY_WINDOW_MILLIS = 5L * 60L * 1000L
        private const val COOLDOWN_MILLIS = 60L * 1000L
    }
}
