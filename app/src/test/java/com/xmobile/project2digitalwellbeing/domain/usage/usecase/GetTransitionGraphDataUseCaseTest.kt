package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionEnricherImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.TransitionExtractorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.TransitionInsightGeneratorImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTransitionGraphDataUseCaseTest {

    @Test
    fun `returns filtered transitions and insight for today`() = runBlocking {
        val repository = FakeTransitionRepository(
            sessions = listOf(
                AppSession("app.youtube", 0L, 60_000L, 60_000L),
                AppSession("app.tiktok", 90_000L, 150_000L, 60_000L),
                AppSession("app.docs", 180_000L, 240_000L, 60_000L)
            )
        )

        val useCase = GetTransitionGraphDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeTransitionPreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            transitionExtractor = TransitionExtractorImpl(),
            transitionInsightGenerator = TransitionInsightGeneratorImpl()
        )

        val outcome = useCase(
            GetTransitionGraphDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok",
                timeRange = AnalysisTimeRange.TODAY,
                filter = TransitionFilter.DISTRACTING_MIXED
            )
        )

        require(outcome is GetTransitionGraphDataOutcome.Success)
        assertEquals(2, outcome.data.transitions.size)
        assertTrue(
            outcome.data.transitions.any {
                it.fromPackageName == "app.youtube" && it.toPackageName == "app.tiktok"
            }
        )
        assertEquals(TransitionFilter.DISTRACTING_MIXED, outcome.data.filter)
    }

    @Test
    fun `maps preference read failure to data access error`() = runBlocking {
        val useCase = GetTransitionGraphDataUseCase(
            repository = FakeTransitionRepository(),
            usagePreferencesRepository = FakeTransitionPreferencesRepository(
                error = UsageDataLayerException(
                    UsageDataLayerError.CacheReadFailed(
                        source = UsageDataLayerSource.SYNC_STATE_CACHE,
                        cause = IllegalStateException("preferences failed")
                    )
                )
            ),
            sessionEnricher = SessionEnricherImpl(),
            transitionExtractor = TransitionExtractorImpl(),
            transitionInsightGenerator = TransitionInsightGeneratorImpl()
        )

        val outcome = useCase(
            GetTransitionGraphDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok",
                timeRange = AnalysisTimeRange.WEEK,
                filter = TransitionFilter.ALL
            )
        )

        require(outcome is GetTransitionGraphDataOutcome.Failure)
        assertTrue(outcome.error is TransitionGraphDataError.DataAccessFailure)
        assertEquals(TransitionGraphDataStage.READ_PREFERENCES, outcome.error.stage)
    }

    private class FakeTransitionRepository(
        private val sessions: List<AppSession> = emptyList()
    ) : UsageRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long) = emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent>()

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            return packageNames.associateWith { packageName ->
                val category = when (packageName) {
                    "app.youtube" -> AppCategory.VIDEO
                    "app.tiktok" -> AppCategory.SOCIAL
                    "app.docs" -> AppCategory.PRODUCTIVITY
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
        override suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long) = emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.Insight>()
        override suspend fun saveInsights(insights: List<com.xmobile.project2digitalwellbeing.domain.usage.model.Insight>, windowStartMillis: Long, windowEndMillis: Long) = Unit
        override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit
        override suspend fun getSyncState(): UsageSyncState = UsageSyncState(null, null, null, false)
        override suspend fun saveSyncState(state: UsageSyncState) = Unit
        override suspend fun commitRefreshResult(
            windowStartMillis: Long,
            windowEndMillis: Long,
            sessions: List<AppSession>,
            insights: List<com.xmobile.project2digitalwellbeing.domain.usage.model.Insight>,
            newSyncState: UsageSyncState
        ) = Unit
    }

    private class FakeTransitionPreferencesRepository(
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
