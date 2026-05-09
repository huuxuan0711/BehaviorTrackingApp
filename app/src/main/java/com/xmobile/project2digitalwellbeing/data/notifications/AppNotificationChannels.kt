package com.xmobile.project2digitalwellbeing.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.xmobile.project2digitalwellbeing.R

object AppNotificationChannels {
    const val INSIGHTS_CHANNEL_ID = "insights_channel"
    const val WEEKLY_REPORTS_CHANNEL_ID = "weekly_reports_channel"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val insightsChannel = NotificationChannel(
            INSIGHTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_insights_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_insights_description)
        }

        val weeklyChannel = NotificationChannel(
            WEEKLY_REPORTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_weekly_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_weekly_description)
        }

        manager.createNotificationChannel(insightsChannel)
        manager.createNotificationChannel(weeklyChannel)
    }
}
