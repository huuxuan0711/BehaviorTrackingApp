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
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregatorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractorImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetLateNightAnalysisDataUseCaseTest {

    @Test
    fun `returns late-night analysis data for overnight window`() = runBlocking {
        val repository = FakeLateNightRepository(
            sessions = listOf(
                AppSession("app.social", 0L, 60L * 60L * 1000L, 60L * 60L * 1000L),
                AppSession("app.video", 90L * 60L * 1000L, 150L * 60L * 1000L, 60L * 60L * 1000L)
            ),
            insights = listOf(
                Insight(InsightType.LATE_NIGHT_SWITCHING, 82, 0.9f, emptyMap(), listOf("app.social", "app.video"))
            )
        )

        val useCase = GetLateNightAnalysisDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeLateNightPreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            aggregator = UsageAggregatorImpl(),
            featureExtractor = UsageFeatureExtractorImpl()
        )

        val outcome = useCase(
            GetLateNightAnalysisDataParams(
                nowMillis = 4L * 60L * 60L * 1000L,
                timezoneId = "UTC",
                topAppsLimit = 1
            )
        )

        require(outcome is GetLateNightAnalysisDataOutcome.Success)
        assertEquals("1969-12-31", outcome.data.startLocalDate)
        assertEquals("1970-01-01", outcome.data.endLocalDate)
        assertEquals(2L * 60L * 60L * 1000L, outcome.data.totalScreenTimeMillis)
        assertEquals(2, outcome.data.totalSessionCount)
        assertEquals(1, outcome.data.topApps.size)
        assertEquals(InsightType.LATE_NIGHT_SWITCHING, outcome.data.insight?.type)
        assertEquals(0, outcome.data.peakUsageWindowStartHour)
    }

    @Test
    fun `maps preference read failure to data access error`() = runBlocking {
        val useCase = GetLateNightAnalysisDataUseCase(
            repository = FakeLateNightRepository(),
            usagePreferencesRepository = FakeLateNightPreferencesRepository(
                error = UsageDataLayerException(
                    UsageDataLayerError.CacheReadFailed(
                        source = UsageDataLayerSource.SYNC_STATE_CACHE,
                        cause = IllegalStateException("preferences failed")
                    )
                )
            ),
            sessionEnricher = SessionEnricherImpl(),
            aggregator = UsageAggregatorImpl(),
            featureExtractor = UsageFeatureExtractorImpl()
        )

        val outcome = useCase(
            GetLateNightAnalysisDataParams(
                nowMillis = 1_000L,
                timezoneId = "UTC"
            )
        )

        require(outcome is GetLateNightAnalysisDataOutcome.Failure)
        assertTrue(outcome.error is LateNightAnalysisDataError.DataAccessFailure)
        assertEquals(LateNightAnalysisDataStage.READ_PREFERENCES, outcome.error.stage)
    }

    private class FakeLateNightRepository(
        private val sessions: List<AppSession> = emptyList(),
        private val insights: List<Insight> = emptyList()
    ) : UsageRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long) =
            emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent>()

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            return packageNames.associateWith { packageName ->
                val category = when (packageName) {
                    "app.social" -> AppCategory.SOCIAL
                    "app.video" -> AppCategory.VIDEO
                    else -> AppCategory.UNKNOWN
                }
                AppMetadata(
                    packageName = packageName,
                    appName = packageName,
                    sourceCategory = SourceAppCategory.UNKNOWN,
                    reportingCategory = category,
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

    private class FakeLateNightPreferencesRepository(
        private val error: Throwable? = null
    ) : UsagePreferencesRepository {
        override val usageAnalysisPreferences = flowOf(UsageAnalysisPreferences.DEFAULT)

        override suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences {
            error?.let { throw it }
            return UsageAnalysisPreferences.DEFAULT
        }

        override suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) = Unit
    }
}
