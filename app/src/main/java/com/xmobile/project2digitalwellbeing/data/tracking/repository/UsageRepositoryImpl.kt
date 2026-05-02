package com.xmobile.project2digitalwellbeing.data.tracking.repository

import androidx.room.withTransaction
import com.xmobile.project2digitalwellbeing.data.apps.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.apps.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.dao.AppMetadataDao
import com.xmobile.project2digitalwellbeing.data.apps.source.system.AppMetadataDataSource
import com.xmobile.project2digitalwellbeing.data.insights.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.insights.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.dao.InsightDao
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.data.tracking.mapper.toDomain
import com.xmobile.project2digitalwellbeing.data.tracking.mapper.toEntity
import com.xmobile.project2digitalwellbeing.data.analytics.source.local.room.AnalyticsDatabase
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SessionDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.local.room.dao.SyncStateDao
import com.xmobile.project2digitalwellbeing.data.tracking.source.system.UsageStatsDataSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val usageStatsDataSource: UsageStatsDataSource,
    private val appMetadataDataSource: AppMetadataDataSource,
    private val usageDatabase: AnalyticsDatabase,
    private val appMetadataDao: AppMetadataDao,
    private val sessionDao: SessionDao,
    private val insightDao: InsightDao,
    private val syncStateDao: SyncStateDao
) : UsageRepository {

    override suspend fun getUsageEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppUsageEvent> {
        return try {
            usageStatsDataSource.getUsageEvents(startTimeMillis, endTimeMillis)
        } catch (exception: UsageDataLayerException) {
            throw exception
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.SystemReadFailed(
                    source = UsageDataLayerSource.USAGE_REPOSITORY,
                    cause = exception
                )
            )
        }
    }

    override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
        if (packageNames.isEmpty()) {
            return emptyMap()
        }

        return try {
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

            buildMap {
                cachedMetadata.values.forEach { entity ->
                    put(entity.packageName, entity.toDomain())
                }
                putAll(resolvedMetadata)
            }
        } catch (exception: UsageDataLayerException) {
            throw exception
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.METADATA_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun getSessions(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppSession> {
        return try {
            sessionDao.getSessionsInRange(startTimeMillis, endTimeMillis).map { it.toDomain() }
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.SESSION_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun saveSessions(sessions: List<AppSession>) {
        try {
            sessionDao.insertSessions(sessions.map { it.toEntity() })
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.SESSION_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) {
        try {
            sessionDao.deleteSessionsInRange(startTimeMillis, endTimeMillis)
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.SESSION_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun getInsights(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<Insight> {
        return try {
            insightDao.getInsightsInRange(startTimeMillis, endTimeMillis).map { it.toDomain() }
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.INSIGHT_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun saveInsights(
        insights: List<Insight>,
        windowStartMillis: Long,
        windowEndMillis: Long
    ) {
        try {
            insightDao.insertInsights(
                insights.map { it.toEntity(windowStartMillis = windowStartMillis, windowEndMillis = windowEndMillis) }
            )
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.INSIGHT_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) {
        try {
            insightDao.deleteInsightsInRange(startTimeMillis, endTimeMillis)
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.INSIGHT_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun getSyncState(): UsageSyncState {
        return try {
            syncStateDao.getSyncState()?.toDomain()
                ?: UsageSyncState(
                    lastProcessedTimestampMillis = null,
                    lastSeenEventTimestampMillis = null,
                    lastSuccessfulRefreshTimestampMillis = null,
                    isInitialSyncCompleted = false
                )
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheReadFailed(
                    source = UsageDataLayerSource.SYNC_STATE_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun saveSyncState(state: UsageSyncState) {
        try {
            syncStateDao.upsertSyncState(state.toEntity())
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.CacheWriteFailed(
                    source = UsageDataLayerSource.SYNC_STATE_CACHE,
                    cause = exception
                )
            )
        }
    }

    override suspend fun commitRefreshResult(
        windowStartMillis: Long,
        windowEndMillis: Long,
        sessions: List<AppSession>,
        insights: List<Insight>,
        newSyncState: UsageSyncState
    ) {
        try {
            usageDatabase.withTransaction {
                sessionDao.deleteSessionsInRange(windowStartMillis, windowEndMillis)
                if (sessions.isNotEmpty()) {
                    sessionDao.insertSessions(sessions.map { it.toEntity() })
                }
                insightDao.deleteInsightsInRange(windowStartMillis, windowEndMillis)
                if (insights.isNotEmpty()) {
                    insightDao.insertInsights(
                        insights.map { insight ->
                            insight.toEntity(
                                windowStartMillis = windowStartMillis,
                                windowEndMillis = windowEndMillis
                            )
                        }
                    )
                }
                syncStateDao.upsertSyncState(newSyncState.toEntity())
            }
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.TransactionFailed(
                    source = UsageDataLayerSource.USAGE_REPOSITORY,
                    cause = exception
                )
            )
        }
    }
}
