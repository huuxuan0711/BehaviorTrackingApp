package com.xmobile.project2digitalwellbeing.data.usage.repository

import com.xmobile.project2digitalwellbeing.data.usage.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.usage.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.usage.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.usage.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.InsightDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.SessionDao
import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.dao.SyncStateDao
import com.xmobile.project2digitalwellbeing.data.usage.source.system.AppMetadataDataSource
import com.xmobile.project2digitalwellbeing.data.usage.source.system.UsageStatsDataSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val usageStatsDataSource: UsageStatsDataSource,
    private val appMetadataDataSource: AppMetadataDataSource,
    private val appMetadataDao: AppMetadataDao,
    private val sessionDao: SessionDao,
    private val insightDao: InsightDao,
    private val syncStateDao: SyncStateDao
) : UsageRepository {

    override suspend fun getUsageEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppUsageEvent> {
        return usageStatsDataSource.getUsageEvents(startTimeMillis, endTimeMillis)
    }

    override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
        if (packageNames.isEmpty()) {
            return emptyMap()
        }

        val cachedMetadata = appMetadataDao.getByPackageNames(packageNames.toList())
            .associateBy { it.packageName }

        val missingPackageNames = packageNames - cachedMetadata.keys
        val resolvedMetadata = if (missingPackageNames.isEmpty()) {
            emptyMap()
        } else {
            appMetadataDataSource.getAppMetadata(missingPackageNames).also { freshMetadata ->
                if (freshMetadata.isNotEmpty()) {
                    val updatedAtMillis = System.currentTimeMillis()
                    appMetadataDao.upsertAll(
                        freshMetadata.values.map { metadata ->
                            metadata.toEntity(updatedAtMillis = updatedAtMillis)
                        }
                    )
                }
            }
        }

        return buildMap {
            cachedMetadata.values.forEach { entity ->
                put(entity.packageName, entity.toDomain())
            }
            putAll(resolvedMetadata)
        }
    }

    override suspend fun getSessions(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppSession> {
        return sessionDao.getSessionsInRange(startTimeMillis, endTimeMillis).map { it.toDomain() }
    }

    override suspend fun saveSessions(sessions: List<AppSession>) {
        sessionDao.insertSessions(sessions.map { it.toEntity() })
    }

    override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) {
        sessionDao.deleteSessionsInRange(startTimeMillis, endTimeMillis)
    }

    override suspend fun getInsights(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<Insight> {
        return insightDao.getInsightsInRange(startTimeMillis, endTimeMillis).map { it.toDomain() }
    }

    override suspend fun saveInsights(
        insights: List<Insight>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ) {
        insightDao.insertInsights(
            insights.map { it.toEntity(windowStartMillis = windowStartMillis, windowEndMillis = windowEndMillis) }
        )
    }

    override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) {
        insightDao.deleteInsightsInRange(startTimeMillis, endTimeMillis)
    }

    override suspend fun getSyncState(): UsageSyncState {
        return syncStateDao.getSyncState()?.toDomain()
            ?: UsageSyncState(
                lastProcessedTimestampMillis = null,
                lastSeenEventTimestampMillis = null,
                lastSuccessfulRefreshTimestampMillis = null,
                isInitialSyncCompleted = false
            )
    }

    override suspend fun saveSyncState(state: UsageSyncState) {
        syncStateDao.upsertSyncState(state.toEntity())
    }
}
