package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageRefreshPolicyImplTest {

    private val policy = UsageRefreshPolicyImpl()

    @Test
    fun `full refresh starts from beginning of local day 13 days back`() {
        val zoneId = ZoneId.of("Asia/Bangkok")
        val nowMillis = LocalDateTime.of(2026, 5, 13, 15, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val expectedStartMillis = LocalDateTime.of(2026, 4, 30, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val window = policy.resolveWindow(
            params = RefreshUsageDataParams(
                nowMillis = nowMillis,
                timezoneId = zoneId.id
            ),
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            )
        )

        assertEquals(RefreshMode.FULL, window.refreshMode)
        assertEquals(expectedStartMillis, window.startTimeMillis)
        assertEquals(nowMillis, window.endTimeMillis)
    }

    @Test
    fun `requested range bypasses cooldown and uses explicit window`() {
        val window = policy.resolveWindow(
            params = RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Asia/Bangkok",
                requestedRangeStartMillis = 1_000L,
                requestedRangeEndMillis = 5_000L
            ),
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = 9_000L,
                lastSeenEventTimestampMillis = 9_000L,
                lastSuccessfulRefreshTimestampMillis = 9_500L,
                isInitialSyncCompleted = true
            )
        )

        assertEquals(RefreshMode.INCREMENTAL, window.refreshMode)
        assertEquals(1_000L, window.startTimeMillis)
        assertEquals(5_000L, window.endTimeMillis)
    }
}
