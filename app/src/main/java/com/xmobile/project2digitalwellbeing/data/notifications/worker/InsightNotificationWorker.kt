package com.xmobile.project2digitalwellbeing.data.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.data.notifications.AppNotifier
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataOutcome
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId

@HiltWorker
class InsightNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        return when (
            val outcome = getDashboardDataUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )
        ) {
            is GetDashboardDataOutcome.Success -> {
                val insight = outcome.data.topInsight ?: return Result.success()
                AppNotifier.showInsightNotification(
                    context = applicationContext,
                    title = applicationContext.getString(R.string.notification_insight_title),
                    message = insight.description
                )
                Result.success()
            }

            is GetDashboardDataOutcome.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "InsightNotificationWorker"
    }
}
