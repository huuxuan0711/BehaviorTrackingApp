package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageTrendAnalyzer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetWeeklyOverviewDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val topAppsLimit: Int = 5
)

data class WeeklyOverviewData(
    val weeklyUsage: WeeklyUsage,
    val trend: UsageTrend,
    val topApps: List<AppUsageStat>
)

sealed interface GetWeeklyOverviewDataOutcome {
    data class Success(val data: WeeklyOverviewData) : GetWeeklyOverviewDataOutcome

    data class Failure(val error: WeeklyOverviewDataError) : GetWeeklyOverviewDataOutcome
}

sealed interface WeeklyOverviewDataError {
    val stage: WeeklyOverviewDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: WeeklyOverviewDataStage,
        override val cause: Throwable?
    ) : WeeklyOverviewDataError

    data class DataAccessFailure(
        override val stage: WeeklyOverviewDataStage,
        override val cause: Throwable?
    ) : WeeklyOverviewDataError

    data class ProcessingFailure(
        override val stage: WeeklyOverviewDataStage,
        override val cause: Throwable?
    ) : WeeklyOverviewDataError
}

enum class WeeklyOverviewDataStage {
    RESOLVE_WEEK,
    READ_CURRENT_WEEK_SESSIONS,
    READ_PREVIOUS_WEEK_SESSIONS,
    BUILD_CURRENT_WEEK_USAGE,
    BUILD_PREVIOUS_WEEK_USAGE,
    BUILD_TREND,
    READ_APP_METADATA,
    BUILD_TOP_APPS
}

class GetWeeklyOverviewDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val aggregator: UsageAggregator,
    private val trendAnalyzer: UsageTrendAnalyzer
) {

    suspend operator fun invoke(params: GetWeeklyOverviewDataParams): GetWeeklyOverviewDataOutcome {
        val zoneId = runStage(WeeklyOverviewDataStage.RESOLVE_WEEK, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val anchorDate = runStage(WeeklyOverviewDataStage.RESOLVE_WEEK, params) {
            Instant.ofEpochMilli(params.nowMillis)
                .atZone(zoneId)
                .toLocalDate()
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val currentWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val currentWeekEnd = currentWeekStart.plusDays(6)
        val previousWeekStart = currentWeekStart.minusDays(7)
        val previousWeekEnd = currentWeekStart.minusDays(1)

        val currentWeekSessions = runStage(WeeklyOverviewDataStage.READ_CURRENT_WEEK_SESSIONS, params) {
            repository.getSessions(
                currentWeekStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                currentWeekEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            )
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val previousWeekSessions = runStage(WeeklyOverviewDataStage.READ_PREVIOUS_WEEK_SESSIONS, params) {
            repository.getSessions(
                previousWeekStart.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                previousWeekEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            )
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val weeklyUsage = runStage(WeeklyOverviewDataStage.BUILD_CURRENT_WEEK_USAGE, params) {
            aggregator.buildWeeklyUsage(
                sessions = currentWeekSessions,
                timezoneId = params.timezoneId,
                startLocalDate = currentWeekStart.toString(),
                endLocalDate = currentWeekEnd.toString()
            )
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val previousWeeklyUsage = runStage(WeeklyOverviewDataStage.BUILD_PREVIOUS_WEEK_USAGE, params) {
            aggregator.buildWeeklyUsage(
                sessions = previousWeekSessions,
                timezoneId = params.timezoneId,
                startLocalDate = previousWeekStart.toString(),
                endLocalDate = previousWeekEnd.toString()
            )
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val trend = runStage(WeeklyOverviewDataStage.BUILD_TREND, params) {
            trendAnalyzer.compareWeekly(weeklyUsage, previousWeeklyUsage)
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val appMetadata = runStage(WeeklyOverviewDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(currentWeekSessions.map { it.packageName }.toSet())
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        val topApps = runStage(WeeklyOverviewDataStage.BUILD_TOP_APPS, params) {
            aggregator.buildAppUsageStats(
                sessions = currentWeekSessions,
                appMetadataByPackage = appMetadata
            ).take(params.topAppsLimit)
        }.getOrElse { return GetWeeklyOverviewDataOutcome.Failure(it.toWeeklyOverviewDataError(params.timezoneId)) }

        return GetWeeklyOverviewDataOutcome.Success(
            WeeklyOverviewData(
                weeklyUsage = weeklyUsage,
                trend = trend,
                topApps = topApps
            )
        )
    }

    private suspend fun <T> runStage(
        stage: WeeklyOverviewDataStage,
        params: GetWeeklyOverviewDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(WeeklyOverviewDataException(stage, throwable))
        }
    }
}

private data class WeeklyOverviewDataException(
    val stageValue: WeeklyOverviewDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toWeeklyOverviewDataError(timezoneId: String): WeeklyOverviewDataError {
    val weeklyException = this as? WeeklyOverviewDataException
    val stage = weeklyException?.stageValue ?: WeeklyOverviewDataStage.BUILD_CURRENT_WEEK_USAGE
    val cause = weeklyException?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> WeeklyOverviewDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException ->
            WeeklyOverviewDataError.DataAccessFailure(stage = stage, cause = cause)

        else -> when (stage) {
            WeeklyOverviewDataStage.READ_CURRENT_WEEK_SESSIONS,
            WeeklyOverviewDataStage.READ_PREVIOUS_WEEK_SESSIONS,
            WeeklyOverviewDataStage.READ_APP_METADATA ->
                WeeklyOverviewDataError.DataAccessFailure(stage = stage, cause = cause)

            WeeklyOverviewDataStage.RESOLVE_WEEK,
            WeeklyOverviewDataStage.BUILD_CURRENT_WEEK_USAGE,
            WeeklyOverviewDataStage.BUILD_PREVIOUS_WEEK_USAGE,
            WeeklyOverviewDataStage.BUILD_TREND,
            WeeklyOverviewDataStage.BUILD_TOP_APPS ->
                WeeklyOverviewDataError.ProcessingFailure(stage = stage, cause = cause)
        }
    }
}
