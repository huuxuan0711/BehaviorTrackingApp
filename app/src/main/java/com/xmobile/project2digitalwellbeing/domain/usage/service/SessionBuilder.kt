package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent

interface SessionBuilder {
    fun buildSessions(
        events: List<AppUsageEvent>,
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        nowMillis: Long
    ): List<AppSession>
}
