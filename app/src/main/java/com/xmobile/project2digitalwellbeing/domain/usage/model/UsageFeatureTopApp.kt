package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class UsageFeatureTopApp(
    val packageName: String,
    val appName: String?,
    val category: AppCategory,
    val totalTimeMillis: Long,
    val launchCount: Int
)
