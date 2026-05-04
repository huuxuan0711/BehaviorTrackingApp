package com.xmobile.project2digitalwellbeing.domain.insights.service

import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import javax.inject.Inject

data class InterpretedInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val suggestion: String,
    val score: Int
)

class InsightInterpreter @Inject constructor() {

    fun interpret(insight: Insight): InterpretedInsight {
        return InterpretedInsight(
            type = insight.type,
            title = insight.type.toTitle(),
            description = insight.type.toDescription(),
            suggestion = insight.type.toSuggestion(),
            score = insight.score
        )
    }

    private fun InsightType.toTitle(): String {
        return name.lowercase()
            .split('_')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    private fun InsightType.toDescription(): String {
        return when (this) {
            InsightType.WORK_HOUR_DISTRACTION -> "You're spending a lot of time on distracting apps during work hours."
            InsightType.MORNING_ROUTINE -> "Your morning starts with heavy phone usage."
            InsightType.CONSTANT_CHECKING -> "You tend to check your phone frequently for very short periods."
            InsightType.APP_RELIANCE -> "One app is dominating your screen time today."
            InsightType.LATE_NIGHT_USAGE -> "A meaningful share of your phone use happens late at night."
            InsightType.FREQUENT_SWITCHING -> "You switch between apps frequently, suggesting fragmented attention."
            InsightType.BINGE_USAGE -> "You spend long uninterrupted stretches inside the same app."
            InsightType.LATE_NIGHT_SWITCHING -> "Your late-night sessions include rapid switching between apps."
        }
    }

    private fun InsightType.toSuggestion(): String {
        return when (this) {
            InsightType.WORK_HOUR_DISTRACTION -> "Consider using a focus mode to stay on task."
            InsightType.MORNING_ROUTINE -> "Try a screen-free routine for the first 30 minutes of your day."
            InsightType.CONSTANT_CHECKING -> "Try batching your notification checks to reduce distractions."
            InsightType.APP_RELIANCE -> "Try to diversify your activities and set time limits for this app."
            InsightType.LATE_NIGHT_USAGE -> "Set a bedtime reminder to help you disconnect earlier."
            InsightType.FREQUENT_SWITCHING -> "Focus on one task at a time to improve your deep work."
            InsightType.BINGE_USAGE -> "Take regular breaks to avoid digital fatigue."
            InsightType.LATE_NIGHT_SWITCHING -> "Prioritize rest over late-night scrolling."
        }
    }
}
