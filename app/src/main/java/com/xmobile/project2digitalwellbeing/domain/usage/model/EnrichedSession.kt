package com.xmobile.project2digitalwellbeing.domain.usage.model

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession

data class EnrichedSession(
    val session: AppSession,
    val appName: String?,
    val category: AppCategory,
    val hourOfDay: Int,
    val isLateNight: Boolean
)
