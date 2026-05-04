package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight

data class DashboardUiState(
    val isLoading: Boolean = false,
    val currentDateLabel: String = "",
    val dailyUsage: DailyUsage? = null,
    val topInsight: InterpretedInsight? = null,
    val hourlyUsage: List<HourlyUsage> = emptyList(),
    val topApps: List<AppUsageStat> = emptyList(),
    val errorMessage: String? = null,
    val insightSummaryText: String = "",
    val lateNightRatioText: String = "0%"
)
