package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class SessionTimelineItemUiModel(
    val packageName: String,
    val appName: String,
    val periodLabel: String,
    val timeRangeText: String,
    val durationText: String,
    val durationMillis: Long,
    val progressRatio: Float,
    val transitionLabel: String?,
    val category: AppCategory
)
