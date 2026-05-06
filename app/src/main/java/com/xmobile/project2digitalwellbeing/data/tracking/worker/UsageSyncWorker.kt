package com.xmobile.project2digitalwellbeing.data.tracking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataParams
import com.xmobile.project2digitalwellbeing.domain.tracking.usecase.RefreshUsageDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshUsageDataUseCase: RefreshUsageDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val nowMillis = System.currentTimeMillis()
        val timezoneId = ZoneId.systemDefault().id

        return try {
            refreshUsageDataUseCase(
                RefreshUsageDataParams(
                    nowMillis = nowMillis,
                    timezoneId = timezoneId,
                    forceFullRefresh = false
                )
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "UsageSyncWorker"
    }
}
