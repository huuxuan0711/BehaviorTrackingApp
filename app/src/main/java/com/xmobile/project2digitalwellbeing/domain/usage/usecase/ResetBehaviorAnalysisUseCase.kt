package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class ResetBehaviorAnalysisUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke() {
        usageRepository.deleteInsightsInRange(0, Long.MAX_VALUE)
    }
}
