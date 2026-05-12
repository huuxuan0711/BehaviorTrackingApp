package com.xmobile.project2digitalwellbeing.domain.reasoning.model

data class LlmGroundedContext(
    val primaryPattern: String?,
    val secondaryPatterns: List<String>,
    val riskScore: Int,
    val confidence: Float,
    val summary: String,
    val evidence: List<Map<String, String>>,
    val recommendations: List<Map<String, String>>
)
