package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage

fun DashboardUiState.toInsightSummaryText(): String {
    return when {
        errorMessage != null && topInsight == null ->
            errorMessage

        topInsight != null -> {
            "${topInsight.description} (${topInsight.score}/100)"
        }

        else ->
            "No insights are available yet. Open your apps normally and check back again."
    }
}

fun List<HourlyUsage>.toLateNightRatioText(): String {
    val total = sumOf { it.totalTimeMillis }
    if (total <= 0L) return "0%"
    val lateNight = filter { it.hourOfDay >= 22 || it.hourOfDay < 6 }
        .sumOf { it.totalTimeMillis }
    return "${((lateNight.toDouble() / total.toDouble()) * 100).toInt()}%"
}
