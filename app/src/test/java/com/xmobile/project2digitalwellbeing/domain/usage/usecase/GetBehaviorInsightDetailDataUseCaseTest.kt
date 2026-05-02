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
import com.xmobile.project2digitalwellbeing.domain.usage.service.InsightComposerImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionEnricherImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.TransitionExtractorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.TransitionInsightGeneratorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregatorImpl
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractorImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBehaviorInsightDetailDataUseCaseTest {

    @Test
    fun `returns behavior detail for requested raw insight type`() = runBlocking {
        val repository = FakeBehaviorRepository(
            sessions = listOf(
                AppSession("app.social", 0L, 60_000L, 60_000L),
                AppSession("app.video", 120_000L, 180_000L, 60_000L),
                AppSession("app.social", 240_000L, 300_000L, 60_000L)
            ),
            insights = listOf(
                Insight(InsightType.FREQUENT_SWITCHING, 70, 0.8f, emptyMap(), listOf("app.social", "app.video")),
                Insight(InsightType.BINGE_USAGE, 40, 0.5f, emptyMap(), listOf("app.video"))
            )
        )

        val useCase = GetBehaviorInsightDetailDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeBehaviorPreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            featureExtractor = UsageFeatureExtractorImpl(),
            aggregator = UsageAggregatorImpl(),
            transitionExtractor = TransitionExtractorImpl(),
            transitionInsightGenerator = TransitionInsightGeneratorImpl(),
            insightComposer = InsightComposerImpl()
        )

        val outcome = useCase(
            GetBehaviorInsightDetailDataParams(
                nowMillis = 1_000L,
                timezoneId = "UTC",
                insightType = InsightType.FREQUENT_SWITCHING
            )
        )

        require(outcome is GetBehaviorInsightDetailDataOutcome.Success)
        assertEquals(InsightType.FREQUENT_SWITCHING, outcome.data.sourceInsightType)
        assertEquals("Frequent Switching", outcome.data.title)
        assertEquals(2, outcome.data.relatedPackages.size)
    }

    @Test
    fun `maps metadata read failure to data access error`() = runBlocking {
        val useCase = GetBehaviorInsightDetailDataUseCase(
            repository = FakeBehaviorRepository(
                metadataError = UsageDataLayerException(
                    UsageDataLayerError.CacheReadFailed(
                        source = UsageDataLayerSource.METADATA_CACHE,
                        cause = IllegalStateException("metadata failed")
                    )
                )
            ),
            usagePreferencesRepository = FakeBehaviorPreferencesRepository(),
            sessionEnricher = SessionEnricherImpl(),
            featureExtractor = UsageFeatureExtractorImpl(),
            aggregator = UsageAggregatorImpl(),
            transitionExtractor = TransitionExtractorImpl(),
            transitionInsightGenerator = TransitionInsightGeneratorImpl(),
            insightComposer = InsightComposerImpl()
        )

        val outcome = useCase(
            GetBehaviorInsightDetailDataParams(
                nowMillis = 1_000L,
                timezoneId = "UTC"
            )
        )

        require(outcome is GetBehaviorInsightDetailDataOutcome.Failure)
        assertTrue(outcome.error is BehaviorInsightDetailDataError.DataAccessFailure)
        assertEquals(BehaviorInsightDetailDataStage.READ_APP_METADATA, outcome.error.stage)
    }

    private class FakeBehaviorRepository(
        private val sessions: List<AppSession> = emptyList(),
        private val insights: List<Insight> = emptyList(),
        private val metadataError: Throwable? = null
    ) : UsageRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long) =
            emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent>()

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            metadataError?.let { throw it }
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

    private class FakeBehaviorPreferencesRepository : UsagePreferencesRepository {
        override val usageAnalysisPreferences = flowOf(UsageAnalysisPreferences.DEFAULT)

        override suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences {
            return UsageAnalysisPreferences.DEFAULT
        }

        override suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) = Unit
    }
}
