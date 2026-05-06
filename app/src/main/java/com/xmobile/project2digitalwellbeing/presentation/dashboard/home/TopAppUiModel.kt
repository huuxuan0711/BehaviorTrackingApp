package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.graphics.drawable.Drawable
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class TopAppUiModel(
    val name: String,
    val durationText: String,
    val progressRatio: Float,
    val icon: Drawable?,
    val category: AppCategory
)
