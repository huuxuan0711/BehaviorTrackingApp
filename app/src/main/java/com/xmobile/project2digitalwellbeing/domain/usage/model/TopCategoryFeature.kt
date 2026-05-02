package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

data class TopCategoryFeature(
    val category: AppCategory,
    val totalTimeMillis: Long,
    val sessionCount: Int
)
