package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
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
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.DashboardData
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetDashboardExperienceUseCaseTest {

    private val getDashboardDataUseCase: GetDashboardDataUseCase = mock()
    private val appRepository: AppRepository = mock()
    private val usagePreferencesRepository: UsagePreferencesRepository = mock()
    private val sessionEnricher: SessionEnricher = mock()
    private val featureExtractor: UsageFeatureExtractor = mock()
    private val getBehaviorReasoningUseCase: GetBehaviorReasoningUseCase = mock()
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase = mock()
    private val cloudSecretRepository: CloudSecretRepository = mock()
    private val networkStatusProvider: NetworkStatusProvider = mock()
    private val insightResolutionStrategy: InsightResolutionStrategy = mock()
    private val localInsightNarrator: LocalInsightNarrator = mock()

    private lateinit var useCase: GetDashboardExperienceUseCase

    @Before
    fun setup() {
        useCase = GetDashboardExperienceUseCase(
            getDashboardDataUseCase,
            appRepository,
            usagePreferencesRepository,
            sessionEnricher,
            featureExtractor,
            getBehaviorReasoningUseCase,
            generateCloudInsightTextUseCase,
            cloudSecretRepository,
            networkStatusProvider,
            insightResolutionStrategy,
            localInsightNarrator
        )
    }

    @Test
    fun `invoke processes success properly with local fallback`() = runBlocking {
        val params = GetDashboardDataParams(nowMillis = 0L, timezoneId = "UTC")

        val data = DashboardData(
            currentLocalDate = "",
            dailyUsage = DailyUsage("", "", 0L, 0, emptyList()),
            topInsight = null,
            hourlyUsage = emptyList(),
            topApps = emptyList()
        )

        whenever(getDashboardDataUseCase.invoke(params)).thenReturn(
            GetDashboardDataOutcome.Success(data)
        )

        whenever(usagePreferencesRepository.getUsageAnalysisPreferences()).thenReturn(
            UsageAnalysisPreferences.DEFAULT
        )

        // Emulate empty sessions list so behaviorReasoning is null
        whenever(sessionEnricher.enrichSessions(any(), any(), any(), any())).thenReturn(emptyList())

        whenever(insightResolutionStrategy.resolve(any())).thenReturn(
            InsightResolutionDecision(
                mode = InsightResolutionMode.FALLBACK_TO_LOCAL,
                useRuleInsight = true,
                useLocalReasoning = true,
                requestCloudEnhancement = false
            )
        )

        whenever(localInsightNarrator.narrate(any(), anyOrNull(), anyOrNull(), any())).thenReturn("Dashboard Local Narrator")

        val result = useCase(params)

        assertTrue(result is GetDashboardExperienceOutcome.Success)
        val success = result as GetDashboardExperienceOutcome.Success
        assertEquals("Dashboard Local Narrator", success.data.insightSummaryText)
        assertEquals(InsightResolutionMode.FALLBACK_TO_LOCAL, success.data.resolutionMode)
    }
}




