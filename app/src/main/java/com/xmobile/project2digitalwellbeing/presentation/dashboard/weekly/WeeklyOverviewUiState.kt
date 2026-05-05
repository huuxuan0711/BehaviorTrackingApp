package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeeklyOverviewUiState(
    val weekStartDate: LocalDate = currentWeekStart(),
    val dateRangeLabel: String = "",
    val averageDailyScreenTimeText: String = "0m",
    val mostUsedDayText: String = "No data yet",
    val totalScreenTimeText: String = "0m",
    val trendText: String = "No weekly trend yet. Patterns become clearer after a few active days.",
    val chartBars: List<WeeklyChartBarUiModel> = emptyList(),
    val topApps: List<AppUsageStat> = emptyList(),
    val errorMessage: String? = null,
    val canNavigateNext: Boolean = false
)

private fun currentWeekStart(): LocalDate {
    return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
