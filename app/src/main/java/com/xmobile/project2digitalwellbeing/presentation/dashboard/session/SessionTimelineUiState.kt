package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import java.time.LocalDate

data class SessionTimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dateRangeLabel: String = "",
    val insightText: String = "",
    val sessions: List<SessionTimelineItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canNavigateNext: Boolean = false
)
