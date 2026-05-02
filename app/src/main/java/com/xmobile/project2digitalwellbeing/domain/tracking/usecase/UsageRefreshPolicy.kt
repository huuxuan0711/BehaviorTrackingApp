package com.xmobile.project2digitalwellbeing.domain.tracking.usecase

import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState

interface UsageRefreshPolicy {
    fun resolveWindow(
        params: RefreshUsageDataParams,
        syncState: UsageSyncState
    ): UsageRefreshWindow
}
