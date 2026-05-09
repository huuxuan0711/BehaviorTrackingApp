package com.xmobile.project2digitalwellbeing.domain.preferences.usecase

import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUsageAnalysisPreferencesUseCase @Inject constructor(
    private val repository: UsagePreferencesRepository
) {
    operator fun invoke(): Flow<UsageAnalysisPreferences> {
        return repository.usageAnalysisPreferences
    }
}
