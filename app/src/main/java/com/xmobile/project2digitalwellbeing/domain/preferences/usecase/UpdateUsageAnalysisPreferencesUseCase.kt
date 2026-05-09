package com.xmobile.project2digitalwellbeing.domain.preferences.usecase

import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import javax.inject.Inject

class UpdateUsageAnalysisPreferencesUseCase @Inject constructor(
    private val repository: UsagePreferencesRepository
) {
    suspend operator fun invoke(
        action: (UsageAnalysisPreferences) -> UsageAnalysisPreferences
    ) {
        val current = repository.getUsageAnalysisPreferences()
        repository.saveUsageAnalysisPreferences(action(current))
    }
}
