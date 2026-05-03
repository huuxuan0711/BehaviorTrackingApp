package com.xmobile.project2digitalwellbeing.data.tracking.repository

import android.util.Log
import androidx.room.withTransaction
import com.xmobile.project2digitalwellbeing.BuildConfig
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private val logTag = "UsageSessions"

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
            sessionDao.getSessionsInRange(startTimeMillis, endTimeMillis)
                .map { it.toDomain() }
                .also { sessions ->
                    logSessions(
                        stage = "READ",
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        sessions = sessions
                    )
                }
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
            logSessions(
                stage = "SAVE",
                startTimeMillis = sessions.minOfOrNull { it.startTimeMillis } ?: 0L,
                endTimeMillis = sessions.maxOfOrNull { it.endTimeMillis } ?: 0L,
                sessions = sessions
            )
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
            logSessions(
                stage = "COMMIT",
                startTimeMillis = windowStartMillis,
                endTimeMillis = windowEndMillis,
                sessions = sessions
            )
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.TransactionFailed(
                    source = UsageDataLayerSource.USAGE_REPOSITORY,
                    cause = exception
                )
            )
        }
    }

    private fun logSessions(
        stage: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        sessions: List<AppSession>
    ) {
        if (!BuildConfig.DEBUG) return

        val topPackages = sessions
            .groupBy { it.packageName }
            .mapValues { (_, grouped) ->
                grouped.sumOf { it.durationMillis } to grouped.size
            }
            .entries
            .sortedByDescending { it.value.first }
            .take(8)
            .joinToString(separator = " | ") { (packageName, value) ->
                val totalDurationMillis = value.first
                val count = value.second
                "$packageName count=$count total=${totalDurationMillis.toDurationText()}"
            }

        val suspiciousSamples = sessions
            .filter { it.durationMillis <= 0L || it.durationMillis >= 3L * 60L * 60L * 1000L }
            .take(8)
            .joinToString(separator = " | ") { session ->
                "${session.packageName} ${session.startTimeMillis.toDebugTime()}..${session.endTimeMillis.toDebugTime()} duration=${session.durationMillis.toDurationText()}"
            }
            .ifBlank { "none" }

        val sampleSessions = sessions
            .take(5)
            .joinToString(separator = " | ") { session ->
                "${session.packageName} ${session.startTimeMillis.toDebugTime()}..${session.endTimeMillis.toDebugTime()} (${session.durationMillis.toDurationText()})"
            }
            .ifBlank { "none" }

        Log.d(
            logTag,
            buildString {
                append("[$stage] range=${startTimeMillis.toDebugTime()}..${endTimeMillis.toDebugTime()}")
                append(" count=${sessions.size}")
                append(" topPackages=")
                append(if (topPackages.isBlank()) "none" else topPackages)
                append(" suspicious=")
                append(suspiciousSamples)
                append(" sample=")
                append(sampleSessions)
            }
        )
    }

    private fun Long.toDebugTime(): String {
        if (this <= 0L) return "0"
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.getDefault()))
    }

    private fun Long.toDurationText(): String {
        val totalMinutes = this / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
