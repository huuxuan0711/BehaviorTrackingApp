package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

sealed interface WeeklyOverviewEffect {
    data object OpenPermission : WeeklyOverviewEffect
}
