package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException
import java.time.DateTimeException
import javax.inject.Inject

class UsageErrorMapperImpl @Inject constructor() : UsageErrorMapper {

    override fun mapRefreshError(
        stage: UsagePipelineStage,
        params: RefreshUsageDataParams,
        throwable: Throwable
    ): UsageDataError {
        return when {
            throwable is UsageDataLayerException -> throwable.error.toUsageDataError(stage)

            throwable is SecurityException -> {
                UsageDataError.PermissionDenied(
                    stage = stage,
                    cause = throwable
                )
            }

            throwable is DateTimeException || throwable is IllegalArgumentException -> {
                UsageDataError.InvalidTimeZone(
                    timezoneId = params.timezoneId,
                    stage = stage,
                    cause = throwable
                )
            }

            stage in setOf(
                UsagePipelineStage.READ_SYNC_STATE,
                UsagePipelineStage.FETCH_USAGE_EVENTS,
                UsagePipelineStage.READ_PREFERENCES,
                UsagePipelineStage.READ_APP_METADATA
            ) -> {
                UsageDataError.DataAccessFailure(
                    stage = stage,
                    cause = throwable
                )
            }

            stage == UsagePipelineStage.PERSIST_RESULTS -> {
                UsageDataError.PersistenceFailure(
                    stage = stage,
                    cause = throwable
                )
            }

            stage in setOf(
                UsagePipelineStage.BUILD_SESSIONS,
                UsagePipelineStage.BUILD_DAILY_USAGE,
                UsagePipelineStage.ENRICH_SESSIONS,
                UsagePipelineStage.EXTRACT_FEATURES,
                UsagePipelineStage.GENERATE_INSIGHTS
            ) -> {
                UsageDataError.ProcessingFailure(
                    stage = stage,
                    cause = throwable
                )
            }

            else -> {
                UsageDataError.UnknownFailure(
                    stage = stage,
                    cause = throwable
                )
            }
        }
    }

    private fun UsageDataLayerError.toUsageDataError(
        stage: UsagePipelineStage
    ): UsageDataError {
        return when (this) {
            is UsageDataLayerError.PermissionDenied -> {
                UsageDataError.PermissionDenied(
                    stage = stage,
                    cause = cause
                )
            }

            is UsageDataLayerError.SystemReadFailed,
            is UsageDataLayerError.CacheReadFailed -> {
                UsageDataError.DataAccessFailure(
                    stage = stage,
                    cause = cause
                )
            }

            is UsageDataLayerError.CacheWriteFailed,
            is UsageDataLayerError.TransactionFailed -> {
                UsageDataError.PersistenceFailure(
                    stage = stage,
                    cause = cause
                )
            }

            is UsageDataLayerError.Unknown -> {
                UsageDataError.UnknownFailure(
                    stage = stage,
                    cause = cause
                )
            }
        }
    }
}
