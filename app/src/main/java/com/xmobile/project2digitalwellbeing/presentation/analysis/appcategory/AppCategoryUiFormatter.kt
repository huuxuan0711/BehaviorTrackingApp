package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup

@StringRes
internal fun AppCategory.getDisplayNameRes(): Int {
    return when (this) {
        AppCategory.SOCIAL -> R.string.auto_social
        AppCategory.VIDEO -> R.string.auto_video
        AppCategory.PRODUCTIVITY -> R.string.auto_productivity
        AppCategory.COMMUNICATION -> R.string.auto_communication
        AppCategory.GAME -> R.string.auto_game
        AppCategory.BROWSER -> R.string.auto_browser
        AppCategory.MUSIC -> R.string.auto_music
        AppCategory.EDUCATION -> R.string.auto_education
        AppCategory.TOOLS -> R.string.auto_tools
        AppCategory.SYSTEM -> R.string.auto_system
        AppCategory.OTHER -> R.string.auto_other
        AppCategory.UNKNOWN -> R.string.auto_unknown
    }
}

@ColorRes
internal fun AppCategory.getBadgeColorRes(): Int {
    return when (this) {
        AppCategory.PRODUCTIVITY,
        AppCategory.EDUCATION,
        AppCategory.TOOLS -> R.color.auto_color_2e7d32

        AppCategory.SOCIAL,
        AppCategory.VIDEO,
        AppCategory.GAME,
        AppCategory.MUSIC -> R.color.auto_color_e65100

        AppCategory.COMMUNICATION,
        AppCategory.BROWSER,
        AppCategory.SYSTEM,
        AppCategory.OTHER,
        AppCategory.UNKNOWN -> R.color.auto_color_424242
    }
}

@StringRes
internal fun AppFocusGroup.getDisplayNameRes(): Int {
    return when (this) {
        AppFocusGroup.PRODUCTIVE -> R.string.auto_productive
        AppFocusGroup.DISTRACTING -> R.string.auto_distracting
        AppFocusGroup.NEUTRAL -> R.string.auto_neutral
    }
}

@ColorRes
internal fun AppFocusGroup.getBadgeColorRes(): Int {
    return when (this) {
        AppFocusGroup.PRODUCTIVE -> R.color.auto_color_2e7d32
        AppFocusGroup.DISTRACTING -> R.color.auto_color_e65100
        AppFocusGroup.NEUTRAL -> R.color.auto_color_424242
    }
}
