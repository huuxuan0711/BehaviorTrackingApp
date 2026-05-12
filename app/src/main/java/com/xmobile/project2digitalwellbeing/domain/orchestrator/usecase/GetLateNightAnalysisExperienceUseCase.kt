package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.InsightResolutionStrategy
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextParams
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.LateNightAnalysisData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.LateNightAnalysisDataError
import javax.inject.Inject
import kotlin.math.min

data class LateNightAnalysisExperienceData(
    val data: LateNightAnalysisData,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetLateNightAnalysisExperienceOutcome {
    data class Success(val data: LateNightAnalysisExperienceData) : GetLateNightAnalysisExperienceOutcome
    data class Failure(val error: LateNightAnalysisDataError) : GetLateNightAnalysisExperienceOutcome
}

class GetLateNightAnalysisExperienceUseCase @Inject constructor(
    private val getLateNightAnalysisDataUseCase: GetLateNightAnalysisDataUseCase,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase
) {

    suspend operator fun invoke(params: GetLateNightAnalysisDataParams): GetLateNightAnalysisExperienceOutcome {
        return when (val outcome = getLateNightAnalysisDataUseCase(params)) {
            is GetLateNightAnalysisDataOutcome.Failure -> GetLateNightAnalysisExperienceOutcome.Failure(outcome.error)
            is GetLateNightAnalysisDataOutcome.Success -> {
                val localSummary = outcome.data.insightSummary
                val context = buildCloudContext(outcome.data)
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val initialDecision = insightResolutionStrategy.resolve(
                    InsightResolutionContext(
                        hasRuleInsight = localSummary.isNotBlank(),
                        hasLocalReasoning = true,
                        allowCloudEnhancement = preferences.cloudEnhancementEnabled,
                        networkAvailable = networkStatusProvider.isNetworkAvailable(),
                        cloudEnhancementEligible = cloudSecretRepository.hasGeminiApiKey()
                    )
                )
                val cloudSummary = if (initialDecision.requestCloudEnhancement) {
                    generateCloudInsightTextUseCase(
                        GenerateCloudInsightTextParams(
                            surface = CloudInsightSurface.LATE_NIGHT_ANALYSIS,
                            groundedContext = context,
                            fallbackInsight = null,
                            languageCode = preferences.languageCode
                        )
                    ).getOrNull()?.text
                } else {
                    null
                }
                val effectiveMode = if (initialDecision.requestCloudEnhancement && cloudSummary == null) {
                    InsightResolutionMode.FALLBACK_TO_LOCAL
                } else {
                    initialDecision.mode
                }
                GetLateNightAnalysisExperienceOutcome.Success(
                    LateNightAnalysisExperienceData(
                        data = outcome.data,
                        resolutionMode = effectiveMode,
                        insightSummaryText = cloudSummary ?: localSummary
                    )
                )
            }
        }
    }

    private fun buildCloudContext(data: LateNightAnalysisData): LlmGroundedContext {
        val evidence = listOf(
            mapOf("pattern" to "LATE_NIGHT", "key" to "totalScreenTimeMillis", "value" to data.totalScreenTimeMillis.toString()),
            mapOf("pattern" to "LATE_NIGHT", "key" to "sessionCount", "value" to data.totalSessionCount.toString()),
            mapOf("pattern" to "LATE_NIGHT", "key" to "avgSessionMillis", "value" to data.averageSessionLengthMillis.toString())
        )
        val recs = listOf(
            mapOf(
                "title" to "Night routine",
                "description" to data.recommendation,
                "suggestedTimeLabel" to "After ${data.peakUsageWindowStartHour ?: 22}:00",
                "priority" to "1"
            )
        )
        return LlmGroundedContext(
            primaryPattern = "LATE_NIGHT_ANALYSIS",
            secondaryPatterns = emptyList(),
            riskScore = min(100, (data.totalScreenTimeMillis / (30L * 60L * 1000L)).toInt() * 10),
            confidence = 0.72f,
            summary = data.insightSummary,
            evidence = evidence,
            recommendations = recs
        )
    }
}
