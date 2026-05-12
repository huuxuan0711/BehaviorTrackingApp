package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightInterpreter
import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.InsightResolutionStrategy
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.LocalInsightNarrator
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextParams
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextUseCase
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GetBehaviorReasoningParams
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GetBehaviorReasoningUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.UsagePatternData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.UsagePatternDataError
import javax.inject.Inject

data class UsagePatternExperienceData(
    val data: UsagePatternData,
    val behaviorReasoning: BehaviorReasoningResult?,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetUsagePatternExperienceOutcome {
    data class Success(val data: UsagePatternExperienceData) : GetUsagePatternExperienceOutcome
    data class Failure(val error: UsagePatternDataError) : GetUsagePatternExperienceOutcome
}

class GetUsagePatternExperienceUseCase @Inject constructor(
    private val getUsagePatternDataUseCase: GetUsagePatternDataUseCase,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase,
    private val localInsightNarrator: LocalInsightNarrator,
    private val insightInterpreter: InsightInterpreter
) {

    suspend operator fun invoke(params: GetUsagePatternDataParams): GetUsagePatternExperienceOutcome {
        return when (val outcome = getUsagePatternDataUseCase(params)) {
            is GetUsagePatternDataOutcome.Failure -> GetUsagePatternExperienceOutcome.Failure(outcome.error)
            is GetUsagePatternDataOutcome.Success -> {
                val data = outcome.data
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val behaviorReasoning = getBehaviorReasoningUseCase(
                    GetBehaviorReasoningParams(
                        features = data.features,
                        usageInsights = listOfNotNull(data.topInsight),
                        transitionInsight = null,
                        dailyTrend = null,
                        weeklyTrend = null,
                        preferences = preferences
                    )
                )
                val initialDecision = insightResolutionStrategy.resolve(
                    InsightResolutionContext(
                        hasRuleInsight = data.topInsight != null,
                        hasLocalReasoning = behaviorReasoning.primaryHypothesis != null,
                        allowCloudEnhancement = preferences.cloudEnhancementEnabled,
                        networkAvailable = networkStatusProvider.isNetworkAvailable(),
                        cloudEnhancementEligible = behaviorReasoning.llmContext.primaryPattern != null &&
                            cloudSecretRepository.hasGeminiApiKey()
                    )
                )
                val llmContext = behaviorReasoning.llmContext
                val cloudInsightText = if (initialDecision.requestCloudEnhancement) {
                    generateCloudInsightTextUseCase(
                        GenerateCloudInsightTextParams(
                            surface = CloudInsightSurface.USAGE_PATTERN,
                            groundedContext = llmContext,
                            fallbackInsight = data.topInsight?.let(insightInterpreter::interpret),
                            languageCode = preferences.languageCode
                        )
                    ).getOrNull()?.text
                } else {
                    null
                }
                val effectiveDecision = if (initialDecision.requestCloudEnhancement && cloudInsightText == null) {
                    initialDecision.copy(mode = InsightResolutionMode.FALLBACK_TO_LOCAL)
                } else {
                    initialDecision
                }
                val insightSummaryText = cloudInsightText ?: localInsightNarrator.narrate(
                    resolutionDecision = effectiveDecision,
                    reasoningResult = behaviorReasoning,
                    fallbackInsight = data.topInsight?.let(insightInterpreter::interpret),
                    languageCode = preferences.languageCode
                )

                GetUsagePatternExperienceOutcome.Success(
                    UsagePatternExperienceData(
                        data = data,
                        behaviorReasoning = behaviorReasoning,
                        resolutionMode = effectiveDecision.mode,
                        insightSummaryText = insightSummaryText
                    )
                )
            }
        }
    }
}
