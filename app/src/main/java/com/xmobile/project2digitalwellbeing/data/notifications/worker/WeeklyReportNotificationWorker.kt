package com.xmobile.project2digitalwellbeing.data.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xmobile.project2digitalwellbeing.data.notifications.AppNotifier
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrendDirection
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetWeeklyOverviewDataUseCase
import com.xmobile.project2digitalwellbeing.helper.UsageFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import kotlin.math.abs

@HiltWorker
class WeeklyReportNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWeeklyOverviewDataUseCase: GetWeeklyOverviewDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        return when (
            val outcome = getWeeklyOverviewDataUseCase(
                GetWeeklyOverviewDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )
        ) {
            is GetWeeklyOverviewDataOutcome.Success -> {
                val totalScreenTimeText = UsageFormatter.formatDuration(
                    applicationContext,
                    outcome.data.weeklyUsage.totalScreenTimeMillis
                )
                val ratioPercent = (abs(outcome.data.trend.deltaRatio) * 100f).toInt()
                val trendSummary = when (outcome.data.trend.direction) {
                    UsageTrendDirection.UP -> "Screen time increased by $ratioPercent% this week."
                    UsageTrendDirection.DOWN -> "Screen time decreased by $ratioPercent% this week."
                    UsageTrendDirection.FLAT -> "Screen time stayed stable this week."
                }

                AppNotifier.showWeeklyReportNotification(
                    context = applicationContext,
                    totalScreenTimeText = totalScreenTimeText,
                    trendSummary = trendSummary
                )
                Result.success()
            }

            is GetWeeklyOverviewDataOutcome.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "WeeklyReportNotificationWorker"
    }
}
