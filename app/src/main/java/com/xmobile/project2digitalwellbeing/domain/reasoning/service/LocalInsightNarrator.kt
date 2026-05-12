package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision

interface LocalInsightNarrator {
    fun narrate(
        resolutionDecision: InsightResolutionDecision,
        reasoningResult: BehaviorReasoningResult?,
        fallbackInsight: InterpretedInsight?,
        languageCode: String
    ): String
}
