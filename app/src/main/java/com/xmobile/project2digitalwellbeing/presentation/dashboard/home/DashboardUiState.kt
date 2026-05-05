package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight

data class DashboardUiState(
    val isLoading: Boolean = false,
    val currentDateLabel: String = "Today",
    val dailyUsage: DailyUsage? = null,
    val topInsight: InterpretedInsight? = null,
    val hourlyUsage: List<HourlyUsage> = emptyList(),
    val topApps: List<AppUsageStat> = emptyList(),
    val errorMessage: String? = null,
    val insightSummaryText: String = "No clear pattern yet. Use your phone normally, then pull to refresh.",
    val lateNightRatioText: String = "0%"
)
