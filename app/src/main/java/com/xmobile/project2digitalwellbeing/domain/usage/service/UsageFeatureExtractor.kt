package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures

interface UsageFeatureExtractor {
    fun extractFeatures(sessions: List<EnrichedSession>): UsageFeatures
}
