package com.xmobile.project2digitalwellbeing.domain.usage.usecase

import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerError
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerException
import com.xmobile.project2digitalwellbeing.data.usage.error.UsageDataLayerSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.CategoryUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDashboardDataUseCaseTest {

    @Test
    fun `returns dashboard data with top insight and limited top apps`() = runBlocking {
        val repository = FakeUsageRepository(
            sessions = listOf(
                AppSession("app.a", 1_000L, 61_000L, 60_000L),
                AppSession("app.b", 70_000L, 190_000L, 120_000L),
                AppSession("app.a", 200_000L, 260_000L, 60_000L)
            ),
            insights = listOf(
                Insight(InsightType.FREQUENT_SWITCHING, 55, 0.7f, emptyMap(), emptyList()),
                Insight(InsightType.LATE_NIGHT_USAGE, 80, 0.9f, emptyMap(), emptyList())
            )
        )

        val useCase = GetDashboardDataUseCase(
            repository = repository,
            aggregator = FakeUsageAggregator()
        )

        val outcome = useCase(
            GetDashboardDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok",
                topAppsLimit = 1
            )
        )

        require(outcome is GetDashboardDataOutcome.Success)
        assertEquals("1970-01-01", outcome.data.currentLocalDate)
        assertEquals(240_000L, outcome.data.dailyUsage.totalScreenTimeMillis)
        assertEquals(InsightType.LATE_NIGHT_USAGE, outcome.data.topInsight?.type)
        assertEquals(1, outcome.data.topApps.size)
        assertEquals("app.a", outcome.data.topApps.first().packageName)
    }

    @Test
    fun `maps invalid timezone to dashboard error`() = runBlocking {
        val useCase = GetDashboardDataUseCase(
            repository = FakeUsageRepository(),
            aggregator = FakeUsageAggregator()
        )

        val outcome = useCase(
            GetDashboardDataParams(
                nowMillis = 1_000L,
                timezoneId = "Bad/Timezone"
            )
        )

        require(outcome is GetDashboardDataOutcome.Failure)
        assertTrue(outcome.error is DashboardDataError.InvalidTimeZone)
        assertEquals(DashboardDataStage.RESOLVE_DATE, outcome.error.stage)
    }

    @Test
    fun `maps session read failure to data access error`() = runBlocking {
        val useCase = GetDashboardDataUseCase(
            repository = FakeUsageRepository(
                sessionsError = UsageDataLayerException(
                    UsageDataLayerError.CacheReadFailed(
                        source = UsageDataLayerSource.SESSION_CACHE,
                        cause = IllegalStateException("session cache failed")
                    )
                )
            ),
            aggregator = FakeUsageAggregator()
        )

        val outcome = useCase(
            GetDashboardDataParams(
                nowMillis = 1_000L,
                timezoneId = "Asia/Bangkok"
            )
        )

        require(outcome is GetDashboardDataOutcome.Failure)
        assertTrue(outcome.error is DashboardDataError.DataAccessFailure)
        assertEquals(DashboardDataStage.READ_SESSIONS, outcome.error.stage)
    }

    private class FakeUsageRepository(
        private val sessions: List<AppSession> = emptyList(),
        private val insights: List<Insight> = emptyList(),
        private val sessionsError: Throwable? = null
    ) : UsageRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long) = emptyList<com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent>()

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            return packageNames.associateWith { packageName ->
                AppMetadata(
                    packageName = packageName,
                    appName = packageName,
                    sourceCategory = SourceAppCategory.UNKNOWN,
                    reportingCategory = AppCategory.UNKNOWN,
                    classificationSource = ClassificationSource.UNKNOWN,
                    confidence = 0f
                )
            }
        }

        override suspend fun getSessions(startTimeMillis: Long, endTimeMillis: Long): List<AppSession> {
            sessionsError?.let { throw it }
            return sessions
        }

        override suspend fun saveSessions(sessions: List<AppSession>) = Unit

        override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit

        override suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long): List<Insight> = insights

        override suspend fun saveInsights(
            insights: List<Insight>,
            windowStartMillis: Long,
            windowEndMillis: Long
        ) = Unit

        override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit

        override suspend fun getSyncState(): UsageSyncState = UsageSyncState(null, null, null, false)

        override suspend fun saveSyncState(state: UsageSyncState) = Unit

        override suspend fun commitRefreshResult(
            windowStartMillis: Long,
            windowEndMillis: Long,
            sessions: List<AppSession>,
            insights: List<Insight>,
            newSyncState: UsageSyncState
        ) = Unit
    }

    private class FakeUsageAggregator : UsageAggregator {
        override fun buildDailyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            localDate: String
        ): DailyUsage {
            return DailyUsage(
                localDate = localDate,
                timezoneId = timezoneId,
                totalScreenTimeMillis = sessions.sumOf { it.durationMillis },
                totalSessionCount = sessions.size,
                sessions = sessions
            )
        }

        override fun buildAppUsageStats(
            sessions: List<AppSession>,
            appMetadataByPackage: Map<String, AppMetadata>
        ): List<AppUsageStat> {
            return sessions.groupBy { it.packageName }
                .map { (packageName, grouped) ->
                    AppUsageStat(
                        packageName = packageName,
                        appName = appMetadataByPackage[packageName]?.appName,
                        category = appMetadataByPackage[packageName]?.reportingCategory ?: AppCategory.UNKNOWN,
                        totalTimeMillis = grouped.sumOf { it.durationMillis },
                        launchCount = grouped.size
                    )
                }
                .sortedByDescending { it.totalTimeMillis }
        }

        override fun buildHourlyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            localDate: String?
        ): List<HourlyUsage> {
            return listOf(HourlyUsage(hourOfDay = 0, totalTimeMillis = sessions.sumOf { it.durationMillis }))
        }

        override fun buildWeeklyUsage(
            sessions: List<AppSession>,
            timezoneId: String,
            startLocalDate: String,
            endLocalDate: String
        ): WeeklyUsage {
            return WeeklyUsage(
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                timezoneId = timezoneId,
                totalScreenTimeMillis = sessions.sumOf { it.durationMillis },
                averageDailyScreenTimeMillis = 0L,
                mostUsedLocalDate = null,
                dailyUsages = emptyList()
            )
        }

        override fun buildCategoryUsage(
            sessions: List<AppSession>,
            appMetadataByPackage: Map<String, AppMetadata>
        ): List<CategoryUsage> = emptyList()
    }
}
