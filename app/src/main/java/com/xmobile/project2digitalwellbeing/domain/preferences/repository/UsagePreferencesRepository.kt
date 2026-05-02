package com.xmobile.project2digitalwellbeing.domain.preferences.repository

import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import kotlinx.coroutines.flow.Flow

interface UsagePreferencesRepository {
    val usageAnalysisPreferences: Flow<UsageAnalysisPreferences>

    suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences

    suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences)
}
