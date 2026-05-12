package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class InsightResolutionDecision(
    val mode: InsightResolutionMode,
    val useRuleInsight: Boolean,
    val useLocalReasoning: Boolean,
    val requestCloudEnhancement: Boolean
)
