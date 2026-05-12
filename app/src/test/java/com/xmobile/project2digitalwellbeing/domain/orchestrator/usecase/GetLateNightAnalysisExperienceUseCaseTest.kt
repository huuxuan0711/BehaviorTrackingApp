package com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase

import com.xmobile.project2digitalwellbeing.domain.network.NetworkStatusProvider
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionDecision
import com.xmobile.project2digitalwellbeing.domain.reasoning.model.InsightResolutionMode
import com.xmobile.project2digitalwellbeing.domain.reasoning.repository.CloudSecretRepository
import com.xmobile.project2digitalwellbeing.domain.reasoning.service.InsightResolutionStrategy
import com.xmobile.project2digitalwellbeing.domain.reasoning.usecase.GenerateCloudInsightTextUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetLateNightAnalysisDataUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.LateNightAnalysisData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetLateNightAnalysisExperienceUseCaseTest {

    private val getLateNightAnalysisDataUseCase: GetLateNightAnalysisDataUseCase = mock()
    private val usagePreferencesRepository: UsagePreferencesRepository = mock()
    private val cloudSecretRepository: CloudSecretRepository = mock()
    private val networkStatusProvider: NetworkStatusProvider = mock()
    private val insightResolutionStrategy: InsightResolutionStrategy = mock()
    private val generateCloudInsightTextUseCase: GenerateCloudInsightTextUseCase = mock()

    private lateinit var useCase: GetLateNightAnalysisExperienceUseCase

    @Before
    fun setup() {
        useCase = GetLateNightAnalysisExperienceUseCase(
            getLateNightAnalysisDataUseCase,
            usagePreferencesRepository,
            cloudSecretRepository,
            networkStatusProvider,
            insightResolutionStrategy,
            generateCloudInsightTextUseCase,
        )
    }

    @Test
    fun `invoke processes success properly with local fallback`() = runBlocking {
        val params = GetLateNightAnalysisDataParams(nowMillis = 0L, timezoneId = "UTC", topAppsLimit = 5)

        val data = LateNightAnalysisData(
            startLocalDate = "",
            endLocalDate = "",
            totalScreenTimeMillis = 0L,
            totalSessionCount = 0,
            averageSessionLengthMillis = 0L,
            hourlyUsage = emptyList(),
            peakUsageWindowStartHour = 0,
            peakUsageWindowEndHour = 0,
            topApps = emptyList(),
            insight = null,
            insightSummary = "Local Summary",
            recommendation = "Local Recommendation"
        )

        whenever(getLateNightAnalysisDataUseCase.invoke(params)).thenReturn(
            GetLateNightAnalysisDataOutcome.Success(data)
        )

        whenever(usagePreferencesRepository.getUsageAnalysisPreferences()).thenReturn(
            UsageAnalysisPreferences.DEFAULT
        )

        whenever(insightResolutionStrategy.resolve(any())).thenReturn(
            InsightResolutionDecision(
                mode = InsightResolutionMode.FALLBACK_TO_LOCAL,
                useRuleInsight = true,
                useLocalReasoning = true,
                requestCloudEnhancement = false
            )
        )

        val result = useCase(params)

        assertTrue(result is GetLateNightAnalysisExperienceOutcome.Success)
        val success = result as GetLateNightAnalysisExperienceOutcome.Success
        assertEquals("Local Summary", success.data.insightSummaryText)
        assertEquals(InsightResolutionMode.FALLBACK_TO_LOCAL, success.data.resolutionMode)
    }
}


