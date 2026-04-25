package com.xmobile.project2digitalwellbeing.domain.usage.model

data class AppUsageEvent(
    val packageName: String,
    val timestampMillis: Long,
    val type: UsageEventType
)
