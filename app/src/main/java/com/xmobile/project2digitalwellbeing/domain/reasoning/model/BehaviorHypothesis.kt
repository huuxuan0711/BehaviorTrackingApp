package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class BehaviorHypothesis(
    val pattern: BehaviorPatternType,
    val summary: String,
    val evidence: List<BehaviorEvidence>,
    val riskScore: Int,
    val confidence: Float
)
