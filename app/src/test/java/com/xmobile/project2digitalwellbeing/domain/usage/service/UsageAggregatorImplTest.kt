package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageAggregatorImplTest {

    private val aggregator = UsageAggregatorImpl()

    @Test
    fun `buildDailyUsage clips sessions to the selected local day`() {
        val sessions = listOf(
            session("app.a", "2026-04-28T00:30:00+07:00", "2026-04-28T01:00:00+07:00"),
            session("app.b", "2026-04-28T23:50:00+07:00", "2026-04-29T00:10:00+07:00")
        )

        val dailyUsage = aggregator.buildDailyUsage(
            sessions = sessions,
            timezoneId = ZONE_ID,
            localDate = "2026-04-28"
        )

        assertEquals(40L * 60L * 1000L, dailyUsage.totalScreenTimeMillis)
        assertEquals(2, dailyUsage.totalSessionCount)
        assertEquals(2, dailyUsage.sessions.size)
        assertEquals(10L * 60L * 1000L, dailyUsage.sessions.last().durationMillis)
    }

    @Test
    fun `buildHourlyUsage splits a session across hour boundaries`() {
        val sessions = listOf(
            session("app.a", "2026-04-28T10:50:00+07:00", "2026-04-28T11:10:00+07:00")
        )

        val hourlyUsage = aggregator.buildHourlyUsage(
            sessions = sessions,
            timezoneId = ZONE_ID,
            localDate = "2026-04-28"
        )

        assertEquals(10L * 60L * 1000L, hourlyUsage.first { it.hourOfDay == 10 }.totalTimeMillis)
        assertEquals(10L * 60L * 1000L, hourlyUsage.first { it.hourOfDay == 11 }.totalTimeMillis)
        assertEquals(0L, hourlyUsage.first { it.hourOfDay == 9 }.totalTimeMillis)
    }

    @Test
    fun `buildAppUsageStats groups by package and uses metadata`() {
        val sessions = listOf(
            session("app.a", "2026-04-28T10:00:00+07:00", "2026-04-28T10:20:00+07:00"),
            session("app.a", "2026-04-28T12:00:00+07:00", "2026-04-28T12:10:00+07:00"),
            session("app.b", "2026-04-28T09:00:00+07:00", "2026-04-28T09:15:00+07:00")
        )

        val appUsageStats = aggregator.buildAppUsageStats(
            sessions = sessions,
            appMetadataByPackage = mapOf(
                "app.a" to metadata("app.a", "App A", AppCategory.SOCIAL),
                "app.b" to metadata("app.b", "App B", AppCategory.PRODUCTIVITY)
            )
        )

        assertEquals("app.a", appUsageStats.first().packageName)
        assertEquals("App A", appUsageStats.first().appName)
        assertEquals(AppCategory.SOCIAL, appUsageStats.first().category)
        assertEquals(30L * 60L * 1000L, appUsageStats.first().totalTimeMillis)
        assertEquals(2, appUsageStats.first().launchCount)
    }

    @Test
    fun `buildCategoryUsage aggregates total time and app count by reporting category`() {
        val sessions = listOf(
            session("app.a", "2026-04-28T10:00:00+07:00", "2026-04-28T10:20:00+07:00"),
            session("app.b", "2026-04-28T11:00:00+07:00", "2026-04-28T11:15:00+07:00"),
            session("app.c", "2026-04-28T12:00:00+07:00", "2026-04-28T12:30:00+07:00")
        )

        val categoryUsage = aggregator.buildCategoryUsage(
            sessions = sessions,
            appMetadataByPackage = mapOf(
                "app.a" to metadata("app.a", "App A", AppCategory.SOCIAL),
                "app.b" to metadata("app.b", "App B", AppCategory.SOCIAL),
                "app.c" to metadata("app.c", "App C", AppCategory.PRODUCTIVITY)
            )
        )

        val socialUsage = categoryUsage.first { it.category == AppCategory.SOCIAL }
        assertEquals(35L * 60L * 1000L, socialUsage.totalTimeMillis)
        assertEquals(2, socialUsage.sessionCount)
        assertEquals(2, socialUsage.appCount)
        assertEquals(listOf("app.a", "app.b"), socialUsage.packageNames)
    }

    @Test
    fun `buildWeeklyUsage returns total average and most used day`() {
        val sessions = listOf(
            session("app.a", "2026-04-28T10:00:00+07:00", "2026-04-28T10:30:00+07:00"),
            session("app.b", "2026-04-29T10:00:00+07:00", "2026-04-29T11:00:00+07:00")
        )

        val weeklyUsage = aggregator.buildWeeklyUsage(
            sessions = sessions,
            timezoneId = ZONE_ID,
            startLocalDate = "2026-04-28",
            endLocalDate = "2026-04-30"
        )

        assertEquals(90L * 60L * 1000L, weeklyUsage.totalScreenTimeMillis)
        assertEquals(30L * 60L * 1000L, weeklyUsage.averageDailyScreenTimeMillis)
        assertEquals("2026-04-29", weeklyUsage.mostUsedLocalDate)
        assertEquals(3, weeklyUsage.dailyUsages.size)
        assertEquals(0L, weeklyUsage.dailyUsages.last().totalScreenTimeMillis)
    }

    @Test
    fun `buildWeeklyUsage returns null most used date when week is empty`() {
        val weeklyUsage = aggregator.buildWeeklyUsage(
            sessions = emptyList(),
            timezoneId = ZONE_ID,
            startLocalDate = "2026-04-28",
            endLocalDate = "2026-04-30"
        )

        assertNull(weeklyUsage.mostUsedLocalDate)
    }

    private fun metadata(packageName: String, appName: String, reportingCategory: AppCategory): AppMetadata {
        return AppMetadata(
            packageName = packageName,
            appName = appName,
            sourceCategory = SourceAppCategory.UNKNOWN,
            reportingCategory = reportingCategory,
            classificationSource = ClassificationSource.UNKNOWN,
            confidence = 0f
        )
    }

    private fun session(packageName: String, start: String, end: String): AppSession {
        val startMillis = java.time.OffsetDateTime.parse(start).toInstant().toEpochMilli()
        val endMillis = java.time.OffsetDateTime.parse(end).toInstant().toEpochMilli()
        return AppSession(
            packageName = packageName,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            durationMillis = endMillis - startMillis
        )
    }

    private companion object {
        private const val ZONE_ID = "Asia/Bangkok"
    }
}
