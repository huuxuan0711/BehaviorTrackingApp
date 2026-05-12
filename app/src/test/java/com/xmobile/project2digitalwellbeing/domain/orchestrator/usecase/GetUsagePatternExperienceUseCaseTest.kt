package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightInterpreter
import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.BehaviorReasoningResult
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.LlmGroundedContext
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.InsightResolutionStrategy
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.LocalInsightNarrator
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextUseCase
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GetBehaviorReasoningUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetUsagePatternDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.UsagePatternData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetUsagePatternExperienceUseCaseTest {

    private val getUsagePatternDataUseCase: GetUsagePatternDataUseCase = mock()
    private val usagePreferencesRepository: UsagePreferencesRepository = mock()
    private val cloudSecretRepository: CloudSecretRepository = mock()
    private val networkStatusProvider: NetworkStatusProvider = mock()
    private val insightResolutionStrategy: InsightResolutionStrategy = mock()
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase = mock()
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase = mock()
    private val localInsightNarrator: LocalInsightNarrator = mock()

    private lateinit var useCase: GetUsagePatternExperienceUseCase

    @Before
    fun setup() {
        useCase = GetUsagePatternExperienceUseCase(
            getUsagePatternDataUseCase = getUsagePatternDataUseCase,
            usagePreferencesRepository = usagePreferencesRepository,
            cloudSecretRepository = cloudSecretRepository,
            networkStatusProvider = networkStatusProvider,
            insightResolutionStrategy = insightResolutionStrategy,
            getBehaviorReasoningUseCase = getBehaviorReasoningUseCase,
            generateCloudInsightTextUseCase = generateCloudInsightTextUseCase,
            localInsightNarrator = localInsightNarrator,
            insightInterpreter = InsightInterpreter()
        )
    }

    @Test
    fun `invoke returns local narrated usage pattern experience`() = runBlocking {
        val params = GetUsagePatternDataParams(nowMillis = 0L, timezoneId = "UTC")
        val features = features()
        val data = UsagePatternData(
            currentLocalDate = "1970-01-01",
            totalSessionCount = 3,
            averageSessionLengthMillis = 60_000L,
            longestSessionMillis = 120_000L,
            switchCount = 2,
            averageSwitchIntervalMillis = 30_000L,
            sessionLengthDistribution = features.sessionLengthDistribution,
            topAppsByLaunchCount = emptyList(),
            topInsight = null,
            features = features
        )
        whenever(getUsagePatternDataUseCase.invoke(params)).thenReturn(GetUsagePatternDataOutcome.Success(data))
        whenever(usagePreferencesRepository.getUsageAnalysisPreferences()).thenReturn(UsageAnalysisPreferences.DEFAULT)
        whenever(getBehaviorReasoningUseCase.invoke(any())).thenReturn(reasoningResult())
        whenever(insightResolutionStrategy.resolve(any())).thenReturn(
            InsightResolutionDecision(
                mode = InsightResolutionMode.LOCAL_REASONING,
                useRuleInsight = false,
                useLocalReasoning = true,
                requestCloudEnhancement = false
            )
        )
        whenever(localInsightNarrator.narrate(any(), any(), anyOrNull(), any())).thenReturn("Usage pattern local summary")

        val result = useCase(params)

        assertTrue(result is GetUsagePatternExperienceOutcome.Success)
        val success = result as GetUsagePatternExperienceOutcome.Success
        assertEquals(data, success.data.data)
        assertEquals("Usage pattern local summary", success.data.insightSummaryText)
        assertEquals(InsightResolutionMode.LOCAL_REASONING, success.data.resolutionMode)
    }

    private fun reasoningResult(): BehaviorReasoningResult {
        return BehaviorReasoningResult(
            primaryHypothesis = null,
            secondaryHypotheses = emptyList(),
            recommendations = emptyList(),
            summary = "Local reasoning summary",
            llmContext = LlmGroundedContext(
                primaryPattern = "USAGE_PATTERN",
                secondaryPatterns = emptyList(),
                riskScore = 10,
                confidence = 0.5f,
                summary = "Grounded usage pattern",
                evidence = emptyList(),
                recommendations = emptyList()
            )
        )
    }

    private fun features(): UsageFeatures {
        val distribution = SessionLengthDistribution(
            shortSessionCount = 1,
            mediumSessionCount = 1,
            longSessionCount = 1,
            shortSessionRatio = 0.33f,
            mediumSessionRatio = 0.33f,
            longSessionRatio = 0.34f
        )
        return UsageFeatures(
            totalScreenTimeMillis = 180_000L,
            totalSessionCount = 3,
            longestSessionMillis = 120_000L,
            lateNightUsageMillis = 0L,
            lateNightSessionCount = 0,
            lateNightUsageRatio = 0f,
            lateNightAverageSessionLengthMillis = 0L,
            workHourDistractionMillis = 0L,
            morningUsageMillis = 0L,
            morningSessionCount = 0,
            switchCount = 2,
            switchesPerHour = 1f,
            averageSessionLengthMillis = 60_000L,
            averageSwitchIntervalMillis = 30_000L,
            peakUsageHour = 10,
            sessionLengthDistribution = distribution,
            topAppsByDuration = emptyList(),
            topAppsByLaunchCount = emptyList(),
            topCategoriesByDuration = emptyList(),
            lateNightTopApps = emptyList(),
            workHourTopApps = emptyList(),
            morningTopApps = emptyList()
        )
    }
}
