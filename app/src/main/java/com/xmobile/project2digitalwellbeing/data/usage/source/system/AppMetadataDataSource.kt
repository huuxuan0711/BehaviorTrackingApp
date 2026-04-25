package com.xmobile.project2digitalwellbeing.data.usage.source.system

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata

interface AppMetadataDataSource {
    suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata>
}
