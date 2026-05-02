package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageStat

fun List<AppUsageStat>.toTopAppUiModels(context: Context): List<TopAppUiModel> {
    val totalDurationMillis = sumOf { it.totalTimeMillis }
    return map { app ->
        TopAppUiModel(
            name = app.appName ?: app.packageName,
            durationText = app.totalTimeMillis.toDashboardDurationText(),
            progressRatio = if (totalDurationMillis <= 0L) 0f else {
                app.totalTimeMillis.toFloat() / totalDurationMillis.toFloat()
            },
            icon = context.packageManager.getApplicationIconOrNull(app.packageName)
                ?: ContextCompat.getDrawable(context, R.drawable.smartphone)
        )
    }
}

fun Long.toDashboardDurationText(): String {
    val totalMinutes = this / (60L * 1000L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun PackageManager.getApplicationIconOrNull(packageName: String): Drawable? =
    try {
        getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
