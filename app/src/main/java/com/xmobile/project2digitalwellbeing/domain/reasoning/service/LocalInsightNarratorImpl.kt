package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision
import javax.inject.Inject

class LocalInsightNarratorImpl @Inject constructor() : LocalInsightNarrator {

    override fun narrate(
        resolutionDecision: InsightResolutionDecision,
        reasoningResult: BehaviorReasoningResult?,
        fallbackInsight: InterpretedInsight?
    ): String {
        val reasoning = reasoningResult?.primaryHypothesis
        if (resolutionDecision.useLocalReasoning && reasoning != null) {
            val recommendation = reasoningResult.recommendations.firstOrNull()?.description
            return buildString {
                append(reasoningResult.summary)
                if (!recommendation.isNullOrBlank()) {
                    append(" ")
                    append(recommendation)
                }
            }.trim()
        }

        if (resolutionDecision.useRuleInsight && fallbackInsight != null) {
            return "${fallbackInsight.description} ${fallbackInsight.suggestion}".trim()
        }

        return DEFAULT_EMPTY_TEXT
    }

    private companion object {
        private const val DEFAULT_EMPTY_TEXT =
            "No clear pattern yet. Use your phone normally, then pull to refresh."
    }
}
