package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightSensitivity

data class UsageAnalysisPreferences(
    val lateNightStartHour: Int,
    val longSessionThresholdMillis: Long,
    val trackAllCategories: Boolean,
    val insightSensitivity: InsightSensitivity
) {
    companion object {
        const val DEFAULT_LATE_NIGHT_START_HOUR = 22
        const val DEFAULT_LATE_NIGHT_END_HOUR = 5
        const val DEFAULT_LONG_SESSION_THRESHOLD_MINUTES = 20
        const val MIN_LONG_SESSION_THRESHOLD_MINUTES = 10

        val DEFAULT = UsageAnalysisPreferences(
            lateNightStartHour = DEFAULT_LATE_NIGHT_START_HOUR,
            longSessionThresholdMillis = DEFAULT_LONG_SESSION_THRESHOLD_MINUTES * 60L * 1000L,
            trackAllCategories = true,
            insightSensitivity = InsightSensitivity.MEDIUM
        )
    }
}
