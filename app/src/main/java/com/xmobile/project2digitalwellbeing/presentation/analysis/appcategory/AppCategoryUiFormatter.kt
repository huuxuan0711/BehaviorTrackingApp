package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.graphics.Color
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup

internal fun AppCategory.toDisplayName(): String {
    return when (this) {
        AppCategory.SOCIAL -> "Social"
        AppCategory.VIDEO -> "Video"
        AppCategory.PRODUCTIVITY -> "Productivity"
        AppCategory.COMMUNICATION -> "Communication"
        AppCategory.GAME -> "Game"
        AppCategory.BROWSER -> "Browser"
        AppCategory.MUSIC -> "Music"
        AppCategory.EDUCATION -> "Education"
        AppCategory.TOOLS -> "Tools"
        AppCategory.SYSTEM -> "System"
        AppCategory.OTHER -> "Other"
        AppCategory.UNKNOWN -> "Unknown"
    }
}

internal fun AppCategory.toBadgeColor(): Int {
    return when (this) {
        AppCategory.PRODUCTIVITY,
        AppCategory.EDUCATION,
        AppCategory.TOOLS -> Color.parseColor("#2E7D32")

        AppCategory.SOCIAL,
        AppCategory.VIDEO,
        AppCategory.GAME,
        AppCategory.MUSIC -> Color.parseColor("#E65100")

        AppCategory.COMMUNICATION,
        AppCategory.BROWSER,
        AppCategory.SYSTEM,
        AppCategory.OTHER,
        AppCategory.UNKNOWN -> Color.parseColor("#424242")
    }
}

internal fun AppFocusGroup.toDisplayName(): String {
    return when (this) {
        AppFocusGroup.PRODUCTIVE -> "Productive"
        AppFocusGroup.DISTRACTING -> "Distracting"
        AppFocusGroup.NEUTRAL -> "Neutral"
    }
}

internal fun AppFocusGroup.toBadgeColor(): Int {
    return when (this) {
        AppFocusGroup.PRODUCTIVE -> Color.parseColor("#2E7D32")
        AppFocusGroup.DISTRACTING -> Color.parseColor("#E65100")
        AppFocusGroup.NEUTRAL -> Color.parseColor("#424242")
    }
}
