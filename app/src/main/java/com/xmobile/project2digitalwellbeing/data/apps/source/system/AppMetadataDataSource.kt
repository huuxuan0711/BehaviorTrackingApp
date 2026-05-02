package com.xmobile.project2digitalwellbeing.data.apps.source.system

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata

interface AppMetadataDataSource {
    suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata>
}
