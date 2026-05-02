package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

sealed interface SessionTimelineEffect {
    data object OpenPermission : SessionTimelineEffect
}
