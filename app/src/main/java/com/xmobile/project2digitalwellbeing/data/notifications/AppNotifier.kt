package com.xmobile.project2digitalwellbeing.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.presentation.main.MainActivity
import kotlin.math.abs

object AppNotifier {
    private const val INSIGHT_NOTIFICATION_ID = 4101
    private const val WEEKLY_NOTIFICATION_ID = 4102

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showInsightNotification(
        context: Context,
        title: String,
        message: String
    ) {
        if (!canPostNotifications(context)) return

        val notification = NotificationCompat.Builder(context, AppNotificationChannels.INSIGHTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_usage_access)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createOpenMainPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(INSIGHT_NOTIFICATION_ID, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWeeklyReportNotification(
        context: Context,
        totalScreenTimeText: String,
        trendSummary: String?
    ) {
        if (!canPostNotifications(context)) return

        val summary = trendSummary?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_weekly_fallback_summary)
        val message = context.getString(
            R.string.notification_weekly_message_template,
            totalScreenTimeText,
            summary
        )

        val notification = NotificationCompat.Builder(context, AppNotificationChannels.WEEKLY_REPORTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_usage_access)
            .setContentTitle(context.getString(R.string.notification_weekly_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createOpenMainPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(WEEKLY_NOTIFICATION_ID, notification)
    }

    private fun createOpenMainPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
