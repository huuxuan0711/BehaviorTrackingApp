package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.CategoryUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class UsageAggregatorImpl @Inject constructor() : UsageAggregator {

    override fun buildDailyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        localDate: String
    ): DailyUsage {
        val zoneId = ZoneId.of(timezoneId)
        val date = LocalDate.parse(localDate)
        val dayStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEndMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val clippedSessions = sessions.mapNotNull { session ->
            session.clipToRange(
                startMillis = dayStartMillis,
                endMillis = dayEndMillis
            )
        }

        return DailyUsage(
            localDate = localDate,
            timezoneId = timezoneId,
            totalScreenTimeMillis = clippedSessions.sumOf { it.durationMillis },
            totalSessionCount = clippedSessions.size,
            sessions = clippedSessions
        )
    }

    override fun buildAppUsageStats(
        sessions: List<AppSession>,
        appMetadataByPackage: Map<String, AppMetadata>
    ): List<AppUsageStat> {
        return sessions
            .groupBy { it.packageName }
            .map { (packageName, appSessions) ->
                val metadata = appMetadataByPackage[packageName]
                AppUsageStat(
                    packageName = packageName,
                    appName = metadata?.appName,
                    category = metadata?.reportingCategory ?: AppCategory.UNKNOWN,
                    totalTimeMillis = appSessions.sumOf { it.durationMillis },
                    launchCount = appSessions.size
                )
            }
            .sortedWith(
                compareByDescending<AppUsageStat> { it.totalTimeMillis }
                    .thenByDescending { it.launchCount }
                    .thenBy { it.packageName }
            )
    }

    override fun buildHourlyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        localDate: String?
    ): List<HourlyUsage> {
        val zoneId = ZoneId.of(timezoneId)
        val hourBuckets = LongArray(24)
        val range = localDate?.let { dateText ->
            val date = LocalDate.parse(dateText)
            val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            startMillis to endMillis
        }

        sessions.forEach { session ->
            val effectiveSession = range?.let { (startMillis, endMillis) ->
                session.clipToRange(startMillis = startMillis, endMillis = endMillis)
            } ?: session

            if (effectiveSession != null) {
                accumulateSessionByHour(
                    session = effectiveSession,
                    zoneId = zoneId,
                    hourBuckets = hourBuckets
                )
            }
        }

        return hourBuckets.mapIndexed { hourOfDay, totalTimeMillis ->
            HourlyUsage(
                hourOfDay = hourOfDay,
                totalTimeMillis = totalTimeMillis
            )
        }
    }

    override fun buildWeeklyUsage(
        sessions: List<AppSession>,
        timezoneId: String,
        startLocalDate: String,
        endLocalDate: String
    ): WeeklyUsage {
        val startDate = LocalDate.parse(startLocalDate)
        val endDate = LocalDate.parse(endLocalDate)
        val dailyUsages = generateSequence(startDate) { current ->
            current.plusDays(1).takeIf { !it.isAfter(endDate) }
        }.map { currentDate ->
            buildDailyUsage(
                sessions = sessions,
                timezoneId = timezoneId,
                localDate = currentDate.toString()
            )
        }.toList()

        val totalScreenTimeMillis = dailyUsages.sumOf { it.totalScreenTimeMillis }
        val averageDailyScreenTimeMillis = if (dailyUsages.isEmpty()) {
            0L
        } else {
            totalScreenTimeMillis / dailyUsages.size
        }
        val mostUsedLocalDate = dailyUsages
            .maxByOrNull { it.totalScreenTimeMillis }
            ?.takeIf { it.totalScreenTimeMillis > 0L }
            ?.localDate

        return WeeklyUsage(
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            timezoneId = timezoneId,
            totalScreenTimeMillis = totalScreenTimeMillis,
            averageDailyScreenTimeMillis = averageDailyScreenTimeMillis,
            mostUsedLocalDate = mostUsedLocalDate,
            dailyUsages = dailyUsages
        )
    }

    override fun buildCategoryUsage(
        sessions: List<AppSession>,
        appMetadataByPackage: Map<String, AppMetadata>
    ): List<CategoryUsage> {
        return sessions
            .groupBy { session ->
                appMetadataByPackage[session.packageName]?.reportingCategory ?: AppCategory.UNKNOWN
            }
            .map { (category, categorySessions) ->
                val packageNames = categorySessions
                    .map { it.packageName }
                    .distinct()
                    .sorted()

                CategoryUsage(
                    category = category,
                    totalTimeMillis = categorySessions.sumOf { it.durationMillis },
                    sessionCount = categorySessions.size,
                    appCount = packageNames.size,
                    packageNames = packageNames
                )
            }
            .sortedWith(
                compareByDescending<CategoryUsage> { it.totalTimeMillis }
                    .thenBy { it.category.name }
            )
    }

    private fun accumulateSessionByHour(
        session: AppSession,
        zoneId: ZoneId,
        hourBuckets: LongArray
    ) {
        var currentStartMillis = session.startTimeMillis
        val sessionEndMillis = session.endTimeMillis

        while (currentStartMillis < sessionEndMillis) {
            val currentInstant = Instant.ofEpochMilli(currentStartMillis)
            val currentZonedDateTime = currentInstant.atZone(zoneId)
            val nextHourMillis = currentZonedDateTime
                .plusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant()
                .toEpochMilli()
            val sliceEndMillis = minOf(sessionEndMillis, nextHourMillis)
            hourBuckets[currentZonedDateTime.hour] += (sliceEndMillis - currentStartMillis)
            currentStartMillis = sliceEndMillis
        }
    }

    private fun AppSession.clipToRange(startMillis: Long, endMillis: Long): AppSession? {
        val clippedStartMillis = maxOf(startTimeMillis, startMillis)
        val clippedEndMillis = minOf(endTimeMillis, endMillis)
        val clippedDurationMillis = clippedEndMillis - clippedStartMillis

        if (clippedDurationMillis <= 0L) {
            return null
        }

        return AppSession(
            packageName = packageName,
            startTimeMillis = clippedStartMillis,
            endTimeMillis = clippedEndMillis,
            durationMillis = clippedDurationMillis
        )
    }
}
