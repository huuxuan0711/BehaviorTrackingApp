package com.xmobile.project2digitalwellbeing.domain.reasoning.model

import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight

data class CloudInsightRequest(
    val surface: CloudInsightSurface,
    val groundedContext: LlmGroundedContext,
    val fallbackInsight: InterpretedInsight? = null,
    val languageCode: String = "en"
)
