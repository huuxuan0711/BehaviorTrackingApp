package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetLateNightAnalysisDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val topAppsLimit: Int = 5
)

data class LateNightAnalysisData(
    val startLocalDate: String,
    val endLocalDate: String,
    val totalScreenTimeMillis: Long,
    val totalSessionCount: Int,
    val averageSessionLengthMillis: Long,
    val hourlyUsage: List<HourlyUsage>,
    val peakUsageWindowStartHour: Int?,
    val peakUsageWindowEndHour: Int?,
    val topApps: List<UsageFeatureTopApp>,
    val insight: Insight?,
    val insightSummary: String,
    val recommendation: String
)

sealed interface GetLateNightAnalysisDataOutcome {
    data class Success(val data: LateNightAnalysisData) : GetLateNightAnalysisDataOutcome

    data class Failure(val error: LateNightAnalysisDataError) : GetLateNightAnalysisDataOutcome
}

sealed interface LateNightAnalysisDataError {
    val stage: LateNightAnalysisDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: LateNightAnalysisDataStage,
        override val cause: Throwable?
    ) : LateNightAnalysisDataError

    data class DataAccessFailure(
        override val stage: LateNightAnalysisDataStage,
        override val cause: Throwable?
    ) : LateNightAnalysisDataError

    data class ProcessingFailure(
        override val stage: LateNightAnalysisDataStage,
        override val cause: Throwable?
    ) : LateNightAnalysisDataError
}

enum class LateNightAnalysisDataStage {
    RESOLVE_RANGE,
    READ_SESSIONS,
    READ_INSIGHTS,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_FEATURES,
    BUILD_HOURLY_USAGE
}

class GetLateNightAnalysisDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val aggregator: UsageAggregator,
    private val featureExtractor: UsageFeatureExtractor
) {
    suspend operator fun invoke(params: GetLateNightAnalysisDataParams): GetLateNightAnalysisDataOutcome {
        val zoneId = runStage(LateNightAnalysisDataStage.RESOLVE_RANGE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val preferences = runStage(LateNightAnalysisDataStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val range = runStage(LateNightAnalysisDataStage.RESOLVE_RANGE, params) {
            resolveLateNightWindow(
                nowMillis = params.nowMillis,
                zoneId = zoneId,
                preferences = preferences
            )
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val sessions = runStage(LateNightAnalysisDataStage.READ_SESSIONS, params) {
            repository.getSessions(range.startTimeMillis, range.endTimeMillis)
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val dayInsightRangeStart = runStage(LateNightAnalysisDataStage.RESOLVE_RANGE, params) {
            range.endLocalDate
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val dayInsightRangeEnd = runStage(LateNightAnalysisDataStage.RESOLVE_RANGE, params) {
            range.endLocalDate
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val insights = runStage(LateNightAnalysisDataStage.READ_INSIGHTS, params) {
            repository.getInsights(dayInsightRangeStart, dayInsightRangeEnd)
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val appMetadataByPackage = runStage(LateNightAnalysisDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val enrichedSessions = runStage(LateNightAnalysisDataStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadataByPackage,
                preferences = preferences
            )
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val features = runStage(LateNightAnalysisDataStage.EXTRACT_FEATURES, params) {
            featureExtractor.extractFeatures(
                sessions = enrichedSessions,
                preferences = preferences
            )
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val hourlyUsage = runStage(LateNightAnalysisDataStage.BUILD_HOURLY_USAGE, params) {
            aggregator.buildHourlyUsage(
                sessions = sessions,
                timezoneId = params.timezoneId,
                localDate = null
            ).filter { it.hourOfDay in range.lateNightHours }
        }.getOrElse { return GetLateNightAnalysisDataOutcome.Failure(it.toLateNightAnalysisError(params.timezoneId)) }

        val peakUsageHour = hourlyUsage
            .maxByOrNull { it.totalTimeMillis }
            ?.takeIf { it.totalTimeMillis > 0L }
            ?.hourOfDay

        val lateNightInsight = insights
            .filter { it.type == InsightType.LATE_NIGHT_SWITCHING || it.type == InsightType.LATE_NIGHT_USAGE }
            .maxWithOrNull(compareBy<Insight> { it.score }.thenBy { it.confidence })

        return GetLateNightAnalysisDataOutcome.Success(
            LateNightAnalysisData(
                startLocalDate = range.startLocalDate.toString(),
                endLocalDate = range.endLocalDate.toString(),
                totalScreenTimeMillis = features.lateNightUsageMillis,
                totalSessionCount = features.lateNightSessionCount,
                averageSessionLengthMillis = features.lateNightAverageSessionLengthMillis,
                hourlyUsage = hourlyUsage,
                peakUsageWindowStartHour = peakUsageHour,
                peakUsageWindowEndHour = peakUsageHour?.let { (it + 1) % 24 },
                topApps = features.lateNightTopApps.take(params.topAppsLimit),
                insight = lateNightInsight,
                insightSummary = buildInsightSummary(lateNightInsight, features, preferences),
                recommendation = buildRecommendation(lateNightInsight, features, preferences)
            )
        )
    }

    private fun resolveLateNightWindow(
        nowMillis: Long,
        zoneId: ZoneId,
        preferences: UsageAnalysisPreferences
    ): LateNightWindow {
        val localNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val endHour = UsageAnalysisPreferences.DEFAULT_LATE_NIGHT_END_HOUR
        val startDate = when {
            localNow.hour < endHour -> localNow.toLocalDate().minusDays(1)
            localNow.hour >= preferences.lateNightStartHour -> localNow.toLocalDate()
            else -> localNow.toLocalDate().minusDays(1)
        }
        val endDate = startDate.plusDays(1)
        val startDateTime = startDate.atTime(preferences.lateNightStartHour, 0).atZone(zoneId)
        val endDateTime = endDate.atTime(endHour, 0).atZone(zoneId)

        return LateNightWindow(
            startTimeMillis = startDateTime.toInstant().toEpochMilli(),
            endTimeMillis = endDateTime.toInstant().toEpochMilli(),
            startLocalDate = startDate,
            endLocalDate = endDate,
            lateNightHours = buildLateNightHours(preferences.lateNightStartHour, endHour)
        )
    }

    private fun buildLateNightHours(startHour: Int, endHour: Int): Set<Int> {
        return if (startHour <= endHour) {
            (startHour until endHour).toSet()
        } else {
            ((startHour..23) + (0 until endHour)).toSet()
        }
    }

    private fun buildInsightSummary(
        insight: Insight?,
        features: com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): String {
        return when (insight?.type) {
            InsightType.LATE_NIGHT_SWITCHING ->
                "Your late-night usage is fragmented, with frequent switching after ${formatHour(preferences.lateNightStartHour)}."

            InsightType.LATE_NIGHT_USAGE ->
                "A large share of your usage happens after ${formatHour(preferences.lateNightStartHour)}, which can disrupt sleep routines."

            else -> if (features.lateNightUsageMillis > 0L) {
                "Most of tonight's usage happened after ${formatHour(preferences.lateNightStartHour)}."
            } else {
                "No meaningful late-night usage was detected in the latest analysis window."
            }
        }
    }

    private fun buildRecommendation(
        insight: Insight?,
        features: com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatures,
        preferences: UsageAnalysisPreferences
    ): String {
        return when (insight?.type) {
            InsightType.LATE_NIGHT_SWITCHING ->
                "Reduce rapid app switching after ${formatHour(preferences.lateNightStartHour)} by setting a cutoff for social and video apps."

            InsightType.LATE_NIGHT_USAGE ->
                "Try to reduce screen exposure after ${formatHour(preferences.lateNightStartHour)} to protect sleep continuity."

            else -> if (features.lateNightUsageMillis >= 60L * 60L * 1000L) {
                "A fixed shutdown time before sleep would likely reduce this late-night screen block."
            } else {
                "Current late-night usage is limited. Keep the same boundary if this matches your target."
            }
        }
    }

    private fun formatHour(hourOfDay: Int): String {
        val normalized = ((hourOfDay % 24) + 24) % 24
        val period = if (normalized < 12) "AM" else "PM"
        val displayHour = when (val hour = normalized % 12) {
            0 -> 12
            else -> hour
        }
        return "$displayHour $period"
    }

    private suspend fun <T> runStage(
        stage: LateNightAnalysisDataStage,
        params: GetLateNightAnalysisDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(LateNightAnalysisDataException(stage, throwable))
        }
    }
}

private data class LateNightWindow(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val startLocalDate: java.time.LocalDate,
    val endLocalDate: java.time.LocalDate,
    val lateNightHours: Set<Int>
)

private data class LateNightAnalysisDataException(
    val stageValue: LateNightAnalysisDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toLateNightAnalysisError(timezoneId: String): LateNightAnalysisDataError {
    val wrapped = this as? LateNightAnalysisDataException
    val stage = wrapped?.stageValue ?: LateNightAnalysisDataStage.BUILD_HOURLY_USAGE
    val cause = wrapped?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> LateNightAnalysisDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException -> {
            LateNightAnalysisDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )
        }

        else -> when (stage) {
            LateNightAnalysisDataStage.READ_SESSIONS,
            LateNightAnalysisDataStage.READ_INSIGHTS,
            LateNightAnalysisDataStage.READ_PREFERENCES,
            LateNightAnalysisDataStage.READ_APP_METADATA -> LateNightAnalysisDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )

            LateNightAnalysisDataStage.RESOLVE_RANGE,
            LateNightAnalysisDataStage.ENRICH_SESSIONS,
            LateNightAnalysisDataStage.EXTRACT_FEATURES,
            LateNightAnalysisDataStage.BUILD_HOURLY_USAGE -> LateNightAnalysisDataError.ProcessingFailure(
                stage = stage,
                cause = cause
            )
        }
    }
}
