package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent

interface SessionBuilder {
    fun buildSessions(
        events: List<AppUsageEvent>,
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        nowMillis: Long
    ): List<AppSession>
}
