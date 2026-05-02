package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

sealed interface RefreshUsageDataOutcome {
    data class Success(val result: RefreshUsageDataResult) : RefreshUsageDataOutcome

    data class Failure(val error: UsageDataError) : RefreshUsageDataOutcome
}
