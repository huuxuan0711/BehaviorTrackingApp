package com.xmobile.project2digitalwellbeing.domain.insights.model

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter

data class TransitionInsight(
    val filter: TransitionFilter,
    val title: String,
    val summary: String,
    val score: Int,
    val confidence: Float,
    val totalTransitionCount: Int,
    val lateNightTransitionRatio: Float,
    val averageIntervalMillis: Long,
    val dominantTransition: AppTransitionStat
)
