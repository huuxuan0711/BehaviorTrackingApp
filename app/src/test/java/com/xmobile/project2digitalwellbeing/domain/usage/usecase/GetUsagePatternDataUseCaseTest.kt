package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionEnricherImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractorImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetUsagePatternDataUseCaseTest {

    @Test
    fun `returns usage pattern data with top apps and top insight`() = runBlocking {
        val repository = FakePatternRepository(
            sessions = listOf(
                AppSession("app.a", 0L, 60_000L, 60_000L),
                AppSession("app.b", 90_000L, 240_000L, 150_000L),
                AppSession("app.a", 300_000L, 420_000L, 120_000L)
            ),
            insights = listOf(
                Insight(InsightType.BINGE_USAGE, 80, 0.9f, emptyMap(), listOf("app.b"))
            )
        )

        val useCase = GetUsagePatternDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            featureExtractor = UsageFeatureExtractorImpl()
        )

        val outcome = useCase(
            GetUsagePatternDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok",
                topAppsLimit = 1
            )
        )

        require(outcome is GetUsagePatternDataOutcome.Success)
        assertEquals(3, outcome.data.totalSessionCount)
        assertEquals(1, outcome.data.topAppsByLaunchCount.size)
        assertEquals("app.a", outcome.data.topAppsByLaunchCount.first().packageName)
        assertEquals(InsightType.BINGE_USAGE, outcome.data.topInsight?.type)
    }

    @Test
    fun `maps metadata read failure to data access error`() = runBlocking {
        val repository = FakePatternRepository(
            metadataError = UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.METADATA_CACHE,
                    cause = IllegalStateException("metadata failed")
                )
            )
        )

        val useCase = GetUsagePatternDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            featureExtractor = UsageFeatureExtractorImpl()
        )

        val outcome = useCase(
            GetUsagePatternDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        require(outcome is GetUsagePatternDataOutcome.Failure)
        assertTrue(outcome.error is UsagePatternDataError.DataAccessFailure)
        assertEquals(UsagePatternDataStage.READ_APP_METADATA, outcome.error.stage)
    }

    private class FakePatternRepository(
        private val sessions: List<AppSession> = emptyList(),
        private val insights: List<Insight> = emptyList(),
        private val metadataError: Throwable? = null
    ) : UsageRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long) = emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent>()

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            metadataError?.let { throw it }
            return packageNames.associateWith { packageName ->
                AppMetadata(
                    packageName = packageName,
                    appName = packageName,
                    sourceCategory = SourceAppCategory.UNKNOWN,
                    reportingCategory = AppCategory.UNKNOWN,
                    classificationSource = ClassificationSource.UNKNOWN,
                    confidence = 0f
                )
            }
        }

        override suspend fun getSessions(startTimeMillis: Long, endTimeMillis: Long): List<AppSession> = sessions
        override suspend fun saveSessions(sessions: List<AppSession>) = Unit
        override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit
        override suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long): List<Insight> = insights
        override suspend fun saveInsights(insights: List<Insight>, windowStartMillis: Long, windowEndMillis: Long) = Unit
        override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit
        override suspend fun getSyncState(): UsageSyncState = UsageSyncState(null, null, null, false)
        override suspend fun saveSyncState(state: UsageSyncState) = Unit
        override suspend fun commitRefreshResult(
            windowStartMillis: Long,
            windowEndMillis: Long,
            sessions: List<AppSession>,
            insights: List<Insight>,
            newSyncState: UsageSyncState
        ) = Unit
    }

    private class FakeUsagePreferencesRepository : UsagePreferencesRepository {
        override val usageAnalysisPreferences = flowOf(UsageAnalysisPreferences.DEFAULT)
        override suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences = UsageAnalysisPreferences.DEFAULT
        override suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) = Unit
    }
}
