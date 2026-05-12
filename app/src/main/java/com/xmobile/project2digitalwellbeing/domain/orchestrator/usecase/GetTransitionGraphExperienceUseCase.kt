package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.CloudInsightSurface
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightInterpreter
import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.InsightResolutionStrategy
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.LocalInsightNarrator
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextParams
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextUseCase
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GetBehaviorReasoningParams
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GetBehaviorReasoningUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.TransitionGraphData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.TransitionGraphDataError
import javax.inject.Inject

data class TransitionGraphExperienceData(
    val data: TransitionGraphData,
    val behaviorReasoning: BehaviorReasoningResult?,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetTransitionGraphExperienceOutcome {
    data class Success(val data: TransitionGraphExperienceData) : GetTransitionGraphExperienceOutcome
    data class Failure(val error: TransitionGraphDataError) : GetTransitionGraphExperienceOutcome
}

class GetTransitionGraphExperienceUseCase @Inject constructor(
    private val getTransitionGraphDataUseCase: GetTransitionGraphDataUseCase,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase,
    private val localInsightNarrator: LocalInsightNarrator,
    private val insightInterpreter: InsightInterpreter // Injecting InsightInterpreter
) {

    suspend operator fun invoke(params: GetTransitionGraphDataParams): GetTransitionGraphExperienceOutcome {
        return when (val outcome = getTransitionGraphDataUseCase(params)) {
            is GetTransitionGraphDataOutcome.Failure -> GetTransitionGraphExperienceOutcome.Failure(outcome.error)
            is GetTransitionGraphDataOutcome.Success -> {
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val data = outcome.data

                // 1. Build behavior reasoning (local graph/chain analysis)
                val transitionInsight = data.insight

                val interpretedInsight = transitionInsight?.let {
                    InterpretedInsight(
                        type = com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType.LATE_NIGHT_SWITCHING, // Using generic type as fallback
                        title = it.title,
                        description = it.summary,
                        suggestion = "Review app transitions",
                        score = it.score
                    )
                }

                val behaviorReasoning = getBehaviorReasoningUseCase(
                    GetBehaviorReasoningParams(
                        features = data.features,
                        usageInsights = emptyList(), // usageInsights is for general insight models, we pass transitionInsight natively below
                        transitionInsight = transitionInsight,
                        dailyTrend = null,
                        weeklyTrend = null,
                        preferences = preferences
                    )
                )

                // 2. Resolve Strategy
                val hasLocalSummary = data.insight?.summary?.isNotBlank() == true
                val initialDecision = insightResolutionStrategy.resolve(
                    InsightResolutionContext(
                        hasRuleInsight = hasLocalSummary,
                        hasLocalReasoning = behaviorReasoning.primaryHypothesis != null,
                        allowCloudEnhancement = preferences.cloudEnhancementEnabled,
                        networkAvailable = networkStatusProvider.isNetworkAvailable(),
                        cloudEnhancementEligible = behaviorReasoning.llmContext?.primaryPattern != null &&
                            cloudSecretRepository.hasGeminiApiKey()
                    )
                )

                // 3. Attempt Cloud Generation
                val cloudInsightText = if (initialDecision.requestCloudEnhancement && behaviorReasoning.llmContext != null) {
                    generateCloudInsightTextUseCase(
                        GenerateCloudInsightTextParams(
                            surface = CloudInsightSurface.TRANSITION_GRAPH,
                            groundedContext = behaviorReasoning.llmContext,
                            fallbackInsight = null,
                            languageCode = preferences.languageCode
                        )
                    ).getOrNull()?.text
                } else {
                    null
                }

                // 4. Fallback
                val effectiveResolutionDecision = if (
                    initialDecision.requestCloudEnhancement && cloudInsightText == null
                ) {
                    initialDecision.copy(mode = InsightResolutionMode.FALLBACK_TO_LOCAL)
                } else {
                    initialDecision
                }

                val insightSummaryText = cloudInsightText ?: localInsightNarrator.narrate(
                    resolutionDecision = effectiveResolutionDecision,
                    reasoningResult = behaviorReasoning,
                    fallbackInsight = interpretedInsight,
                    languageCode = preferences.languageCode
                )

                GetTransitionGraphExperienceOutcome.Success(
                    TransitionGraphExperienceData(
                        data = data,
                        behaviorReasoning = behaviorReasoning,
                        resolutionMode = effectiveResolutionDecision.mode,
                        insightSummaryText = insightSummaryText
                    )
                )
            }
        }
    }
}
