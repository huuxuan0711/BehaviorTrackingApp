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
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrendDirection
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.WeeklyOverviewData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.WeeklyOverviewDataError
import javax.inject.Inject
import kotlin.math.abs

data class WeeklyOverviewExperienceData(
    val weeklyData: WeeklyOverviewData,
    val resolutionMode: InsightResolutionMode,
    val insightSummaryText: String
)

sealed interface GetWeeklyOverviewExperienceOutcome {
    data class Success(val data: WeeklyOverviewExperienceData) : GetWeeklyOverviewExperienceOutcome
    data class Failure(val error: WeeklyOverviewDataError) : GetWeeklyOverviewExperienceOutcome
}

class GetWeeklyOverviewExperienceUseCase @Inject constructor(
    private val getWeeklyOverviewDataUseCase: GetWeeklyOverviewDataUseCase,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val cloudSecretRepository: CloudSecretRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val insightResolutionStrategy: InsightResolutionStrategy,
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase
) {

    suspend operator fun invoke(params: GetWeeklyOverviewDataParams): GetWeeklyOverviewExperienceOutcome {
        return when (val outcome = getWeeklyOverviewDataUseCase(params)) {
            is GetWeeklyOverviewDataOutcome.Failure -> GetWeeklyOverviewExperienceOutcome.Failure(outcome.error)
            is GetWeeklyOverviewDataOutcome.Success -> {
                val preferences = usagePreferencesRepository.getUsageAnalysisPreferences()
                val localSummary = outcome.data.trend.toFriendlySummary(preferences.languageCode)
                val cloudContext = buildCloudContext(outcome.data, preferences.languageCode)
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
                            surface = CloudInsightSurface.WEEKLY_OVERVIEW,
                            groundedContext = cloudContext,
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

                GetWeeklyOverviewExperienceOutcome.Success(
                    WeeklyOverviewExperienceData(
                        weeklyData = outcome.data,
                        resolutionMode = effectiveMode,
                        insightSummaryText = cloudSummary ?: localSummary
                    )
                )
            }
        }
    }

    private fun buildCloudContext(data: WeeklyOverviewData, languageCode: String): LlmGroundedContext {
        val topApps = data.topApps.take(3).map {
            mapOf(
                "title" to (it.appName ?: it.packageName),
                "description" to "total=${it.totalTimeMillis} launch=${it.launchCount}",
                "suggestedTimeLabel" to when (languageCode.lowercase()) {
                    "vi" -> "hàng tuần"
                    "fr" -> "hebdomadaire"
                    "de" -> "wöchentlich"
                    else -> "weekly"
                },
                "priority" to "1"
            )
        }
        val evidence = listOf(
            mapOf("pattern" to "WEEKLY_TREND", "key" to "deltaRatio", "value" to data.trend.deltaRatio.toString()),
            mapOf("pattern" to "WEEKLY_TREND", "key" to "totalScreenTimeMillis", "value" to data.weeklyUsage.totalScreenTimeMillis.toString())
        )
        return LlmGroundedContext(
            primaryPattern = "WEEKLY_TREND",
            secondaryPatterns = emptyList(),
            riskScore = (abs(data.trend.deltaRatio) * 100f).toInt().coerceIn(0, 100),
            confidence = 0.7f,
            summary = data.trend.toFriendlySummary(languageCode),
            evidence = evidence,
            recommendations = if (topApps.isEmpty()) {
                listOf(
                    mapOf(
                        "title" to when (languageCode.lowercase()) {
                            "vi" -> "Xem lại mức dùng tuần"
                            "fr" -> "Revoir l'usage hebdomadaire"
                            "de" -> "Wochennutzung prüfen"
                            else -> "Review weekly usage"
                        },
                        "description" to when (languageCode.lowercase()) {
                            "vi" -> "Đặt một mục tiêu thời gian màn hình cho tuần tới."
                            "fr" -> "Fixez un objectif de temps d'écran pour la semaine prochaine."
                            "de" -> "Setzen Sie ein Bildschirmzeit-Ziel für die nächste Woche."
                            else -> "Set one screen-time target for next week."
                        },
                        "suggestedTimeLabel" to when (languageCode.lowercase()) {
                            "vi" -> "Sáng thứ Hai"
                            "fr" -> "Lundi matin"
                            "de" -> "Montagmorgen"
                            else -> "Monday morning"
                        },
                        "priority" to "1"
                    )
                )
            } else {
                topApps
            }
        )
    }

    private fun com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend.toFriendlySummary(languageCode: String): String {
        val ratioPercent = (abs(deltaRatio) * 100f).toInt()
        return when (languageCode.lowercase()) {
            "vi" -> when (direction) {
                UsageTrendDirection.UP -> "Thời gian màn hình của bạn đã tăng $ratioPercent% so với tuần trước."
                UsageTrendDirection.DOWN -> "Thời gian màn hình của bạn đã giảm $ratioPercent% so với tuần trước."
                UsageTrendDirection.FLAT -> "Thời gian màn hình của bạn đang ổn định so với tuần trước."
            }
            "fr" -> when (direction) {
                UsageTrendDirection.UP -> "Votre temps d'écran a augmenté de $ratioPercent% par rapport à la semaine dernière."
                UsageTrendDirection.DOWN -> "Votre temps d'écran a diminué de $ratioPercent% par rapport à la semaine dernière."
                UsageTrendDirection.FLAT -> "Votre temps d'écran est stable par rapport à la semaine dernière."
            }
            "de" -> when (direction) {
                UsageTrendDirection.UP -> "Ihre Bildschirmzeit ist im Vergleich zur letzten Woche um $ratioPercent% gestiegen."
                UsageTrendDirection.DOWN -> "Ihre Bildschirmzeit ist im Vergleich zur letzten Woche um $ratioPercent% gesunken."
                UsageTrendDirection.FLAT -> "Ihre Bildschirmzeit ist im Vergleich zur letzten Woche stabil."
            }
            else -> when (direction) {
                UsageTrendDirection.UP -> "Your screen time increased by $ratioPercent% compared to last week."
                UsageTrendDirection.DOWN -> "Your screen time decreased by $ratioPercent% compared to last week."
                UsageTrendDirection.FLAT -> "Your screen time is stable compared to last week."
            }
        }
    }
}
