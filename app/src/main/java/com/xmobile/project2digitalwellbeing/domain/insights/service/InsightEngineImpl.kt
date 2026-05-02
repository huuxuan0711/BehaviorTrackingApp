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
        val ratioThreshold = ratioThreshold(
            base = LATE_NIGHT_RATIO_THRESHOLD,
            sensitivity = preferences.insightSensitivity
        )
        val usageThresholdMillis = durationThreshold(
            base = MIN_LATE_NIGHT_USAGE_MILLIS,
            sensitivity = preferences.insightSensitivity
        )

        if (features.lateNightUsageRatio < ratioThreshold ||
            features.lateNightUsageMillis < usageThresholdMillis
        ) {
            return null
        }

        val severity = normalize(
            value = features.lateNightUsageRatio,
            min = ratioThreshold,
            max = 0.7f
        )
        val durationBoost = normalize(
            value = features.lateNightUsageMillis.toFloat(),
            min = usageThresholdMillis.toFloat(),
            max = 3f * MILLIS_PER_HOUR
        )
        val score = weightedScore(
            primary = severity,
            secondary = durationBoost
        )
        val confidence = (0.65f + severity * 0.25f + durationBoost * 0.1f).coerceAtMost(0.95f)

        return Insight(
            type = InsightType.LATE_NIGHT_USAGE,
            score = score,
            confidence = confidence,
            evidence = mapOf(
                "lateNightUsageRatio" to formatRatio(features.lateNightUsageRatio),
                "lateNightUsageMillis" to features.lateNightUsageMillis.toString(),
                "lateNightSessionCount" to features.lateNightSessionCount.toString(),
                "lateNightAverageSessionLengthMillis" to features.lateNightAverageSessionLengthMillis.toString()
            ),
            relatedPackages = features.lateNightTopApps.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildFrequentSwitchingInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val minimumSwitchCount = countThreshold(
            base = MIN_SWITCH_COUNT,
            sensitivity = preferences.insightSensitivity
        )
        val switchesPerHourThreshold = ratioThreshold(
            base = SWITCHES_PER_HOUR_THRESHOLD,
            sensitivity = preferences.insightSensitivity
        )

        if (features.switchCount < minimumSwitchCount || features.switchesPerHour < switchesPerHourThreshold) {
            return null
        }

        val switchDensity = normalize(
            value = features.switchesPerHour,
            min = switchesPerHourThreshold,
            max = 30f
        )
        val switchCountSeverity = normalize(
            value = features.switchCount.toFloat(),
            min = minimumSwitchCount.toFloat(),
            max = 150f
        )
        val score = weightedScore(
            primary = switchDensity,
            secondary = switchCountSeverity
        )
        val confidence = (0.6f + switchDensity * 0.2f + switchCountSeverity * 0.2f).coerceAtMost(0.93f)

        return Insight(
            type = InsightType.FREQUENT_SWITCHING,
            score = score,
            confidence = confidence,
            evidence = mapOf(
                "switchCount" to features.switchCount.toString(),
                "switchesPerHour" to formatRatio(features.switchesPerHour),
                "averageSwitchIntervalMillis" to features.averageSwitchIntervalMillis.toString()
            ),
            relatedPackages = features.topAppsByLaunchCount.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildBingeUsageInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val minimumBingeSessionMillis = durationThreshold(
            base = preferences.longSessionThresholdMillis,
            sensitivity = preferences.insightSensitivity
        )
        val minimumAverageBingeSessionMillis = durationThreshold(
            base = maxOf(MIN_AVERAGE_BINGE_SESSION_MILLIS, preferences.longSessionThresholdMillis / 2L),
            sensitivity = preferences.insightSensitivity
        )

        if (features.longestSessionMillis < minimumBingeSessionMillis &&
            features.averageSessionLengthMillis < minimumAverageBingeSessionMillis
        ) {
            return null
        }

        val longestSessionSeverity = normalize(
            value = features.longestSessionMillis.toFloat(),
            min = minimumBingeSessionMillis.toFloat(),
            max = 3f * MILLIS_PER_HOUR
        )
        val averageSessionSeverity = normalize(
            value = features.averageSessionLengthMillis.toFloat(),
            min = minimumAverageBingeSessionMillis.toFloat(),
            max = 45f * MILLIS_PER_MINUTE
        )
        val score = weightedScore(
            primary = longestSessionSeverity,
            secondary = averageSessionSeverity
        )
        val confidence = (0.58f + longestSessionSeverity * 0.22f + averageSessionSeverity * 0.2f).coerceAtMost(0.9f)

        return Insight(
            type = InsightType.BINGE_USAGE,
            score = score,
            confidence = confidence,
            evidence = mapOf(
                "longestSessionMillis" to features.longestSessionMillis.toString(),
                "averageSessionLengthMillis" to features.averageSessionLengthMillis.toString(),
                "totalScreenTimeMillis" to features.totalScreenTimeMillis.toString()
            ),
            relatedPackages = features.topAppsByDuration.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun buildLateNightSwitchingInsight(
        features: UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): Insight? {
        val ratioThreshold = ratioThreshold(
            base = LATE_NIGHT_SWITCHING_RATIO_THRESHOLD,
            sensitivity = preferences.insightSensitivity
        )
        val switchingThreshold = ratioThreshold(
            base = LATE_NIGHT_SWITCHING_SWITCH_THRESHOLD,
            sensitivity = preferences.insightSensitivity
        )

        if (features.lateNightUsageRatio < ratioThreshold ||
            features.switchesPerHour < switchingThreshold
        ) {
            return null
        }

        val lateNightSeverity = normalize(
            value = features.lateNightUsageRatio,
            min = ratioThreshold,
            max = 0.7f
        )
        val switchingSeverity = normalize(
            value = features.switchesPerHour,
            min = switchingThreshold,
            max = 30f
        )
        val score = weightedScore(
            primary = lateNightSeverity,
            secondary = switchingSeverity
        )
        val confidence = (0.68f + lateNightSeverity * 0.16f + switchingSeverity * 0.16f).coerceAtMost(0.96f)

        return Insight(
            type = InsightType.LATE_NIGHT_SWITCHING,
            score = score,
            confidence = confidence,
            evidence = mapOf(
                "lateNightUsageRatio" to formatRatio(features.lateNightUsageRatio),
                "switchesPerHour" to formatRatio(features.switchesPerHour),
                "lateNightUsageMillis" to features.lateNightUsageMillis.toString()
            ),
            relatedPackages = features.lateNightTopApps.take(TOP_ITEMS_LIMIT).map { it.packageName }
        )
    }

    private fun weightedScore(primary: Float, secondary: Float): Int {
        return (((primary * 0.7f) + (secondary * 0.3f)) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max <= min) {
            return 0f
        }
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun ratioThreshold(base: Float, sensitivity: InsightSensitivity): Float {
        return when (sensitivity) {
            InsightSensitivity.LOW -> base * 1.15f
            InsightSensitivity.MEDIUM -> base
            InsightSensitivity.HIGH -> base * 0.85f
        }
    }

    private fun countThreshold(base: Int, sensitivity: InsightSensitivity): Int {
        return when (sensitivity) {
            InsightSensitivity.LOW -> (base * 1.2f).roundToInt()
            InsightSensitivity.MEDIUM -> base
            InsightSensitivity.HIGH -> (base * 0.8f).roundToInt().coerceAtLeast(1)
        }
    }

    private fun durationThreshold(base: Long, sensitivity: InsightSensitivity): Long {
        return when (sensitivity) {
            InsightSensitivity.LOW -> (base * 1.15f).roundToInt().toLong()
            InsightSensitivity.MEDIUM -> base
            InsightSensitivity.HIGH -> (base * 0.85f).roundToInt().toLong().coerceAtLeast(MIN_DURATION_THRESHOLD_MILLIS)
        }
    }

    private fun formatRatio(value: Float): String = String.format("%.2f", value)

    private companion object {
        private const val TOP_ITEMS_LIMIT = 3
        private const val LATE_NIGHT_RATIO_THRESHOLD = 0.25f
        private const val LATE_NIGHT_SWITCHING_RATIO_THRESHOLD = 0.20f
        private const val SWITCHES_PER_HOUR_THRESHOLD = 10f
        private const val LATE_NIGHT_SWITCHING_SWITCH_THRESHOLD = 12f
        private const val MIN_SWITCH_COUNT = 15
        private const val MIN_LATE_NIGHT_USAGE_MILLIS = 30L * 60L * 1000L
        private const val MIN_AVERAGE_BINGE_SESSION_MILLIS = 8L * 60L * 1000L
        private const val MIN_DURATION_THRESHOLD_MILLIS = 5L * 60L * 1000L
        private const val MILLIS_PER_MINUTE = 60L * 1000L
        private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
    }
}
