package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class SessionTimelineUiState(
    val weekStartDate: LocalDate = currentWeekStart(),
    val dateRangeLabel: String = "",
    val insightText: String = "No session insight available yet.",
    val sessions: List<SessionTimelineItemUiModel> = emptyList(),
    val errorMessage: String? = null,
    val canNavigateNext: Boolean = false
)

private fun currentWeekStart(): LocalDate {
    return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
