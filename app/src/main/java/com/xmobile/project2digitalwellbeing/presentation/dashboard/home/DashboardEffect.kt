package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

sealed interface DashboardEffect {
    data object OpenDailyOverview : DashboardEffect
    data object OpenWeeklyOverview : DashboardEffect
    data object OpenSessionTimeline : DashboardEffect
    data object OpenBehaviorInsight : DashboardEffect
    data object RequestPermission : DashboardEffect
}
