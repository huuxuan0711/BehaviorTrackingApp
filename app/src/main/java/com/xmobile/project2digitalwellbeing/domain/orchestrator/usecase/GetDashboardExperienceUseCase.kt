package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
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
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import javax.inject.Inject

data class DashboardExperienceData(
    val dashboardData: com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardData,
    val behaviorReasoning: BehaviorReasoningResult?,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetDashboardExperienceOutcome {
    data class Success(val data: DashboardExperienceData) : GetDashboardExperienceOutcome
    data class Failure(
        val error: com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardDataError
    ) : GetDashboardExperienceOutcome
}

class GetDashboardExperienceUseCase @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val appRepository: AppRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val featureExtractor: UsageFeatureExtractor,
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val localInsightNarrator: LocalInsightNarrator
) {

    suspend operator fun invoke(params: GetDashboardDataParams): GetDashboardExperienceOutcome {
        return when (val dashboardOutcome = getDashboardDataUseCase(params)) {
            is GetDashboardDataOutcome.Failure -> GetDashboardExperienceOutcome.Failure(dashboardOutcome.error)
            is GetDashboardDataOutcome.Success -> {
                val dashboardData = dashboardOutcome.data
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val behaviorReasoning = buildBehaviorReasoning(
                    params = params,
                    dashboardData = dashboardData,
                    preferences = preferences
                )
                val initialResolutionDecision = insightResolutionStrategy.resolve(
                    InsightResolutionContext(
                        hasRuleInsight = dashboardData.topInsight != null,
                        hasLocalReasoning = behaviorReasoning?.primaryHypothesis != null,
                        allowCloudEnhancement = preferences.cloudEnhancementEnabled,
                        networkAvailable = networkStatusProvider.isNetworkAvailable(),
                        cloudEnhancementEligible = behaviorReasoning?.llmContext?.primaryPattern != null &&
                            cloudSecretRepository.hasGeminiApiKey()
                    )
                )
                val cloudInsightText = generateCloudInsightTextIfNeeded(
                    resolutionDecision = initialResolutionDecision,
                    behaviorReasoning = behaviorReasoning,
                    fallbackInsight = dashboardData.topInsight
                )
                val effectiveResolutionDecision = if (
                    initialResolutionDecision.requestCloudEnhancement && cloudInsightText == null
                ) {
                    initialResolutionDecision.copy(mode = InsightResolutionMode.FALLBACK_TO_LOCAL)
                } else {
                    initialResolutionDecision
                }
                val insightSummaryText = cloudInsightText ?: localInsightNarrator.narrate(
                    resolutionDecision = effectiveResolutionDecision,
                    reasoningResult = behaviorReasoning,
                    fallbackInsight = dashboardData.topInsight
                )
                GetDashboardExperienceOutcome.Success(
                    DashboardExperienceData(
                        dashboardData = dashboardData,
                        behaviorReasoning = behaviorReasoning,
                        resolutionMode = effectiveResolutionDecision.mode,
                        insightSummaryText = insightSummaryText
                    )
                )
            }
        }
    }

    private suspend fun buildBehaviorReasoning(
        params: GetDashboardDataParams,
        dashboardData: com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardData,
        preferences: com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
    ): BehaviorReasoningResult? {
        val appMetadataByPackage = appRepository.getAppMetadata(
            dashboardData.dailyUsage.sessions.map { it.packageName }.toSet()
        )
        val enrichedSessions = sessionEnricher.enrichSessions(
            sessions = dashboardData.dailyUsage.sessions,
            timezoneId = params.timezoneId,
            appMetadataByPackage = appMetadataByPackage,
            preferences = preferences
        )
        if (enrichedSessions.isEmpty()) {
            return null
        }

        val features = featureExtractor.extractFeatures(
            sessions = enrichedSessions,
            preferences = preferences
        )
        return getBehaviorReasoningUseCase(
            GetBehaviorReasoningParams(
                features = features,
                usageInsights = emptyList(),
                transitionInsight = null,
                dailyTrend = null,
                weeklyTrend = null,
                preferences = preferences
            )
        )
    }

    private suspend fun generateCloudInsightTextIfNeeded(
        resolutionDecision: com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision,
        behaviorReasoning: BehaviorReasoningResult?,
        fallbackInsight: com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight?
    ): String? {
        if (!resolutionDecision.requestCloudEnhancement) {
            return null
        }
        val llmContext = behaviorReasoning?.llmContext ?: return null
        return generateCloudInsightTextUseCase(
            GenerateCloudInsightTextParams(
                surface = CloudInsightSurface.DASHBOARD,
                groundedContext = llmContext,
                fallbackInsight = fallbackInsight
            )
        ).getOrNull()?.text
    }
}
