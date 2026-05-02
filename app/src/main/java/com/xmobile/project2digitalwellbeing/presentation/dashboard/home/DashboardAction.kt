package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

sealed interface DashboardAction {
    data object OpenDailyOverview : DashboardAction
    data object OpenWeeklyOverview : DashboardAction
    data object OpenSessionTimeline : DashboardAction
    data object OpenBehaviorInsight : DashboardAction
}
