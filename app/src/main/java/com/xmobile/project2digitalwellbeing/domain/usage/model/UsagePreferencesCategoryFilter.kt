package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

fun UsageAnalysisPreferences.shouldIncludeCategory(category: AppCategory): Boolean {
    if (trackAllCategories) return true
    return category != AppCategory.SYSTEM &&
        category != AppCategory.OTHER &&
        category != AppCategory.UNKNOWN
}
