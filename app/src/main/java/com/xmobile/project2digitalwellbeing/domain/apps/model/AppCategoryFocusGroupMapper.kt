package com.xmobile.project2digitalwellbeing.domain.apps.model

fun AppCategory.toFocusGroup(): AppFocusGroup {
    return when (this) {
        AppCategory.PRODUCTIVITY,
        AppCategory.EDUCATION,
        AppCategory.TOOLS -> AppFocusGroup.PRODUCTIVE

        AppCategory.SOCIAL,
        AppCategory.VIDEO,
        AppCategory.GAME,
        AppCategory.MUSIC -> AppFocusGroup.DISTRACTING

        AppCategory.COMMUNICATION,
        AppCategory.BROWSER,
        AppCategory.SYSTEM,
        AppCategory.OTHER,
        AppCategory.UNKNOWN -> AppFocusGroup.NEUTRAL
    }
}
