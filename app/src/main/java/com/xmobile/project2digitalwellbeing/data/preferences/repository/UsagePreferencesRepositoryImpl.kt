package com.xmobile.project2digitalwellbeing.data.preferences.repository

import com.xmobile.project2digitalwellbeing.data.preferences.local.AppPreferencesDataStore
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UsagePreferencesRepositoryImpl @Inject constructor(
    private val appPreferencesDataStore: AppPreferencesDataStore
) : UsagePreferencesRepository {

    override val usageAnalysisPreferences: Flow<UsageAnalysisPreferences>
        get() = appPreferencesDataStore.usageAnalysisPreferences

    override suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences {
        return appPreferencesDataStore.getUsageAnalysisPreferences()
    }

    override suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) {
        appPreferencesDataStore.saveUsageAnalysisPreferences(preferences)
    }
}
