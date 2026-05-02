package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageEventType
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.InsightEngine
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionBuilder
import com.xmobile.project2digitalwellbeing.domain.usage.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.CategoryUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.TopAppFeature
import com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution
import com.xmobile.project2digitalwellbeing.domain.usage.model.TopCategoryFeature
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsagePreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshUsageDataUseCaseTest {

    @Test
    fun `initial refresh uses 24 hour window and persists sessions`() = runBlocking {
        val nowMillis = 100_000L
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = listOf(
                AppUsageEvent("app.a", 90_000L, UsageEventType.FOREGROUND),
                AppUsageEvent("app.a", 95_000L, UsageEventType.BACKGROUND)
            )
        )
        val sessionBuilder = RecordingSessionBuilder(
            sessionsToReturn = listOf(
                AppSession("app.a", 90_000L, 95_000L, 5_000L)
            )
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = sessionBuilder,
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = nowMillis,
                timezoneId = "Asia/Bangkok"
            )
        )
        val result = assertSuccess(outcome)

        assertEquals(0L, result.processedRangeStartMillis)
        assertEquals(nowMillis, result.processedRangeEndMillis)
        assertEquals(RefreshMode.FULL, result.refreshMode)
        assertEquals("1970-01-01", result.currentLocalDate)
        assertEquals(2, result.eventsFetched)
        assertEquals(1, result.sessionsAffected)
        assertEquals(0L, repository.deletedSessionsStart)
        assertEquals(nowMillis, repository.deletedSessionsEnd)
        assertEquals(0L, sessionBuilder.lastRangeStartMillis)
        assertEquals(nowMillis, sessionBuilder.lastRangeEndMillis)
        assertEquals(1, repository.savedSessions.size)
        assertEquals(0, repository.savedInsights.size)
        assertEquals(nowMillis, repository.savedSyncState?.lastProcessedTimestampMillis)
        assertEquals(95_000L, repository.savedSyncState?.lastSeenEventTimestampMillis)
        assertEquals(true, repository.savedSyncState?.isInitialSyncCompleted)
    }

    @Test
    fun `incremental refresh reprocesses safety window`() = runBlocking {
        val nowMillis = 200_000L
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = 180_000L,
                lastSeenEventTimestampMillis = 179_000L,
                lastSuccessfulRefreshTimestampMillis = 180_000L,
                isInitialSyncCompleted = true
            ),
            usageEvents = emptyList()
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = nowMillis,
                timezoneId = "Asia/Bangkok"
            )
        )
        val result = assertSuccess(outcome)

        assertEquals(0L, result.processedRangeStartMillis)
        assertEquals(nowMillis, result.processedRangeEndMillis)
        assertEquals(RefreshMode.INCREMENTAL, result.refreshMode)
    }

    @Test
    fun `force full refresh ignores existing sync state`() = runBlocking {
        val nowMillis = 300_000L
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = 299_000L,
                lastSeenEventTimestampMillis = 299_000L,
                lastSuccessfulRefreshTimestampMillis = 299_000L,
                isInitialSyncCompleted = true
            ),
            usageEvents = emptyList()
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = nowMillis,
                timezoneId = "Asia/Bangkok",
                forceFullRefresh = true
            )
        )
        val result = assertSuccess(outcome)

        assertEquals(0L, result.processedRangeStartMillis)
    }

    @Test
    fun `returns permission error when usage event access fails`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = emptyList(),
            usageEventsError = SecurityException("no permission")
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        val error = assertFailure(outcome)
        assertTrue(error is UsageDataError.PermissionDenied)
        assertEquals(
            UsagePipelineStage.FETCH_USAGE_EVENTS,
            (error as UsageDataError.PermissionDenied).stage
        )
    }

    @Test
    fun `returns invalid timezone error when timezone is malformed`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = emptyList()
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Bad/Timezone"
            )
        )

        val error = assertFailure(outcome)
        assertTrue(error is UsageDataError.InvalidTimeZone)
        assertEquals(
            UsagePipelineStage.BUILD_DAILY_USAGE,
            (error as UsageDataError.InvalidTimeZone).stage
        )
    }

    @Test
    fun `returns persistence error when repository replace fails`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = emptyList(),
            replaceError = IllegalStateException("db failed")
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        val error = assertFailure(outcome)
        assertTrue(error is UsageDataError.PersistenceFailure)
    }

    @Test
    fun `maps structured data layer read errors into data access failure`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = emptyList(),
            usageEventsError = UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.USAGE_REPOSITORY,
                    cause = IllegalStateException("metadata cache read failed")
                )
            )
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        val outcome = useCase(
            RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        val error = assertFailure(outcome)
        assertTrue(error is UsageDataError.DataAccessFailure)
    }

    @Test
    fun `preserves last seen timestamp when no new events are returned`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = 50_000L,
                lastSeenEventTimestampMillis = 49_500L,
                lastSuccessfulRefreshTimestampMillis = 50_000L,
                isInitialSyncCompleted = true
            ),
            usageEvents = emptyList()
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        useCase(
            RefreshUsageDataParams(
                nowMillis = 60_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        assertEquals(49_500L, repository.savedSyncState?.lastSeenEventTimestampMillis)
    }

    @Test
    fun `keeps last seen timestamp null when there is still no event history`() = runBlocking {
        val repository = FakeUsageRepository(
            syncState = UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            ),
            usageEvents = emptyList()
        )

        val useCase = RefreshUsageDataUseCase(
            repository = repository,
            usagePreferencesRepository = FakeUsagePreferencesRepository(),
            refreshPolicy = DefaultRefreshPolicy,
            sessionEnricher = DefaultSessionEnricher,
            errorMapper = UsageErrorMapperImpl(),
            sessionBuilder = RecordingSessionBuilder(emptyList()),
            aggregator = NoOpUsageAggregator,
            featureExtractor = NoOpUsageFeatureExtractor,
            insightEngine = NoOpInsightEngine
        )

        useCase(
            RefreshUsageDataParams(
                nowMillis = 10_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        assertNull(repository.savedSyncState?.lastSeenEventTimestampMillis)
    }

    private class RecordingSessionBuilder(
        private val sessionsToReturn: List<AppSession>
    ) : SessionBuilder {
        var lastRangeStartMillis: Long? = null
        var lastRangeEndMillis: Long? = null

        override fun buildSessions(
            events: List<AppUsageEvent>,
            rangeStartMillis: Long,
            rangeEndMillis: Long,
            nowMillis: Long
        ): List<AppSession> {
            lastRangeStartMillis = rangeStartMillis
            lastRangeEndMillis = rangeEndMillis
            return sessionsToReturn
        }
    }

    private class FakeUsagePreferencesRepository(
        private val preferences: UsageAnalysisPreferences = UsageAnalysisPreferences.DEFAULT
    ) : UsagePreferencesRepository {
        override val usageAnalysisPreferences = flowOf(preferences)

        override suspend fun getUsageAnalysisPreferences(): UsageAnalysisPreferences = preferences

        override suspend fun saveUsageAnalysisPreferences(preferences: UsageAnalysisPreferences) = Unit
    }

    private object DefaultRefreshPolicy : UsageRefreshPolicy {
        private val delegate = UsageRefreshPolicyImpl()

        override fun resolveWindow(
            params: RefreshUsageDataParams,
            syncState: UsageSyncState
        ): UsageRefreshWindow = delegate.resolveWindow(params, syncState)
    }

    private object DefaultSessionEnricher : SessionEnricher {
        private val delegate = com.xmobile.project2digitalwellbeing.domain.usage.service.SessionEnricherImpl()

        override fun enrichSessions(
            sessions: List<AppSession>,
            timezoneId: String,
            appMetadataByPackage: Map<String, AppMetadata>,
            preferences: UsageAnalysisPreferences
        ): List<EnrichedSession> = delegate.enrichSessions(sessions, timezoneId, appMetadataByPackage, preferences)
    }

    private class FakeUsageRepository(
        private val syncState: UsageSyncState,
        private val usageEvents: List<AppUsageEvent>,
        private val usageEventsError: Throwable? = null,
        private val replaceError: Throwable? = null
    ) : UsageRepository {
        var deletedSessionsStart: Long? = null
        var deletedSessionsEnd: Long? = null
        val savedSessions = mutableListOf<AppSession>()
        val savedInsights = mutableListOf<Insight>()
        var savedSyncState: UsageSyncState? = null

        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long): List<AppUsageEvent> {
            usageEventsError?.let { throw it }
            return usageEvents
        }

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
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

        override suspend fun getSessions(startTimeMillis: Long, endTimeMillis: Long): List<AppSession> {
            return emptyList()
        }

        override suspend fun saveSessions(sessions: List<AppSession>) {
            savedSessions += sessions
        }

        override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) {
            deletedSessionsStart = startTimeMillis
            deletedSessionsEnd = endTimeMillis
        }

        override suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long): List<Insight> {
            return emptyList()
        }

        override suspend fun saveInsights(
            insights: List<Insight>,
            windowStartMillis: Long,
            windowEndMillis: Long
        ) = Unit

        override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit

        override suspend fun getSyncState(): UsageSyncState = syncState

        override suspend fun saveSyncState(state: UsageSyncState) {
            savedSyncState = state
        }

        override suspend fun commitRefreshResult(
            windowStartMillis: Long,
            windowEndMillis: Long,
            sessions: List<AppSession>,
            insights: List<Insight>,
            newSyncState: UsageSyncState
        ) {
            replaceError?.let { throw it }
            deletedSessionsStart = windowStartMillis
            deletedSessionsEnd = windowEndMillis
            savedSessions.clear()
            savedSessions += sessions
            savedInsights.clear()
            savedInsights += insights
            savedSyncState = newSyncState
        }
    }

    private object NoOpUsageAggregator : UsageAggregator {
        override fun buildDailyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            localDate: String
        ): DailyUsage {
            return DailyUsage(
                localDate = localDate,
                timezoneId = timezoneId,
                totalScreenTimeMillis = 0L,
                totalSessionCount = 0,
                sessions = emptyList()
            )
        }

        override fun buildAppUsageStats(
            sessions: List<AppSession>,
            appMetadataByPackage: Map<String, AppMetadata>
        ): List<AppUsageStat> = emptyList()

        override fun buildHourlyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            localDate: String?
        ): List<HourlyUsage> = emptyList()

        override fun buildWeeklyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            startLocalDate: String,
            endLocalDate: String
        ): WeeklyUsage {
            return WeeklyUsage(
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                timezoneId = timezoneId,
                totalScreenTimeMillis = 0L,
                averageDailyScreenTimeMillis = 0L,
                mostUsedLocalDate = null,
                dailyUsages = emptyList()
            )
        }

        override fun buildCategoryUsage(
            sessions: List<AppSession>,
            appMetadataByPackage: Map<String, AppMetadata>
        ): List<CategoryUsage> = emptyList()
    }

    private object NoOpUsageFeatureExtractor : UsageFeatureExtractor {
        override fun extractFeatures(
            sessions: List<com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession>,
            preferences: UsageAnalysisPreferences
        ): UsageFeatures {
            return UsageFeatures(
                totalScreenTimeMillis = 0L,
                totalSessionCount = 0,
                longestSessionMillis = 0L,
                lateNightUsageMillis = 0L,
                lateNightSessionCount = 0,
                lateNightUsageRatio = 0f,
                lateNightAverageSessionLengthMillis = 0L,
                switchCount = 0,
                switchesPerHour = 0f,
                averageSessionLengthMillis = 0L,
                averageSwitchIntervalMillis = 0L,
                peakUsageHour = null,
                sessionLengthDistribution = SessionLengthDistribution(
                    shortSessionCount = 0,
                    mediumSessionCount = 0,
                    longSessionCount = 0,
                    shortSessionRatio = 0f,
                    mediumSessionRatio = 0f,
                    longSessionRatio = 0f
                ),
                topAppsByDuration = emptyList<UsageFeatureTopApp>(),
                topAppsByLaunchCount = emptyList<UsageFeatureTopApp>(),
                topCategoriesByDuration = emptyList<TopCategoryFeature>(),
                lateNightTopApps = emptyList<UsageFeatureTopApp>()
            )
        }
    }

    private object NoOpInsightEngine : InsightEngine {
        override fun generateInsights(
            features: UsageFeatures,
            dailyUsage: DailyUsage,
            preferences: UsageAnalysisPreferences
        ): List<Insight> = emptyList()
    }

    private fun assertSuccess(outcome: RefreshUsageDataOutcome): RefreshUsageDataResult {
        require(outcome is RefreshUsageDataOutcome.Success) {
            "Expected success but got $outcome"
        }
        return outcome.result
    }

    private fun assertFailure(outcome: RefreshUsageDataOutcome): UsageDataError {
        require(outcome is RefreshUsageDataOutcome.Failure) {
            "Expected failure but got $outcome"
        }
        return outcome.error
    }
}
