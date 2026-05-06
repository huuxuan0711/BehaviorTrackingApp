package com.xmobile.project2digitalwellbeing.domain.apps.repository

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata

interface AppRepository {
    suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata>
    suspend fun getAllAppMetadata(): List<AppMetadata>
    suspend fun updateAppCategory(packageName: String, category: AppCategory)
    suspend fun resolveAppName(packageName: String): String
}

