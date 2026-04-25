package com.xmobile.project2digitalwellbeing.domain.usage.model

data class AppUsageStat(
    val packageName: String,
    val appName: String?,
    val category: AppCategory,
    val totalTimeMillis: Long,
    val launchCount: Int
)
