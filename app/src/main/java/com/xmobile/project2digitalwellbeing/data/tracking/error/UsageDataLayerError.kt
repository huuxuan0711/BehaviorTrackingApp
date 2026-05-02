package com.xmobile.project2digitalwellbeing.data.tracking.error

sealed interface UsageDataLayerError {
    val source: UsageDataLayerSource
    val cause: Throwable?

    data class PermissionDenied(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError

    data class SystemReadFailed(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError

    data class CacheReadFailed(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError

    data class CacheWriteFailed(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError

    data class TransactionFailed(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError

    data class Unknown(
        override val source: UsageDataLayerSource,
        override val cause: Throwable?
    ) : UsageDataLayerError
}

enum class UsageDataLayerSource {
    USAGE_STATS,
    APP_METADATA,
    METADATA_CACHE,
    SESSION_CACHE,
    INSIGHT_CACHE,
    SYNC_STATE_CACHE,
    USAGE_REPOSITORY
}

class UsageDataLayerException(
    val error: UsageDataLayerError
) : RuntimeException(error.cause)
