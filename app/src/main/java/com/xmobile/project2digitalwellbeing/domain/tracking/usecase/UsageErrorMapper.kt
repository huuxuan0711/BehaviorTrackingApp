package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

interface UsageErrorMapper {
    fun mapRefreshError(
        stage: UsagePipelineStage,
        params: RefreshUsageDataParams,
        throwable: Throwable
    ): UsageDataError
}
