package com.xmobile.project2digitalwellbeing.presentation.dashboard.model

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight

data class DashboardUiState(
    val isLoading: Boolean = false,
    val dailyUsage: DailyUsage? = null,
    val topInsight: Insight? = null,
    val hourlyUsage: List<HourlyUsage> = emptyList(),
    val topApps: List<AppUsageStat> = emptyList(),
    val errorMessage: String? = null
)
