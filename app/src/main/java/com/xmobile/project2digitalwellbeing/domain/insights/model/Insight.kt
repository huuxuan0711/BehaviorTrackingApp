package com.xmobile.project2digitalwellbeing.domain.insights.model

data class Insight(
    val type: InsightType,
    val score: Int,
    val confidence: Float,
    val evidence: Map<String, String>,
    val relatedPackages: List<String>
)
