package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class CategoryUsage(
    val category: AppCategory,
    val totalTimeMillis: Long,
    val sessionCount: Int,
    val appCount: Int,
    val packageNames: List<String>
)
