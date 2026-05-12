package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class BehaviorReasoningResult(
    val primaryHypothesis: BehaviorHypothesis?,
    val secondaryHypotheses: List<BehaviorHypothesis>,
    val recommendations: List<InterventionRecommendation>,
    val summary: String,
    val llmContext: LlmGroundedContext
)
