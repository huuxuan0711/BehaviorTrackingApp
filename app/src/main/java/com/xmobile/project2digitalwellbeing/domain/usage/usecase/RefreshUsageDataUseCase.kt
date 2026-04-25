package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.InsightEngine
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionBuilder
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor

data class RefreshUsageDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val forceFullRefresh: Boolean = false
)

data class RefreshUsageDataResult(
    val processedRangeStartMillis: Long,
    val processedRangeEndMillis: Long,
    val sessionsAffected: Int,
    val insightsGenerated: Int
)

class RefreshUsageDataUseCase(
    private val repository: UsageRepository,
    private val sessionBuilder: SessionBuilder,
    private val aggregator: UsageAggregator,
    private val featureExtractor: UsageFeatureExtractor,
    private val insightEngine: InsightEngine
) {
    suspend operator fun invoke(params: RefreshUsageDataParams): RefreshUsageDataResult {
        TODO("Scaffold only. Refresh orchestration will be implemented after session rules are finalized.")
    }
}
