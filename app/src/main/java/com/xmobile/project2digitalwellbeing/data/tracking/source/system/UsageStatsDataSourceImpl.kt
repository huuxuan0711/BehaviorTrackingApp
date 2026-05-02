package com.xmobile.project2digitalwellbeing.data.tracking.source.system

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.tracking.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageEventType
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UsageStatsDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageStatsDataSource {

    override suspend fun getUsageEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppUsageEvent> {
        if (startTimeMillis >= endTimeMillis) {
            return emptyList()
        }

        if (!UsageAccessPermissionHelper.hasUsageAccessPermission(context)) {
            throw UsageDataLayerException(
                UsageDataLayerError.PermissionDenied(
                    source = UsageDataLayerSource.USAGE_STATS,
                    cause = SecurityException("Usage access permission is not granted.")
                )
            )
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usageEvents = usageStatsManager.queryEvents(startTimeMillis, endTimeMillis)
            val event = UsageEvents.Event()
            val events = mutableListOf<AppUsageEvent>()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                val normalizedType = event.eventType.toUsageEventTypeOrNull() ?: continue
                val packageName = event.packageName?.takeIf { it.isNotBlank() } ?: continue

                events += AppUsageEvent(
                    packageName = packageName,
                    timestampMillis = event.timeStamp,
                    type = normalizedType
                )
            }

            return events
                .distinctBy { Triple(it.packageName, it.timestampMillis, it.type) }
                .sortedBy { it.timestampMillis }
        } catch (exception: SecurityException) {
            throw UsageDataLayerException(
                UsageDataLayerError.PermissionDenied(
                    source = UsageDataLayerSource.USAGE_STATS,
                    cause = exception
                )
            )
        } catch (exception: Exception) {
            throw UsageDataLayerException(
                UsageDataLayerError.SystemReadFailed(
                    source = UsageDataLayerSource.USAGE_STATS,
                    cause = exception
                )
            )
        }
    }

    private fun Int.toUsageEventTypeOrNull(): UsageEventType? {
        return when (this) {
            UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.FOREGROUND

            UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.BACKGROUND

            else -> null
        }
    }
}
