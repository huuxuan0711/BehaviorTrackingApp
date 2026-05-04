package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import com.xmobile.project2digitalwellbeing.domain.insights.service.TransitionInsightGenerator
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.tracking.service.TransitionExtractor
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetSessionTimelineDataParams(
    val nowMillis: Long,
    val timezoneId: String
)

data class SessionTimelineData(
    val startLocalDate: String,
    val endLocalDate: String,
    val lateNightStartHour: Int,
    val sessions: List<EnrichedSession>,
    val insight: TransitionInsight?
)

sealed interface GetSessionTimelineDataOutcome {
    data class Success(val data: SessionTimelineData) : GetSessionTimelineDataOutcome
    data class Failure(val error: SessionTimelineDataError) : GetSessionTimelineDataOutcome
}

sealed interface SessionTimelineDataError {
    val stage: SessionTimelineDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: SessionTimelineDataStage,
        override val cause: Throwable?
    ) : SessionTimelineDataError

    data class DataAccessFailure(
        override val stage: SessionTimelineDataStage,
        override val cause: Throwable?
    ) : SessionTimelineDataError

    data class ProcessingFailure(
        override val stage: SessionTimelineDataStage,
        override val cause: Throwable?
    ) : SessionTimelineDataError
}

enum class SessionTimelineDataStage {
    RESOLVE_RANGE,
    READ_SESSIONS,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_TRANSITIONS,
    GENERATE_INSIGHT
}

class GetSessionTimelineDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val transitionExtractor: TransitionExtractor,
    private val transitionInsightGenerator: TransitionInsightGenerator
) {

    suspend operator fun invoke(params: GetSessionTimelineDataParams): GetSessionTimelineDataOutcome {
        val zoneId = runStage(SessionTimelineDataStage.RESOLVE_RANGE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val endDate = runStage(SessionTimelineDataStage.RESOLVE_RANGE, params) {
            Instant.ofEpochMilli(params.nowMillis).atZone(zoneId).toLocalDate()
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val sessions = runStage(SessionTimelineDataStage.READ_SESSIONS, params) {
            repository.getSessions(startMillis, endMillis)
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val preferences = runStage(SessionTimelineDataStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val appMetadata = runStage(SessionTimelineDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val enrichedSessions = runStage(SessionTimelineDataStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadata,
                preferences = preferences
            ).sortedBy { it.session.startTimeMillis }
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val transitions = runStage(SessionTimelineDataStage.EXTRACT_TRANSITIONS, params) {
            transitionExtractor.extractTransitions(enrichedSessions)
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        val insight = runStage(SessionTimelineDataStage.GENERATE_INSIGHT, params) {
            transitionInsightGenerator.generateInsight(transitions, TransitionFilter.ALL)
        }.getOrElse { return GetSessionTimelineDataOutcome.Failure(it.toSessionTimelineDataError(params.timezoneId)) }

        return GetSessionTimelineDataOutcome.Success(
            SessionTimelineData(
                startLocalDate = endDate.toString(),
                endLocalDate = endDate.toString(),
                lateNightStartHour = preferences.lateNightStartHour,
                sessions = enrichedSessions,
                insight = insight
            )
        )
    }

    private suspend fun <T> runStage(
        stage: SessionTimelineDataStage,
        params: GetSessionTimelineDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(SessionTimelineDataException(stage, throwable))
        }
    }
}

private data class SessionTimelineDataException(
    val stageValue: SessionTimelineDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toSessionTimelineDataError(timezoneId: String): SessionTimelineDataError {
    val wrapped = this as? SessionTimelineDataException
    val stage = wrapped?.stageValue ?: SessionTimelineDataStage.GENERATE_INSIGHT
    val cause = wrapped?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> SessionTimelineDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException ->
            SessionTimelineDataError.DataAccessFailure(stage = stage, cause = cause)

        else -> when (stage) {
            SessionTimelineDataStage.READ_SESSIONS,
            SessionTimelineDataStage.READ_PREFERENCES,
            SessionTimelineDataStage.READ_APP_METADATA ->
                SessionTimelineDataError.DataAccessFailure(stage = stage, cause = cause)

            SessionTimelineDataStage.RESOLVE_RANGE,
            SessionTimelineDataStage.ENRICH_SESSIONS,
            SessionTimelineDataStage.EXTRACT_TRANSITIONS,
            SessionTimelineDataStage.GENERATE_INSIGHT ->
                SessionTimelineDataError.ProcessingFailure(stage = stage, cause = cause)
        }
    }
}
