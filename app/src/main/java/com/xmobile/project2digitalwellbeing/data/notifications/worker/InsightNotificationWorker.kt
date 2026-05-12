package com.xmobile.project2digitalwellbeing.data.notifications.worker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.data.notifications.AppNotifier
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceOutcome
import com.xmobile.project2digitalwellbeing.domain.orchestrator.usecase.GetDashboardExperienceUseCase
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.GetDashboardDataParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId

@HiltWorker
class InsightNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getDashboardExperienceUseCase: GetDashboardExperienceUseCase
) : CoroutineWorker(appContext, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        return when (
            val outcome = getDashboardExperienceUseCase(
                GetDashboardDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId
                )
            )
        ) {
            is GetDashboardExperienceOutcome.Success -> {
                val message = outcome.data.insightSummaryText
                if (message.isBlank()) return Result.success()

                AppNotifier.showInsightNotification(
                    context = applicationContext,
                    title = applicationContext.getString(R.string.notification_insight_title),
                    message = message
                )
                Result.success()
            }

            is GetDashboardExperienceOutcome.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "InsightNotificationWorker"
    }
}
