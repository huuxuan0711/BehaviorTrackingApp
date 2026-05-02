package com.xmobile.project2digitalwellbeing.domain.usage.model

data class UsageTrend(
    val currentValueMillis: Long,
    val previousValueMillis: Long,
    val deltaMillis: Long,
    val deltaRatio: Float,
    val direction: UsageTrendDirection,
    val summary: String
)
