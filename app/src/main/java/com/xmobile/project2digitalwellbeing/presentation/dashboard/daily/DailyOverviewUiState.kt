package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import java.time.LocalDate

data class DailyOverviewUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val dateLabel: String = "Today",
    val totalScreenTimeText: String = "0m",
    val compareText: String = "No yesterday data yet",
    val sessionCountText: String = "0",
    val longestSessionText: String = "0m",
    val hourlyUsage: List<HourlyUsage> = emptyList(),
    val topApps: List<AppUsageStat> = emptyList(),
    val insightText: String = "No insights are available yet.",
    val errorMessage: String? = null
)
