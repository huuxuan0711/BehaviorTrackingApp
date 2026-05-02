package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import java.time.LocalDate

sealed interface DailyOverviewEffect {
    data class ShowDatePicker(val selectedDate: LocalDate) : DailyOverviewEffect
    data object OpenPermission : DailyOverviewEffect
}
