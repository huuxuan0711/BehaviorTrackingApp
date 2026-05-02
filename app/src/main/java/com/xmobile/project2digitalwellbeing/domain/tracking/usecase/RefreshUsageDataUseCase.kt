package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightEngine
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionBuilder
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RefreshUsageDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val forceFullRefresh: Boolean = false
)

data class RefreshUsageDataResult(
    val refreshMode: RefreshMode,
    val processedRangeStartMillis: Long,
    val processedRangeEndMillis: Long,
    val currentLocalDate: String,
    val eventsFetched: Int,
    val sessionsAffected: Int,
    val insightsGenerated: Int
)

class RefreshUsageDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val refreshPolicy: UsageRefreshPolicy,
    private val sessionEnricher: SessionEnricher,
    private val errorMapper: UsageErrorMapper,
    private val sessionBuilder: SessionBuilder,
    private val aggregator: UsageAggregator,
    private val featureExtractor: UsageFeatureExtractor,
    private val insightEngine: InsightEngine
) {
    suspend operator fun invoke(params: RefreshUsageDataParams): RefreshUsageDataOutcome {
        val syncState = runStage(UsagePipelineStage.READ_SYNC_STATE, params) {
            repository.getSyncState()
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val refreshWindow = refreshPolicy.resolveWindow(
            params = params,
            syncState = syncState
        )

        val usageEvents = runStage(UsagePipelineStage.FETCH_USAGE_EVENTS, params) {
            repository.getUsageEvents(
                startTimeMillis = refreshWindow.startTimeMillis,
                endTimeMillis = refreshWindow.endTimeMillis
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val preferences = runStage(UsagePipelineStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val sessions = runStage(UsagePipelineStage.BUILD_SESSIONS, params) {
            withContext(Dispatchers.Default) {
                sessionBuilder.buildSessions(
                    events = usageEvents,
                    rangeStartMillis = refreshWindow.startTimeMillis,
                    rangeEndMillis = refreshWindow.endTimeMillis,
                    nowMillis = params.nowMillis
                )
            }
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val currentLocalDate = runStage(UsagePipelineStage.BUILD_DAILY_USAGE, params) {
            Instant.ofEpochMilli(params.nowMillis)
                .atZone(ZoneId.of(params.timezoneId))
                .toLocalDate()
                .toString()
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val dailyUsage = runStage(UsagePipelineStage.BUILD_DAILY_USAGE, params) {
            aggregator.buildDailyUsage(
                sessions = sessions,
                timezoneId = params.timezoneId,
                localDate = currentLocalDate
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val appMetadataByPackage = runStage(UsagePipelineStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(
                dailyUsage.sessions.map { it.packageName }.toSet()
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val enrichedSessions = runStage(UsagePipelineStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = dailyUsage.sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadataByPackage,
                preferences = preferences
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val features = runStage(UsagePipelineStage.EXTRACT_FEATURES, params) {
            withContext(Dispatchers.Default) {
                featureExtractor.extractFeatures(
                    sessions = enrichedSessions,
                    preferences = preferences
                )
            }
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val insights = runStage(UsagePipelineStage.GENERATE_INSIGHTS, params) {
            insightEngine.generateInsights(
                features = features,
                dailyUsage = dailyUsage,
                preferences = preferences
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        val newSyncState = buildNextSyncState(
            currentSyncState = syncState,
            usageEvents = usageEvents,
            refreshWindow = refreshWindow,
            nowMillis = params.nowMillis
        )

        runStage(UsagePipelineStage.PERSIST_RESULTS, params) {
            repository.commitRefreshResult(
                windowStartMillis = refreshWindow.startTimeMillis,
                windowEndMillis = refreshWindow.endTimeMillis,
                sessions = sessions,
                insights = insights,
                newSyncState = newSyncState
            )
        }.getOrElse { return RefreshUsageDataOutcome.Failure(it.toUsageDataError()) }

        return RefreshUsageDataOutcome.Success(
            RefreshUsageDataResult(
                refreshMode = refreshWindow.refreshMode,
                processedRangeStartMillis = refreshWindow.startTimeMillis,
                processedRangeEndMillis = refreshWindow.endTimeMillis,
                currentLocalDate = currentLocalDate,
                eventsFetched = usageEvents.size,
                sessionsAffected = sessions.size,
                insightsGenerated = insights.size
            )
        )
    }

    private fun buildNextSyncState(
        currentSyncState: UsageSyncState,
        usageEvents: List<com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent>,
        refreshWindow: UsageRefreshWindow,
        nowMillis: Long
    ): UsageSyncState {
        val lastSeenEventTimestampMillis = usageEvents
            .maxOfOrNull { it.timestampMillis }
            ?.let { latestTimestamp ->
                maxOf(latestTimestamp, currentSyncState.lastSeenEventTimestampMillis ?: latestTimestamp)
            }
            ?: currentSyncState.lastSeenEventTimestampMillis

        return UsageSyncState(
            lastProcessedTimestampMillis = refreshWindow.endTimeMillis,
            lastSeenEventTimestampMillis = lastSeenEventTimestampMillis,
            lastSuccessfulRefreshTimestampMillis = nowMillis,
            isInitialSyncCompleted = true
        )
    }

    private suspend fun <T> runStage(
        stage: UsagePipelineStage,
        params: RefreshUsageDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(UsageDataException(errorMapper.mapRefreshError(stage, params, throwable)))
        }
    }
}

class UsageDataException(
    val error: UsageDataError
) : RuntimeException(error.cause)

private fun Throwable.toUsageDataError(): UsageDataError {
    return (this as? UsageDataException)?.error
        ?: UsageDataError.UnknownFailure(
            stage = UsagePipelineStage.PERSIST_RESULTS,
            cause = this
        )
}
