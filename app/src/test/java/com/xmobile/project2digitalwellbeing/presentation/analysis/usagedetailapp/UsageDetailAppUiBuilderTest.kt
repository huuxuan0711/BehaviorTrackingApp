package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.content.Context
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.apps.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.SourceAppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.repository.AppRepository
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.GetAppMetadataUseCase
import com.xmobile.project2digitalwellbeing.domain.apps.usecase.ResolveAppNameUseCase
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.tracking.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.tracking.model.UsageSyncState
import com.xmobile.project2digitalwellbeing.domain.usage.repository.InsightRefreshGroup
import com.xmobile.project2digitalwellbeing.domain.usage.repository.UsageRepository
import com.xmobile.project2digitalwellbeing.domain.usage.service.UsageAggregatorImpl
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UsageDetailAppUiBuilderTest {

    @Test
    fun `builds usage detail state from sessions and app metadata`() = runTest {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 5, 13)
        val nowMillis = today.atTime(12, 30).atZone(zoneId).toInstant().toEpochMilli()
        val targetPackage = "app.focus"
        val otherPackage = "app.chat"
        val sessions = listOf(
            session(targetPackage, today, LocalTime.of(10, 0), LocalTime.of(11, 0), zoneId),
            session(otherPackage, today, LocalTime.of(11, 1), LocalTime.of(11, 6), zoneId)
        )
        val repository = FakeRepository(
            sessions = sessions,
            metadata = mapOf(
                targetPackage to metadata(targetPackage, "Focus App"),
                otherPackage to metadata(otherPackage, "Chat App")
            )
        )
        val builder = UsageDetailAppUiBuilder(
            context = fakeContext(),
            usageRepository = repository,
            getAppMetadataUseCase = GetAppMetadataUseCase(repository),
            resolveAppNameUseCase = ResolveAppNameUseCase(repository),
            aggregator = UsageAggregatorImpl()
        )

        val state = builder.build(targetPackage, nowMillis)

        assertEquals(false, state.isLoading)
        assertEquals("Focus App", state.appName)
        assertEquals("1h 0m", state.todayTotalFormatted)
        assertEquals(1, state.totalSessionsToday)
        assertEquals("60m 0s", state.avgSessionFormatted)
        assertEquals("60m 0s", state.longestSessionFormatted)
        assertEquals("60m 0s", state.shortestSessionFormatted)
        assertEquals("Most active in the morning", state.peakUsageLabel)
        assertEquals("You tend to use Focus App mostly in the morning", state.insightSummary)
        assertEquals("Morning tip", state.tipSummary)
        assertEquals(7, state.weekLineChartData.size)
        assertTrue(state.weekLineChartData.last() > 0f)
        assertEquals(24, state.todayHourlyBarChartData.size)
        assertTrue(state.todayHourlyBarChartData.any { it == 60f })
        assertEquals(1, state.topTransitions.size)
        assertEquals("Focus App", state.topTransitions.first().fromAppName)
        assertEquals("Chat App", state.topTransitions.first().toAppName)
        assertEquals(1, state.topTransitions.first().count)
    }

    private fun fakeContext(): Context {
        val context = mock<Context>()
        whenever(context.getString(R.string.auto_peak_usage_after_midnight)).thenReturn("Most active after midnight")
        whenever(context.getString(R.string.auto_peak_usage_morning)).thenReturn("Most active in the morning")
        whenever(context.getString(R.string.auto_peak_usage_afternoon)).thenReturn("Most active in the afternoon")
        whenever(context.getString(R.string.auto_peak_usage_night)).thenReturn("Most active at night")
        whenever(context.getString(R.string.auto_usage_period_late_night)).thenReturn("late at night")
        whenever(context.getString(R.string.auto_usage_period_morning)).thenReturn("in the morning")
        whenever(context.getString(R.string.auto_usage_period_afternoon)).thenReturn("in the afternoon")
        whenever(context.getString(R.string.auto_usage_period_evening)).thenReturn("in the evening")
        whenever(context.getString(eq(R.string.auto_tip_after_midnight), any())).thenReturn("Late night tip")
        whenever(context.getString(R.string.auto_tip_morning_usage)).thenReturn("Morning tip")
        whenever(context.getString(R.string.auto_tip_afternoon_usage)).thenReturn("Afternoon tip")
        whenever(context.getString(R.string.auto_tip_evening_usage)).thenReturn("Evening tip")
        return context
    }

    private fun session(
        packageName: String,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        zoneId: ZoneId
    ): AppSession {
        val startMillis = date.atTime(start).atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = date.atTime(end).atZone(zoneId).toInstant().toEpochMilli()
        return AppSession(
            packageName = packageName,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            durationMillis = endMillis - startMillis
        )
    }

    private fun metadata(packageName: String, appName: String): AppMetadata {
        return AppMetadata(
            packageName = packageName,
            appName = appName,
            sourceCategory = SourceAppCategory.UNKNOWN,
            reportingCategory = AppCategory.UNKNOWN,
            classificationSource = ClassificationSource.UNKNOWN,
            confidence = 1f
        )
    }

    private class FakeRepository(
        private val sessions: List<AppSession>,
        private val metadata: Map<String, AppMetadata>
    ) : UsageRepository, AppRepository {
        override suspend fun getUsageEvents(startTimeMillis: Long, endTimeMillis: Long): List<AppUsageEvent> = emptyList()

        override suspend fun getSessions(startTimeMillis: Long, endTimeMillis: Long): List<AppSession> = sessions

        override suspend fun saveSessions(sessions: List<AppSession>) = Unit

        override suspend fun deleteSessionsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit

        override suspend fun getInsights(startTimeMillis: Long, endTimeMillis: Long): List<Insight> = emptyList()

        override suspend fun saveInsights(insights: List<Insight>, windowStartMillis: Long, windowEndMillis: Long) = Unit

        override suspend fun deleteInsightsInRange(startTimeMillis: Long, endTimeMillis: Long) = Unit

        override suspend fun getSyncState(): UsageSyncState = UsageSyncState(null, null, null, false)

        override suspend fun saveSyncState(state: UsageSyncState) = Unit

        override suspend fun commitRefreshResult(
            windowStartMillis: Long,
            windowEndMillis: Long,
            sessions: List<AppSession>,
            insights: List<InsightRefreshGroup>,
            newSyncState: UsageSyncState
        ) = Unit

        override suspend fun getAppMetadata(packageNames: Set<String>): Map<String, AppMetadata> {
            return packageNames.mapNotNull { packageName ->
                metadata[packageName]?.let { packageName to it }
            }.toMap()
        }

        override suspend fun getAllAppMetadata(): List<AppMetadata> = metadata.values.toList()

        override suspend fun updateAppCategory(packageName: String, category: AppCategory) = Unit

        override suspend fun resolveAppName(packageName: String): String = metadata[packageName]?.appName ?: packageName
    }
}
