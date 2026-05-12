package com.xmobile.project2digitalwellbeing.domain.reasoning.service

import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision

interface InsightResolutionStrategy {
    fun resolve(context: InsightResolutionContext): InsightResolutionDecision
}
