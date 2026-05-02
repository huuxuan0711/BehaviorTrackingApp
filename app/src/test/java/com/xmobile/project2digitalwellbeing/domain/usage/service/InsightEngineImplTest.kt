package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution
import com.xmobile.project2digitalwellbeing.domain.usage.model.TopCategoryFeature
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineImplTest {

    private val insightEngine = InsightEngineImpl()
    private val dailyUsage = DailyUsage(
        localDate = "2026-04-28",
        timezoneId = "Asia/Bangkok",
        totalScreenTimeMillis = 0L,
        totalSessionCount = 0,
        sessions = emptyList<AppSession>()
    )

    @Test
    fun `generates late night usage insight when ratio and duration are high`() {
        val insights = insightEngine.generateInsights(
            features = baseFeatures(
                lateNightUsageMillis = 90L * 60L * 1000L,
                lateNightSessionCount = 4,
                lateNightUsageRatio = 0.45f,
                lateNightAverageSessionLengthMillis = 22L * 60L * 1000L,
                lateNightTopApps = listOf(topApp("app.social"))
            ),
            dailyUsage = dailyUsage,
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        val insight = insights.first { it.type == InsightType.LATE_NIGHT_USAGE }
        assertTrue(insight.score > 0)
        assertTrue(insight.confidence >= 0.65f)
        assertEquals(listOf("app.social"), insight.relatedPackages)
    }

    @Test
    fun `generates frequent switching insight when switching is dense`() {
        val insights = insightEngine.generateInsights(
            features = baseFeatures(
                switchCount = 42,
                switchesPerHour = 18f,
                averageSwitchIntervalMillis = 45_000L,
                topAppsByLaunchCount = listOf(topApp("app.a"), topApp("app.b"))
            ),
            dailyUsage = dailyUsage,
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        val insight = insights.first { it.type == InsightType.FREQUENT_SWITCHING }
        assertEquals("42", insight.evidence["switchCount"])
        assertEquals(listOf("app.a", "app.b"), insight.relatedPackages)
    }

    @Test
    fun `generates binge insight from long sessions`() {
        val insights = insightEngine.generateInsights(
            features = baseFeatures(
                longestSessionMillis = 50L * 60L * 1000L,
                averageSessionLengthMillis = 12L * 60L * 1000L,
                topAppsByDuration = listOf(topApp("app.video"))
            ),
            dailyUsage = dailyUsage,
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        val insight = insights.first { it.type == InsightType.BINGE_USAGE }
        assertTrue(insight.score > 0)
        assertEquals(listOf("app.video"), insight.relatedPackages)
    }

    @Test
    fun `generates combined late night switching insight when both signals are elevated`() {
        val insights = insightEngine.generateInsights(
            features = baseFeatures(
                lateNightUsageMillis = 60L * 60L * 1000L,
                lateNightUsageRatio = 0.35f,
                switchesPerHour = 16f,
                lateNightTopApps = listOf(topApp("app.social"), topApp("app.video"))
            ),
            dailyUsage = dailyUsage,
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        val insight = insights.first { it.type == InsightType.LATE_NIGHT_SWITCHING }
        assertTrue(insight.score > 0)
        assertEquals(listOf("app.social", "app.video"), insight.relatedPackages)
    }

    @Test
    fun `returns no insights when signals stay below thresholds`() {
        val insights = insightEngine.generateInsights(
            features = baseFeatures(),
            dailyUsage = dailyUsage,
            preferences = UsageAnalysisPreferences.DEFAULT
        )

        assertTrue(insights.isEmpty())
    }

    private fun baseFeatures(
        totalScreenTimeMillis: Long = 3L * 60L * 60L * 1000L,
        totalSessionCount: Int = 12,
        longestSessionMillis: Long = 10L * 60L * 1000L,
        lateNightUsageMillis: Long = 0L,
        lateNightSessionCount: Int = 0,
        lateNightUsageRatio: Float = 0f,
        lateNightAverageSessionLengthMillis: Long = 0L,
        switchCount: Int = 0,
        switchesPerHour: Float = 0f,
        averageSessionLengthMillis: Long = 5L * 60L * 1000L,
        averageSwitchIntervalMillis: Long = 0L,
        peakUsageHour: Int? = null,
        topAppsByDuration: List<UsageFeatureTopApp> = emptyList(),
        topAppsByLaunchCount: List<UsageFeatureTopApp> = emptyList(),
        topCategoriesByDuration: List<TopCategoryFeature> = emptyList(),
        lateNightTopApps: List<UsageFeatureTopApp> = emptyList()
    ): UsageFeatures {
        return UsageFeatures(
            totalScreenTimeMillis = totalScreenTimeMillis,
            totalSessionCount = totalSessionCount,
            longestSessionMillis = longestSessionMillis,
            lateNightUsageMillis = lateNightUsageMillis,
            lateNightSessionCount = lateNightSessionCount,
            lateNightUsageRatio = lateNightUsageRatio,
            lateNightAverageSessionLengthMillis = lateNightAverageSessionLengthMillis,
            switchCount = switchCount,
            switchesPerHour = switchesPerHour,
            averageSessionLengthMillis = averageSessionLengthMillis,
            averageSwitchIntervalMillis = averageSwitchIntervalMillis,
            peakUsageHour = peakUsageHour,
            sessionLengthDistribution = SessionLengthDistribution(
                shortSessionCount = 0,
                mediumSessionCount = 0,
                longSessionCount = 0,
                shortSessionRatio = 0f,
                mediumSessionRatio = 0f,
                longSessionRatio = 0f
            ),
            topAppsByDuration = topAppsByDuration,
            topAppsByLaunchCount = topAppsByLaunchCount,
            topCategoriesByDuration = topCategoriesByDuration,
            lateNightTopApps = lateNightTopApps
        )
    }

    private fun topApp(packageName: String): UsageFeatureTopApp {
        return UsageFeatureTopApp(
            packageName = packageName,
            appName = packageName,
            category = AppCategory.SOCIAL,
            totalTimeMillis = 0L,
            launchCount = 1
        )
    }
}
