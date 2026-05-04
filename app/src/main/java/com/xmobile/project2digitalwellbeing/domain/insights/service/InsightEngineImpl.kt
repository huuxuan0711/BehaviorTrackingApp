package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightSensitivity
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import javax.inject.Inject
import kotlin.math.roundToInt

class InsightEngineImpl @Inject constructor() : InsightEngine {

    override fun generateInsights(
        features: UsageFeatures,
        dailyUsage: DailyUsage,
        preferences: UsageAnalysisPreferences
    ): List<Insight> {
        val insights = buildList {
            buildLateNightUsageInsight(features, preferences)?.let(::add)
            buildFrequentSwitchingInsight(features, preferences)?.let(::add)
            buildBingeUsageInsight(features, preferences)?.let(::add)
            buildLateNightSwitchingInsight(features, preferences)?.let(::add)
            buildWorkHourDistractionInsight(features, preferences)?.let(::add)
            buildMorningRoutineInsight(features, preferences)?.let(::add)
            buildConstantCheckingInsight(features, preferences)?.let(::add)
            buildAppRelianceInsight(features, preferences)?.let(::add)
        }

        return insights.sortedWith(
            compareByDescending<Insight> { it.score }
                .thenByDescending { it.confidence }
        )
    }

    private fun buildLateNightUsageInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val ratioThreshold = ratioThreshold(LATE_NIGHT_RATIO_THRESHOLD, preferences.insightSensitivity)
        val usageThresholdMillis = durationThreshold(MIN_LATE_NIGHT_USAGE_MILLIS, preferences.insightSensitivity)

        if (features.lateNightUsageRatio < ratioThreshold || features.lateNightUsageMillis < usageThresholdMillis) {
            return null
        }

        val severity = normalize(features.lateNightUsageRatio, ratioThreshold, 0.7f)
        val durationBoost = normalize(features.lateNightUsageMillis.toFloat(), usageThresholdMillis.toFloat(), 3f * MILLIS_PER_HOUR)
        
        return Insight(
            type = InsightType.LATE_NIGHT_USAGE,
            score = weightedScore(severity, durationBoost),
            confidence = (0.65f + severity * 0.25f + durationBoost * 0.1f).coerceAtMost(0.95f),
            evidence = mapOf(
                "lateNightUsageRatio" to formatRatio(features.lateNightUsageRatio),
                "lateNightUsageMillis" to features.lateNightUsageMillis.toString()
            ),
            relatedPackages = features.lateNightTopApps.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildFrequentSwitchingInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val switchesPerHourThreshold = ratioThreshold(SWITCHES_PER_HOUR_THRESHOLD, preferences.insightSensitivity)
        if (features.switchesPerHour < switchesPerHourThreshold) return null

        val switchDensity = normalize(features.switchesPerHour, switchesPerHourThreshold, 30f)
        return Insight(
            type = InsightType.FREQUENT_SWITCHING,
            score = (switchDensity * 100).toInt().coerceIn(0, 100),
            confidence = (0.6f + switchDensity * 0.3f).coerceAtMost(0.93f),
            evidence = mapOf("switchesPerHour" to formatRatio(features.switchesPerHour)),
            relatedPackages = features.topAppsByLaunchCount.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildBingeUsageInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val threshold = durationThreshold(preferences.longSessionThresholdMillis, preferences.insightSensitivity)
        if (features.longestSessionMillis < threshold) return null

        val severity = normalize(features.longestSessionMillis.toFloat(), threshold.toFloat(), 3f * MILLIS_PER_HOUR)
        return Insight(
            type = InsightType.BINGE_USAGE,
            score = (severity * 100).toInt().coerceIn(0, 100),
            confidence = (0.58f + severity * 0.32f).coerceAtMost(0.9f),
            evidence = mapOf("longestSessionMillis" to features.longestSessionMillis.toString()),
            relatedPackages = features.topAppsByDuration.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildLateNightSwitchingInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val ratioThreshold = LATE_NIGHT_SWITCHING_RATIO_THRESHOLD
        val switchingThreshold = LATE_NIGHT_SWITCHING_SWITCH_THRESHOLD
        if (features.lateNightUsageRatio < ratioThreshold || features.switchesPerHour < switchingThreshold) return null

        val score = weightedScore(
            normalize(features.lateNightUsageRatio, ratioThreshold, 0.6f),
            normalize(features.switchesPerHour, switchingThreshold, 25f)
        )
        return Insight(
            type = InsightType.LATE_NIGHT_SWITCHING,
            score = score,
            confidence = 0.85f,
            evidence = mapOf("switchesPerHour" to formatRatio(features.switchesPerHour)),
            relatedPackages = features.lateNightTopApps.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildWorkHourDistractionInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val distractionThreshold = durationThreshold(WORK_HOUR_DISTRACTION_THRESHOLD, preferences.insightSensitivity)
        if (features.workHourDistractionMillis < distractionThreshold) return null

        val severity = normalize(features.workHourDistractionMillis.toFloat(), distractionThreshold.toFloat(), 2.5f * MILLIS_PER_HOUR)
        return Insight(
            type = InsightType.WORK_HOUR_DISTRACTION,
            score = (severity * 100).toInt().coerceIn(0, 100),
            confidence = (0.7f + severity * 0.2f).coerceAtMost(0.92f),
            evidence = mapOf("workHourDistractionMillis" to features.workHourDistractionMillis.toString()),
            relatedPackages = features.workHourTopApps
                .filter { it.category in DISTRACTING_CATEGORIES }
                .take(TOP_ITEMS_LIMIT)
                .map { it.packageName }
        )
    }

    private fun buildMorningRoutineInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val morningThreshold = durationThreshold(MORNING_USAGE_THRESHOLD, preferences.insightSensitivity)
        if (features.morningUsageMillis < morningThreshold) return null

        val severity = normalize(features.morningUsageMillis.toFloat(), morningThreshold.toFloat(), MILLIS_PER_HOUR.toFloat())
        return Insight(
            type = InsightType.MORNING_ROUTINE,
            score = (severity * 100).toInt().coerceIn(0, 100),
            confidence = (0.65f + severity * 0.25f).coerceAtMost(0.94f),
            evidence = mapOf("morningUsageMillis" to features.morningUsageMillis.toString()),
            relatedPackages = features.morningTopApps.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildConstantCheckingInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val shortRatio = features.sessionLengthDistribution.shortSessionRatio
        if (shortRatio < CONSTANT_CHECKING_RATIO_THRESHOLD || features.totalSessionCount < MIN_SESSION_COUNT_FOR_CHECKING) return null

        val severity = normalize(shortRatio, CONSTANT_CHECKING_RATIO_THRESHOLD, 0.95f)
        return Insight(
            type = InsightType.CONSTANT_CHECKING,
            score = (severity * 100).toInt().coerceIn(0, 100),
            confidence = (0.75f + severity * 0.15f).coerceAtMost(0.96f),
            evidence = mapOf("shortSessionRatio" to formatRatio(shortRatio)),
            relatedPackages = features.topAppsByLaunchCount.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildAppRelianceInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val topApp = features.topAppsByDuration.firstOrNull() ?: return null
        val totalTime = features.totalScreenTimeMillis
        if (totalTime < MIN_SCREEN_TIME_FOR_RELIANCE) return null

        val ratio = topApp.totalTimeMillis.toFloat() / totalTime.toFloat()
        if (ratio < APP_RELIANCE_RATIO_THRESHOLD) return null

        val severity = normalize(ratio, APP_RELIANCE_RATIO_THRESHOLD, 0.9f)
        return Insight(
            type = InsightType.APP_RELIANCE,
            score = (severity * 100).toInt().coerceIn(0, 100),
            confidence = (0.8f + severity * 0.15f).coerceAtMost(0.98f),
            evidence = mapOf(
                "appRelianceRatio" to formatRatio(ratio),
                "dominantApp" to topApp.packageName
            ),
            relatedPackages = listOf(topApp.packageName)
        )
    }

    private fun weightedScore(primary: Float, secondary: Float): Int {
        return (((primary * 0.7f) + (secondary * 0.3f)) * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun ratioThreshold(base: Float, sensitivity: InsightSensitivity): Float {
        return when (sensitivity) {
            InsightSensitivity.LOW -> base * 1.15f
            InsightSensitivity.MEDIUM -> base
            InsightSensitivity.HIGH -> base * 0.85f
        }
    }

    private fun durationThreshold(base: Long, sensitivity: InsightSensitivity): Long {
        return when (sensitivity) {
            InsightSensitivity.LOW -> (base * 1.15f).toLong()
            InsightSensitivity.MEDIUM -> base
            InsightSensitivity.HIGH -> (base * 0.85f).toLong()
        }
    }

    private fun formatRatio(value: Float): String = String.format("%.2f", value)

    private companion object {
        private const val TOP_ITEMS_LIMIT = 3
        private const val MILLIS_PER_MINUTE = 60L * 1000L
        private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
        
        private const val LATE_NIGHT_RATIO_THRESHOLD = 0.25f
        private const val MIN_LATE_NIGHT_USAGE_MILLIS = 30L * MILLIS_PER_MINUTE
        private const val SWITCHES_PER_HOUR_THRESHOLD = 10f
        private const val LATE_NIGHT_SWITCHING_RATIO_THRESHOLD = 0.20f
        private const val LATE_NIGHT_SWITCHING_SWITCH_THRESHOLD = 12f
        
        private const val WORK_HOUR_DISTRACTION_THRESHOLD = 45L * MILLIS_PER_MINUTE
        private const val MORNING_USAGE_THRESHOLD = 20L * MILLIS_PER_MINUTE
        private const val CONSTANT_CHECKING_RATIO_THRESHOLD = 0.6f
        private const val MIN_SESSION_COUNT_FOR_CHECKING = 15
        private const val APP_RELIANCE_RATIO_THRESHOLD = 0.5f
        private const val MIN_SCREEN_TIME_FOR_RELIANCE = 30L * MILLIS_PER_MINUTE

        private val DISTRACTING_CATEGORIES = setOf(
            com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory.SOCIAL,
            com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory.VIDEO,
            com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory.GAME
        )
    }
}
