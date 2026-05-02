package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.SessionLengthDistribution
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageFeatureTopApp
import com.xmobile.project2digitalwellbeing.domain.preferences.repository.UsagePreferencesRepository
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.tracking.service.SessionEnricher
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageFeatureExtractor
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class GetUsagePatternDataParams(
    val nowMillis: Long,
    val timezoneId: String,
    val topAppsLimit: Int = 5
)

data class UsagePatternData(
    val currentLocalDate: String,
    val totalSessionCount: Int,
    val averageSessionLengthMillis: Long,
    val longestSessionMillis: Long,
    val switchCount: Int,
    val averageSwitchIntervalMillis: Long,
    val sessionLengthDistribution: SessionLengthDistribution,
    val topAppsByLaunchCount: List<UsageFeatureTopApp>,
    val topInsight: Insight?
)

sealed interface GetUsagePatternDataOutcome {
    data class Success(val data: UsagePatternData) : GetUsagePatternDataOutcome

    data class Failure(val error: UsagePatternDataError) : GetUsagePatternDataOutcome
}

sealed interface UsagePatternDataError {
    val stage: UsagePatternDataStage
    val cause: Throwable?

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: UsagePatternDataStage,
        override val cause: Throwable?
    ) : UsagePatternDataError

    data class DataAccessFailure(
        override val stage: UsagePatternDataStage,
        override val cause: Throwable?
    ) : UsagePatternDataError

    data class ProcessingFailure(
        override val stage: UsagePatternDataStage,
        override val cause: Throwable?
    ) : UsagePatternDataError
}

enum class UsagePatternDataStage {
    RESOLVE_DATE,
    READ_SESSIONS,
    READ_INSIGHTS,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_FEATURES
}

class GetUsagePatternDataUseCase @Inject constructor(
    private val repository: UsageRepository,
    private val usagePreferencesRepository: UsagePreferencesRepository,
    private val sessionEnricher: SessionEnricher,
    private val featureExtractor: UsageFeatureExtractor
) {
    suspend operator fun invoke(params: GetUsagePatternDataParams): GetUsagePatternDataOutcome {
        val zoneId = runStage(UsagePatternDataStage.RESOLVE_DATE, params) {
            ZoneId.of(params.timezoneId)
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val currentLocalDate = runStage(UsagePatternDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(params.nowMillis).atZone(zoneId).toLocalDate().toString()
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val windowStartMillis = runStage(UsagePatternDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(params.nowMillis)
                .atZone(zoneId)
                .toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val windowEndMillis = runStage(UsagePatternDataStage.RESOLVE_DATE, params) {
            Instant.ofEpochMilli(windowStartMillis)
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val sessions = runStage(UsagePatternDataStage.READ_SESSIONS, params) {
            repository.getSessions(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val insights = runStage(UsagePatternDataStage.READ_INSIGHTS, params) {
            repository.getInsights(windowStartMillis, windowEndMillis)
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val preferences = runStage(UsagePatternDataStage.READ_PREFERENCES, params) {
            usagePreferencesRepository.getUsageAnalysisPreferences()
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val appMetadataByPackage = runStage(UsagePatternDataStage.READ_APP_METADATA, params) {
            repository.getAppMetadata(sessions.map { it.packageName }.toSet())
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val enrichedSessions = runStage(UsagePatternDataStage.ENRICH_SESSIONS, params) {
            sessionEnricher.enrichSessions(
                sessions = sessions,
                timezoneId = params.timezoneId,
                appMetadataByPackage = appMetadataByPackage,
                preferences = preferences
            )
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        val features = runStage(UsagePatternDataStage.EXTRACT_FEATURES, params) {
            featureExtractor.extractFeatures(
                sessions = enrichedSessions,
                preferences = preferences
            )
        }.getOrElse { return GetUsagePatternDataOutcome.Failure(it.toUsagePatternError(params.timezoneId)) }

        return GetUsagePatternDataOutcome.Success(
            UsagePatternData(
                currentLocalDate = currentLocalDate,
                totalSessionCount = features.totalSessionCount,
                averageSessionLengthMillis = features.averageSessionLengthMillis,
                longestSessionMillis = features.longestSessionMillis,
                switchCount = features.switchCount,
                averageSwitchIntervalMillis = features.averageSwitchIntervalMillis,
                sessionLengthDistribution = features.sessionLengthDistribution,
                topAppsByLaunchCount = features.topAppsByLaunchCount.take(params.topAppsLimit),
                topInsight = insights.maxWithOrNull(
                    compareBy<Insight> { it.score }
                        .thenBy { it.confidence }
                )
            )
        )
    }

    private suspend fun <T> runStage(
        stage: UsagePatternDataStage,
        params: GetUsagePatternDataParams,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            Result.failure(UsagePatternDataException(stage, throwable))
        }
    }
}

private data class UsagePatternDataException(
    val stageValue: UsagePatternDataStage,
    val throwableValue: Throwable
) : RuntimeException(throwableValue)

private fun Throwable.toUsagePatternError(timezoneId: String): UsagePatternDataError {
    val wrapped = this as? UsagePatternDataException
    val stage = wrapped?.stageValue ?: UsagePatternDataStage.EXTRACT_FEATURES
    val cause = wrapped?.throwableValue ?: this

    return when (cause) {
        is java.time.DateTimeException,
        is IllegalArgumentException -> UsagePatternDataError.InvalidTimeZone(
            timezoneId = timezoneId,
            stage = stage,
            cause = cause
        )

        is com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException -> {
            UsagePatternDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )
        }

        else -> when (stage) {
            UsagePatternDataStage.READ_SESSIONS,
            UsagePatternDataStage.READ_INSIGHTS,
            UsagePatternDataStage.READ_PREFERENCES,
            UsagePatternDataStage.READ_APP_METADATA -> UsagePatternDataError.DataAccessFailure(
                stage = stage,
                cause = cause
            )

            UsagePatternDataStage.RESOLVE_DATE,
            UsagePatternDataStage.ENRICH_SESSIONS,
            UsagePatternDataStage.EXTRACT_FEATURES -> UsagePatternDataError.ProcessingFailure(
                stage = stage,
                cause = cause
            )
        }
    }
}
