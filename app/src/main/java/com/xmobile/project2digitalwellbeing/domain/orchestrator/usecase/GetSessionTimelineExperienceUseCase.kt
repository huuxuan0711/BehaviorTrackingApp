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
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetSessionTimelineDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.SessionTimelineData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.SessionTimelineDataError
import javax.inject.Inject

data class SessionTimelineExperienceData(
    val data: SessionTimelineData,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetSessionTimelineExperienceOutcome {
    data class Success(val data: SessionTimelineExperienceData) : GetSessionTimelineExperienceOutcome
    data class Failure(val error: SessionTimelineDataError) : GetSessionTimelineExperienceOutcome
}

class GetSessionTimelineExperienceUseCase @Inject constructor(
    private val getSessionTimelineDataUseCase: GetSessionTimelineDataUseCase,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase
) {

    suspend operator fun invoke(params: GetSessionTimelineDataParams): GetSessionTimelineExperienceOutcome {
        return when (val outcome = getSessionTimelineDataUseCase(params)) {
            is GetSessionTimelineDataOutcome.Failure -> GetSessionTimelineExperienceOutcome.Failure(outcome.error)
            is GetSessionTimelineDataOutcome.Success -> {
                val localSummary = outcome.data.insight?.summary?.takeIf { it.isNotBlank() } ?: DEFAULT_EMPTY_TEXT
                val context = buildCloudContext(outcome.data)
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val initialDecision = insightResolutionStrategy.resolve(
                    InsightResolutionContext(
                        hasRuleInsight = outcome.data.insight != null,
                        hasLocalReasoning = outcome.data.insight != null,
                        allowCloudEnhancement = preferences.cloudEnhancementEnabled,
                        networkAvailable = networkStatusProvider.isNetworkAvailable(),
                        cloudEnhancementEligible = cloudSecretRepository.hasGeminiApiKey() &&
                            context.primaryPattern != null
                    )
                )
                val cloudSummary = if (initialDecision.requestCloudEnhancement) {
                    generateCloudInsightTextUseCase(
                        GenerateCloudInsightTextParams(
                            surface = CloudInsightSurface.SESSION_TIMELINE,
                            groundedContext = context,
                            fallbackInsight = null
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
                GetSessionTimelineExperienceOutcome.Success(
                    SessionTimelineExperienceData(
                        data = outcome.data,
                        resolutionMode = effectiveMode,
                        insightSummaryText = cloudSummary ?: localSummary
                    )
                )
            }
        }
    }

    private fun buildCloudContext(data: SessionTimelineData): LlmGroundedContext {
        val insight = data.insight
        val primaryPattern = if (insight != null) "SESSION_TRANSITION_LOOP" else null
        val evidence = if (insight != null) {
            listOf(
                mapOf("pattern" to "SESSION_TRANSITION", "key" to "totalTransitionCount", "value" to insight.totalTransitionCount.toString()),
                mapOf("pattern" to "SESSION_TRANSITION", "key" to "lateNightTransitionRatio", "value" to insight.lateNightTransitionRatio.toString()),
                mapOf("pattern" to "SESSION_TRANSITION", "key" to "averageIntervalMillis", "value" to insight.averageIntervalMillis.toString())
            )
        } else {
            emptyList()
        }
        val recommendations = if (insight != null) {
            listOf(
                mapOf(
                    "title" to "Reduce switch loops",
                    "description" to "Batch related tasks and avoid rapid app hopping during focused sessions.",
                    "suggestedTimeLabel" to "During deep work blocks",
                    "priority" to "1"
                )
            )
        } else {
            listOf(
                mapOf(
                    "title" to "Continue tracking",
                    "description" to "Use your phone normally to collect enough transition data.",
                    "suggestedTimeLabel" to "Today",
                    "priority" to "1"
                )
            )
        }
        return LlmGroundedContext(
            primaryPattern = primaryPattern,
            secondaryPatterns = emptyList(),
            riskScore = insight?.score ?: 0,
            confidence = insight?.confidence ?: 0f,
            summary = insight?.summary ?: DEFAULT_EMPTY_TEXT,
            evidence = evidence,
            recommendations = recommendations
        )
    }

    private companion object {
        private const val DEFAULT_EMPTY_TEXT =
            "No session insight yet. Meaningful patterns will appear after more usage is recorded."
    }
}
