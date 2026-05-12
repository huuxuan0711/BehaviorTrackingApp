package com.xmobile.project2digitalwellbeing.data.notifications.worker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xmobile.project2digitalwellbeing.data.notifications.AppNotifier
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetWeeklyOverviewExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetWeeklyOverviewExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataParams
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId

@HiltWorker
class WeeklyReportNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeeklyOverviewExperienceUseCase: GetWeeklyOverviewExperienceUseCase
) : CoroutineWorker(appContext, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        return when (
            val outcome = getWeeklyOverviewExperienceUseCase(
                GetWeeklyOverviewDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )
        ) {
            is GetWeeklyOverviewExperienceOutcome.Success -> {
                val totalScreenTimeText = UsageFormatter.formatDuration(
                    applicationContext,
                    outcome.data.weeklyData.weeklyUsage.totalScreenTimeMillis
                )

                val trendSummary = outcome.data.insightSummaryText

                AppNotifier.showWeeklyReportNotification(
                    context = applicationContext,
                    totalScreenTimeText = totalScreenTimeText,
                    trendSummary = trendSummary
                )
                Result.success()
            }

            is GetWeeklyOverviewExperienceOutcome.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "WeeklyReportNotificationWorker"
    }
}
