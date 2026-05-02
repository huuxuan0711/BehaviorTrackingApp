package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrendDirection
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTrendAnalyzerImplTest {

    private val analyzer = UsageTrendAnalyzerImpl()

    @Test
    fun `compareDaily returns up trend when current usage exceeds previous`() {
        val trend = analyzer.compareDaily(
            current = dailyUsage("2026-04-28", 180L * 60_000L),
            previous = dailyUsage("2026-04-27", 120L * 60_000L)
        )

        assertEquals(UsageTrendDirection.UP, trend.direction)
        assertEquals(60L * 60_000L, trend.deltaMillis)
        assertEquals(0.5f, trend.deltaRatio)
    }

    @Test
    fun `compareWeekly returns flat trend when delta is below threshold`() {
        val trend = analyzer.compareWeekly(
            current = weeklyUsage(600L * 60_000L),
            previous = weeklyUsage(600L * 60_000L + 30_000L)
        )

        assertEquals(UsageTrendDirection.FLAT, trend.direction)
    }

    @Test
    fun `compareDaily handles missing previous period`() {
        val trend = analyzer.compareDaily(
            current = dailyUsage("2026-04-28", 45L * 60_000L),
            previous = null
        )

        assertEquals(UsageTrendDirection.UP, trend.direction)
        assertEquals(1f, trend.deltaRatio)
    }

    private fun dailyUsage(localDate: String, totalScreenTimeMillis: Long): DailyUsage {
        return DailyUsage(
            localDate = localDate,
            timezoneId = "Asia/Bangkok",
            totalScreenTimeMillis = totalScreenTimeMillis,
            totalSessionCount = 0,
            sessions = emptyList()
        )
    }

    private fun weeklyUsage(totalScreenTimeMillis: Long): WeeklyUsage {
        return WeeklyUsage(
            startLocalDate = "2026-04-21",
            endLocalDate = "2026-04-27",
            timezoneId = "Asia/Bangkok",
            totalScreenTimeMillis = totalScreenTimeMillis,
            averageDailyScreenTimeMillis = totalScreenTimeMillis / 7L,
            mostUsedLocalDate = null,
            dailyUsages = emptyList()
        )
    }
}
