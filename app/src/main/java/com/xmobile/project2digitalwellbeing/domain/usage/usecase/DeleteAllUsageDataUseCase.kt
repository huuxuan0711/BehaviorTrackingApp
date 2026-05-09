package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class DeleteAllUsageDataUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke() {
        usageRepository.deleteSessionsInRange(0, Long.MAX_VALUE)
        usageRepository.deleteInsightsInRange(0, Long.MAX_VALUE)
    }
}
