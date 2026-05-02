package com.xmobile.project2digitalwellbeing.domain.usage.model

data class SessionLengthDistribution(
    val shortSessionCount: Int,
    val mediumSessionCount: Int,
    val longSessionCount: Int,
    val shortSessionRatio: Float,
    val mediumSessionRatio: Float,
    val longSessionRatio: Float
)
