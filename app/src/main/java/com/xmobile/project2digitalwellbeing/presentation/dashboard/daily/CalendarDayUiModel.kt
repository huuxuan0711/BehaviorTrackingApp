package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import java.time.LocalDate

data class CalendarDayUiModel(
    val date: LocalDate?,
    val label: String,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isEnabled: Boolean
)
