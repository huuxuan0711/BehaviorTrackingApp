package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.insights.model.TransitionInsight
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.tracking.service.TransitionExtractor
import com.xmobile.project2digitalwellbeing.domain.insights.service.TransitionInsightGenerator
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetTransitionGraphDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val timeRange: AnalysisTimeRange,
    val filter: TransitionFilter
)

data class TransitionGraphData(
    val startLocalDate: String,
    val endLocalDate: String,
    val filter: TransitionFilter,
    val timeRange: AnalysisTimeRange,
    val transitions: List<AppTransitionStat>,
    val insight: TransitionInsight?
)

sealed interface GetTransitionGraphDataOutcome {
    data class Success(val data: TransitionGraphData) : GetTransitionGraphDataOutcome

    data class Failure(val error: TransitionGraphDataError) : GetTransitionGraphDataOutcome
}

sealed interface TransitionGraphDataError {
    val stage: TransitionGraphDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: TransitionGraphDataStage,
        override val cause: Throwable?
    ) : TransitionGraphDataError

    data class DataAccessFailure(
        override val stage: TransitionGraphDataStage,
        override val cause: Throwable?
    ) : TransitionGraphDataError

    data class ProcessingFailure(
        override val stage: TransitionGraphDataStage,
        override val cause: Throwable?
    ) : TransitionGraphDataError
}

enum class TransitionGraphDataStage {
    RESOLVE_RANGE,
    READ_SESSIONS,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_TRANSITIONS,
    FILTER_TRANSITIONS,
    GENERATE_INSIGHT
}

class GetTransitionGraphDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val transitionExtractor: TransitionExtractor,
    private val transitionInsightGenerator: TransitionInsightGenerator
) {
    suspend operator fun invoke(params: GetTransitionGraphDataParams): GetTransitionGraphDataOutcome {
        val zoneId = runStage(TransitionGraphDataStage.RESOLVE_RANGE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val range = runStage(TransitionGraphDataStage.RESOLVE_RANGE, params) {
            resolveWindow(params, zoneId)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val sessions = runStage(TransitionGraphDataStage.READ_SESSIONS, params) {
            repository.getSessions(range.startTimeMillis, range.endTimeMillis)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val preferences = runStage(TransitionGraphDataStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val appMetadataByPackage = runStage(TransitionGraphDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val enrichedSessions = runStage(TransitionGraphDataStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadataByPackage,
                preferences = preferences
            )
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val extractedTransitions = runStage(TransitionGraphDataStage.EXTRACT_TRANSITIONS, params) {
            transitionExtractor.extractTransitions(enrichedSessions)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val filteredTransitions = runStage(TransitionGraphDataStage.FILTER_TRANSITIONS, params) {
            transitionInsightGenerator.filterTransitions(extractedTransitions, params.filter)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        val insight = runStage(TransitionGraphDataStage.GENERATE_INSIGHT, params) {
            transitionInsightGenerator.generateInsight(extractedTransitions, params.filter)
        }.getOrElse { return GetTransitionGraphDataOutcome.Failure(it.toTransitionGraphError(params.timezoneId)) }

        return GetTransitionGraphDataOutcome.Success(
            TransitionGraphData(
                startLocalDate = range.startLocalDate,
                endLocalDate = range.endLocalDate,
                filter = params.filter,
                timeRange = params.timeRange,
                transitions = filteredTransitions,
                insight = insight
            )
        )
    }

    private fun resolveWindow(
        params: GetTransitionGraphDataParams,
        zoneId: ZoneId
    ): GraphTimeWindow {
        val today = Instant.ofEpochMilli(params.nowMillis).atZone(zoneId).toLocalDate()
        val startDate = when (params.timeRange) {
            AnalysisTimeRange.TODAY -> today
            AnalysisTimeRange.WEEK -> today.minusDays(6)
        }
        val endDateExclusive = today.plusDays(1)

        return GraphTimeWindow(
            startTimeMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endTimeMillis = endDateExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            startLocalDate = startDate.toString(),
            endLocalDate = today.toString()
        )
    }

    private suspend fun <T> runStage(
        stage: TransitionGraphDataStage,
        params: GetTransitionGraphDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(TransitionGraphDataException(stage, throwable))
        }
    }
}

private data class GraphTimeWindow(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val startLocalDate: String,
    val endLocalDate: String
)

private data class TransitionGraphDataException(
    val stageValue: TransitionGraphDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toTransitionGraphError(timezoneId: String): TransitionGraphDataError {
    val wrapped = this as? TransitionGraphDataException
    val stage = wrapped?.stageValue ?: TransitionGraphDataStage.GENERATE_INSIGHT
    val cause = wrapped?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> TransitionGraphDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException -> {
            TransitionGraphDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )
        }

        else -> when (stage) {
            TransitionGraphDataStage.READ_SESSIONS,
            TransitionGraphDataStage.READ_PREFERENCES,
            TransitionGraphDataStage.READ_APP_METADATA -> TransitionGraphDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )

            TransitionGraphDataStage.RESOLVE_RANGE,
            TransitionGraphDataStage.ENRICH_SESSIONS,
            TransitionGraphDataStage.EXTRACT_TRANSITIONS,
            TransitionGraphDataStage.FILTER_TRANSITIONS,
            TransitionGraphDataStage.GENERATE_INSIGHT -> TransitionGraphDataError.ProcessingFailure(
                stage = stage,
                cause = cause
            )
        }
    }
}
