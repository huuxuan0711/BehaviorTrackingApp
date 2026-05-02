package com.xmobile.project2digitalwellbeing.domain.insights.model

import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter

data class ComposedInsight(
    val title: String,
    val summary: String,
    val score: Int,
    val confidence: Float,
    val sourceInsightTypes: List<InsightType>,
    val transitionFilter: TransitionFilter?,
    val relatedPackages: List<String>
)
