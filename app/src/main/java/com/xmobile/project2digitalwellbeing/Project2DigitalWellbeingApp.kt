package com.xmobile.project2digitalwellbeing

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xmobile.project2digitalwellbeing.data.notifications.AppNotificationChannels
import com.xmobile.project2digitalwellbeing.data.notifications.worker.InsightNotificationWorker
import com.xmobile.project2digitalwellbeing.data.notifications.worker.WeeklyReportNotificationWorker
import com.xmobile.project2digitalwellbeing.data.preferences.local.AppPreferencesDataStore
import com.xmobile.project2digitalwellbeing.data.tracking.worker.UsageSyncWorker
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageAnalysisPreferences
import dagger.hilt.android.HiltAndroidApp
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltAndroidApp
class Project2DigitalWellbeingApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.create(this)
        observePreferences()
        scheduleUsageSync()
    }

    private fun observePreferences() {
        val appPreferencesDataStore = AppPreferencesDataStore(this)
        appScope.launch {
            appPreferencesDataStore.usageAnalysisPreferences
                .collectLatest { preferences ->
                    applyThemePreference(preferences)
                    scheduleNotifications(preferences)
                }
        }
    }

    private fun applyThemePreference(preferences: UsageAnalysisPreferences) {
        AppCompatDelegate.setDefaultNightMode(
            if (preferences.darkModeEnabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
        applyLanguagePreference(preferences.languageCode)
    }

    private fun applyLanguagePreference(languageCode: String) {
        val targetLocales = LocaleListCompat.forLanguageTags(languageCode)
        if (AppCompatDelegate.getApplicationLocales() == targetLocales) return
        AppCompatDelegate.setApplicationLocales(targetLocales)
    }

    private fun scheduleNotifications(preferences: UsageAnalysisPreferences) {
        val workManager = WorkManager.getInstance(this)
        if (preferences.insightNotificationsEnabled) {
            val initialDelay = computeDelayToNextHour(targetHour = 20)
            val request = PeriodicWorkRequestBuilder<InsightNotificationWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                InsightNotificationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(InsightNotificationWorker.WORK_NAME)
        }

        if (preferences.weeklyReportsEnabled) {
            val initialDelay = computeDelayToNextWeeklySlot(
                targetDay = DayOfWeek.MONDAY,
                targetHour = 8
            )
            val request = PeriodicWorkRequestBuilder<WeeklyReportNotificationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WeeklyReportNotificationWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(WeeklyReportNotificationWorker.WORK_NAME)
        }
    }

    private fun computeDelayToNextHour(targetHour: Int): Duration {
        val now = LocalDateTime.now()
        val next = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
        val target = if (next.isAfter(now)) next else next.plusDays(1)
        return Duration.between(now, target)
    }

    private fun computeDelayToNextWeeklySlot(targetDay: DayOfWeek, targetHour: Int): Duration {
        val now = LocalDateTime.now()
        var target = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
        while (target.dayOfWeek != targetDay || !target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target)
    }

    private fun scheduleUsageSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UsageSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
