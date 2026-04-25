package com.xmobile.project2digitalwellbeing.domain.usage.model

data class UsageFeatures(
    val totalScreenTimeMillis: Long,
    val lateNightUsageMillis: Long,
    val switchCount: Int,
    val averageSessionLengthMillis: Long,
    val topApps: List<TopAppFeature>
)
