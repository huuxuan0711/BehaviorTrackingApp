package com.xmobile.project2digitalwellbeing.domain.reasoning.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorBaseline
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningInput
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.BehaviorReasoningEngine
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend
import javax.inject.Inject

data class GetBehaviorReasoningParams(
    val features: UsageFeatures,
    val usageInsights: List<Insight>,
    val transitionInsight: TransitionInsight?,
    val dailyTrend: UsageTrend?,
    val weeklyTrend: UsageTrend?,
    val baseline: BehaviorBaseline = BehaviorBaseline.EMPTY,
    val preferences: UsageAnalysisPreferences
)

class GetBehaviorReasoningUseCase @Inject constructor(
    private val behaviorReasoningEngine: BehaviorReasoningEngine
) {
    operator fun invoke(params: GetBehaviorReasoningParams): BehaviorReasoningResult {
        return behaviorReasoningEngine.reason(
            input = BehaviorReasoningInput(
                features = params.features,
                usageInsights = params.usageInsights,
                transitionInsight = params.transitionInsight,
                dailyTrend = params.dailyTrend,
                weeklyTrend = params.weeklyTrend,
                baseline = params.baseline,
                preferences = params.preferences
            )
        )
    }
}
