package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class InsightResolutionContext(
    val hasRuleInsight: Boolean,
    val hasLocalReasoning: Boolean,
    val allowCloudEnhancement: Boolean = false,
    val networkAvailable: Boolean = false,
    val cloudEnhancementEligible: Boolean = false
)
