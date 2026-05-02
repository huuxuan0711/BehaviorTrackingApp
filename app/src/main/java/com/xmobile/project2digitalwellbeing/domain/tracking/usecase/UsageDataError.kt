package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

sealed interface UsageDataError {
    val stage: UsagePipelineStage
    val cause: Throwable?

    data class PermissionDenied(
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError

    data class InvalidTimeZone(
        val timezoneId: String,
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError

    data class DataAccessFailure(
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError

    data class ProcessingFailure(
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError

    data class PersistenceFailure(
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError

    data class UnknownFailure(
        override val stage: UsagePipelineStage,
        override val cause: Throwable?
    ) : UsageDataError
}

enum class UsagePipelineStage {
    READ_SYNC_STATE,
    FETCH_USAGE_EVENTS,
    BUILD_SESSIONS,
    BUILD_DAILY_USAGE,
    READ_PREFERENCES,
    READ_APP_METADATA,
    ENRICH_SESSIONS,
    EXTRACT_FEATURES,
    GENERATE_INSIGHTS,
    PERSIST_RESULTS
}
