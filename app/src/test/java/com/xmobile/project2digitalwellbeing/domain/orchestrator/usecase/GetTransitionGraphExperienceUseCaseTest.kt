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
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.AnalysisTimeRange
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetTransitionGraphDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.TransitionGraphData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetTransitionGraphExperienceUseCaseTest {

    private val getTransitionGraphDataUseCase: GetTransitionGraphDataUseCase = mock()
    private val usagePreferencesRepository: UsagePreferencesRepository = mock()
    private val cloudSecretRepository: CloudSecretRepository = mock()
    private val networkStatusProvider: NetworkStatusProvider = mock()
    private val insightResolutionStrategy: InsightResolutionStrategy = mock()
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase = mock()
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase = mock()
    private val localInsightNarrator: LocalInsightNarrator = mock()
    private val insightInterpreter: InsightInterpreter = mock()

    private lateinit var useCase: GetTransitionGraphExperienceUseCase

    @Before
    fun setup() {
        useCase = GetTransitionGraphExperienceUseCase(
            getTransitionGraphDataUseCase,
            usagePreferencesRepository,
            cloudSecretRepository,
            networkStatusProvider,
            insightResolutionStrategy,
            getBehaviorReasoningUseCase,
            generateCloudInsightTextUseCase,
            localInsightNarrator,
            insightInterpreter,
        )
    }

    @Test
    fun `invoke processes success properly with local fallback`() = runBlocking {
        try {
            val params = GetTransitionGraphDataParams(
            nowMillis = 0L,
            timezoneId = "UTC",
            timeRange = AnalysisTimeRange.TODAY,
            filter = TransitionFilter.ALL
        )

        val emptyFeatures = UsageFeatures(
            totalScreenTimeMillis = 0,
            totalSessionCount = 0,
            longestSessionMillis = 0,
            lateNightUsageMillis = 0,
            lateNightSessionCount = 0,
            lateNightUsageRatio = 0f,
            lateNightAverageSessionLengthMillis = 0,
            workHourDistractionMillis = 0,
            morningUsageMillis = 0,
            morningSessionCount = 0,
            switchCount = 0,
            switchesPerHour = 0f,
            averageSessionLengthMillis = 0,
            averageSwitchIntervalMillis = 0,
            peakUsageHour = null,
            sessionLengthDistribution = com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution(0,0,0,0f,0f,0f),
            topAppsByDuration = emptyList(),
            topAppsByLaunchCount = emptyList(),
            topCategoriesByDuration = emptyList(),
            lateNightTopApps = emptyList(),
            workHourTopApps = emptyList(),
            morningTopApps = emptyList()
        )

        val graphData = TransitionGraphData(
            startLocalDate = "",
            endLocalDate = "",
            filter = params.filter,
            timeRange = params.timeRange,
            transitions = emptyList(),
            insight = null,
            features = emptyFeatures
        )

        whenever(getTransitionGraphDataUseCase.invoke(params)).thenReturn(
            GetTransitionGraphDataOutcome.Success(graphData)
        )

        whenever(usagePreferencesRepository.getUsageAnalysisPreferences()).thenReturn(
            UsageAnalysisPreferences.DEFAULT
        )
        
        whenever(getBehaviorReasoningUseCase.invoke(any())).thenReturn(
            BehaviorReasoningResult(
                primaryHypothesis = null,
                secondaryHypotheses = emptyList(),
                recommendations = emptyList(),
                summary = "",
                llmContext = LlmGroundedContext(
                    primaryPattern = null,
                    secondaryPatterns = emptyList(),
                    riskScore = 0,
                    confidence = 0f,
                    summary = "",
                    evidence = emptyList(),
                    recommendations = emptyList()
                )
            )
        )
        
        whenever(insightResolutionStrategy.resolve(any())).thenReturn(
            InsightResolutionDecision(
                mode = InsightResolutionMode.FALLBACK_TO_LOCAL,
                useRuleInsight = true,
                useLocalReasoning = true,
                requestCloudEnhancement = false
            )
        )
        
        whenever(localInsightNarrator.narrate(org.mockito.kotlin.any(), org.mockito.kotlin.anyOrNull(), org.mockito.kotlin.anyOrNull())).thenReturn("Local Narrator Text")

        val result = useCase(params)

        assertTrue(result is GetTransitionGraphExperienceOutcome.Success)
        val success = result as GetTransitionGraphExperienceOutcome.Success
        assertEquals("Local Narrator Text", success.data.insightSummaryText)
        assertEquals(InsightResolutionMode.FALLBACK_TO_LOCAL, success.data.resolutionMode)
        } catch (e: Throwable) {
            println(e.stackTraceToString())
            throw e
        }
    }
}
