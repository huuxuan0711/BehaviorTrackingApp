package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.service.InsightInterpreter
import com.xmobile.project2digitalwellbeing.domain.insights.service.InterpretedInsight
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.collections.map

data class GetDashboardDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val topAppsLimit: Int = 5,
    val isSlidingWindow: Boolean = false
)

data class DashboardData(
    val currentLocalDate: String,
    val dailyUsage: DailyUsage,
    val topInsight: InterpretedInsight?,
    val hourlyUsage: List<HourlyUsage>,
    val topApps: List<AppUsageStat>
)

sealed interface GetDashboardDataOutcome {
    data class Success(val data: DashboardData) : GetDashboardDataOutcome

    data class Failure(val error: DashboardDataError) : GetDashboardDataOutcome
}

sealed interface DashboardDataError {
    val stage: DashboardDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: DashboardDataStage,
        override val cause: Throwable?
    ) : DashboardDataError

    data class DataAccessFailure(
        override val stage: DashboardDataStage,
        override val cause: Throwable?
    ) : DashboardDataError

    data class ProcessingFailure(
        override val stage: DashboardDataStage,
        override val cause: Throwable?
    ) : DashboardDataError

    data class UnknownFailure(
        override val stage: DashboardDataStage,
        override val cause: Throwable?
    ) : DashboardDataError
}

enum class DashboardDataStage {
    RESOLVE_DATE,
    READ_SESSIONS,
    READ_INSIGHTS,
    READ_APP_METADATA,
    BUILD_DAILY_USAGE,
    BUILD_HOURLY_USAGE,
    BUILD_TOP_APPS
}

class GetDashboardDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val appRepository: AppRepository,
    private val aggregator: UsageAggregator,
    private val interpreter: InsightInterpreter
) {

    suspend operator fun invoke(params: GetDashboardDataParams): GetDashboardDataOutcome {
        val zoneId = runStage(DashboardDataStage.RESOLVE_DATE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val currentLocalDate = runStage(DashboardDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(params.nowMillis)
                .atZone(zoneId)
                .toLocalDate()
                .toString()
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val windowStartMillis = runStage(DashboardDataStage.RESOLVE_DATE, params) {
            if (params.isSlidingWindow) {
                // Căn chỉnh về đầu giờ hiện tại và lùi lại 23 tiếng để có tổng 24 khung giờ
                // Kết quả là index 23 luôn tương ứng với giờ hiện tại.
                Instant.ofEpochMilli(params.nowMillis)
                    .atZone(zoneId)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .minusHours(23)
                    .toInstant()
                    .toEpochMilli()
            } else {
                params.nowMillis - 24L * 60 * 60 * 1000
            }
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val windowEndMillis = runStage(DashboardDataStage.RESOLVE_DATE, params) {
            params.nowMillis
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val sessions = runStage(DashboardDataStage.READ_SESSIONS, params) {
            repository.getSessions(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val insights = runStage(DashboardDataStage.READ_INSIGHTS, params) {
            repository.getInsights(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val dailyUsage = runStage(DashboardDataStage.BUILD_DAILY_USAGE, params) {
            if (params.isSlidingWindow) {
                aggregator.buildSlidingUsage(
                    sessions = sessions,
                    timezoneId = params.timezoneId,
                    windowStartMillis = windowStartMillis,
                    windowEndMillis = windowEndMillis
                )
            } else {
                aggregator.buildDailyUsage(
                    sessions = sessions,
                    timezoneId = params.timezoneId,
                    localDate = currentLocalDate
                )
            }
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val appMetadataByPackage = runStage(DashboardDataStage.READ_APP_METADATA, params) {
            appRepository.getAppMetadata(dailyUsage.sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val hourlyUsage = runStage(DashboardDataStage.BUILD_HOURLY_USAGE, params) {
            if (params.isSlidingWindow) {
                aggregator.buildSlidingHourlyUsage(
                    sessions = sessions,
                    timezoneId = params.timezoneId,
                    windowStartMillis = windowStartMillis,
                    windowEndMillis = windowEndMillis
                )
            } else {
                aggregator.buildHourlyUsage(
                    sessions = dailyUsage.sessions,
                    timezoneId = params.timezoneId,
                    localDate = currentLocalDate
                )
            }
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val topApps = runStage(DashboardDataStage.BUILD_TOP_APPS, params) {
            aggregator.buildAppUsageStats(
                sessions = dailyUsage.sessions,
                appMetadataByPackage = appMetadataByPackage
            ).take(params.topAppsLimit)
        }.getOrElse { return GetDashboardDataOutcome.Failure(it.toDashboardError(params.timezoneId)) }

        val topInsight = insights.maxWithOrNull(
            compareBy<Insight> { it.score }
                .thenBy { it.confidence }
        )?.let(interpreter::interpret)

        return GetDashboardDataOutcome.Success(
            DashboardData(
                currentLocalDate = currentLocalDate,
                dailyUsage = dailyUsage,
                topInsight = topInsight,
                hourlyUsage = hourlyUsage,
                topApps = topApps
            )
        )
    }

    private suspend fun <T> runStage(
        stage: DashboardDataStage,
        params: GetDashboardDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(DashboardDataException(stage, throwable))
        }
    }
}

private data class DashboardDataException(
    val stageValue: DashboardDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toDashboardError(timezoneId: String): DashboardDataError {
    val dashboardException = this as? DashboardDataException
    val stage = dashboardException?.stageValue ?: DashboardDataStage.BUILD_DAILY_USAGE
    val cause = dashboardException?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> DashboardDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException -> {
            DashboardDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )
        }

        else -> when (stage) {
            DashboardDataStage.READ_SESSIONS,
            DashboardDataStage.READ_INSIGHTS,
            DashboardDataStage.READ_APP_METADATA -> DashboardDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )

            DashboardDataStage.RESOLVE_DATE,
            DashboardDataStage.BUILD_DAILY_USAGE,
            DashboardDataStage.BUILD_HOURLY_USAGE,
            DashboardDataStage.BUILD_TOP_APPS -> DashboardDataError.ProcessingFailure(
                stage = stage,
                cause = cause
            )
        }
    }
}
