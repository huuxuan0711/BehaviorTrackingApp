package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.google.gson.GsonBuilder
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageExport
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class ExportUsageDataUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val preferencesRepository: UsagePreferencesRepository
) {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    suspend operator fun invoke(): String {
        return try {
            val sessions = usageRepository.getSessions(0, Long.MAX_VALUE)
            val insights = usageRepository.getInsights(0, Long.MAX_VALUE)
            val preferences = preferencesRepository.getUsageAnalysisPreferences()
            
            val exportData = UsageExport(
                sessions = sessions,
                insights = insights,
                rawEvents = emptyList(),
                preferences = preferences
            )
            
            gson.toJson(exportData)
        } catch (e: Exception) {
            "{\"error\": \"Export failed: ${e.message}\"}"
        }
    }
}
