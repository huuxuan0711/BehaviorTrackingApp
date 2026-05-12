package com.xmobile.project2digitalwellbeing.domain.reasoning.model

import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend

data class BehaviorReasoningInput(
    val features: UsageFeatures,
    val usageInsights: List<Insight>,
    val transitionInsight: TransitionInsight?,
    val dailyTrend: UsageTrend?,
    val weeklyTrend: UsageTrend?,
    val baseline: BehaviorBaseline = BehaviorBaseline.EMPTY,
    val preferences: UsageAnalysisPreferences
)
