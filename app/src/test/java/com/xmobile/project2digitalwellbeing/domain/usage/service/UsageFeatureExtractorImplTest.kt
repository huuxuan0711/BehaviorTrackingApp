package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageFeatureExtractorImplTest {

    private val extractor = UsageFeatureExtractorImpl()

    @Test
    fun `extractFeatures computes core usage and late-night metrics`() {
        val features = extractor.extractFeatures(
            sessions = listOf(
                enrichedSession(
                    packageName = "app.a",
                    appName = "App A",
                    category = AppCategory.SOCIAL,
                    startMillis = 1_000L,
                    endMillis = 61_000L,
                    hourOfDay = 23,
                    isLateNight = true
                ),
                enrichedSession(
                    packageName = "app.b",
                    appName = "App B",
                    category = AppCategory.PRODUCTIVITY,
                    startMillis = 70_000L,
                    endMillis = 190_000L,
                    hourOfDay = 10,
                    isLateNight = false
                )
            ),
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        assertEquals(180_000L, features.totalScreenTimeMillis)
        assertEquals(2, features.totalSessionCount)
        assertEquals(120_000L, features.longestSessionMillis)
        assertEquals(90_000L, features.averageSessionLengthMillis)
        assertEquals(60_000L, features.lateNightUsageMillis)
        assertEquals(1, features.lateNightSessionCount)
        assertEquals(60_000L, features.lateNightAverageSessionLengthMillis)
        assertEquals(10, features.peakUsageHour)
    }

    @Test
    fun `extractFeatures computes switching and interval metrics`() {
        val features = extractor.extractFeatures(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 21, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 90_000L, 150_000L, 21, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 180_000L, 240_000L, 21, false),
                enrichedSession("app.c", "App C", AppCategory.GAME, 300_000L, 360_000L, 22, true)
            ),
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        assertEquals(2, features.switchCount)
        assertEquals(45_000L, features.averageSwitchIntervalMillis)
        assertEquals(4, features.topAppsByLaunchCount.sumOf { it.launchCount })
    }

    @Test
    fun `extractFeatures builds session length distribution buckets`() {
        val features = extractor.extractFeatures(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 20, false),
                enrichedSession("app.b", "App B", AppCategory.SOCIAL, 0L, 300_000L, 20, false),
                enrichedSession("app.c", "App C", AppCategory.SOCIAL, 0L, 900_000L, 20, false)
            ),
            preferences = UsageAnalysisPreferences.DEFAULT.copy(
                longSessionThresholdMillis = 10L * 60L * 1000L
            )
        )

        assertEquals(1, features.sessionLengthDistribution.shortSessionCount)
        assertEquals(1, features.sessionLengthDistribution.mediumSessionCount)
        assertEquals(1, features.sessionLengthDistribution.longSessionCount)
    }

    @Test
    fun `extractFeatures builds top apps and categories`() {
        val features = extractor.extractFeatures(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 300_000L, 20, false),
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 400_000L, 700_000L, 21, true),
                enrichedSession("app.b", "App B", AppCategory.GAME, 800_000L, 900_000L, 22, true)
            ),
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        assertEquals("app.a", features.topAppsByDuration.first().packageName)
        assertEquals(2, features.topAppsByDuration.first().launchCount)
        assertEquals(AppCategory.SOCIAL, features.topCategoriesByDuration.first().category)
        assertEquals("app.a", features.lateNightTopApps.first().packageName)
    }

    @Test
    fun `extractFeatures returns zeroed values for empty input`() {
        val features = extractor.extractFeatures(
            sessions = emptyList(),
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        assertEquals(0L, features.totalScreenTimeMillis)
        assertEquals(0, features.totalSessionCount)
        assertEquals(0, features.switchCount)
        assertNull(features.peakUsageHour)
        assertEquals(0, features.topAppsByDuration.size)
    }

    private fun enrichedSession(
        packageName: String,
        appName: String,
        category: AppCategory,
        startMillis: Long,
        endMillis: Long,
        hourOfDay: Int,
        isLateNight: Boolean
    ): EnrichedSession {
        return EnrichedSession(
            session = AppSession(
                packageName = packageName,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                durationMillis = endMillis - startMillis
            ),
            appName = appName,
            category = category,
            hourOfDay = hourOfDay,
            isLateNight = isLateNight
        )
    }
}
