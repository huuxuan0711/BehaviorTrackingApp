package com.xmobile.project2digitalwellbeing.domain.tracking.service

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession

interface TransitionExtractor {
    fun extractTransitions(sessions: List<EnrichedSession>): List<AppTransitionStat>
}
