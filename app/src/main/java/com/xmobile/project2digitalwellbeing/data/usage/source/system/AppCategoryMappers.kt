package com.xmobile.project2digitalwellbeing.data.usage.source.system

import android.content.pm.ApplicationInfo
import android.os.Build
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory

internal data class CategoryResolution(
    val sourceCategory: SourceAppCategory,
    val reportingCategory: AppCategory,
    val classificationSource: ClassificationSource,
    val confidence: Float
)

internal fun ApplicationInfo.toSystemBackedCategory(): SourceAppCategory {
    val platformCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) category else null
    return when (platformCategory) {
        ApplicationInfo.CATEGORY_GAME -> SourceAppCategory.GAME
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> SourceAppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_SOCIAL -> SourceAppCategory.SOCIAL_NETWORKING
        ApplicationInfo.CATEGORY_AUDIO -> SourceAppCategory.MUSIC_AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> SourceAppCategory.VIDEO_STREAMING
        ApplicationInfo.CATEGORY_IMAGE -> SourceAppCategory.CREATIVITY
        ApplicationInfo.CATEGORY_MAPS -> SourceAppCategory.TOOLS_UTILITIES
        ApplicationInfo.CATEGORY_NEWS -> SourceAppCategory.READING_INFORMATION
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> SourceAppCategory.TOOLS_UTILITIES
        else -> SourceAppCategory.UNKNOWN
    }
}

internal fun SourceAppCategory.toReportingCategory(): AppCategory {
    return when (this) {
        SourceAppCategory.SOCIAL_NETWORKING -> AppCategory.SOCIAL
        SourceAppCategory.MESSAGING -> AppCategory.COMMUNICATION
        SourceAppCategory.VIDEO_STREAMING -> AppCategory.VIDEO
        SourceAppCategory.BROWSER -> AppCategory.BROWSER
        SourceAppCategory.MUSIC_AUDIO -> AppCategory.MUSIC
        SourceAppCategory.PRODUCTIVITY,
        SourceAppCategory.FINANCE -> AppCategory.PRODUCTIVITY
        SourceAppCategory.EDUCATION -> AppCategory.EDUCATION
        SourceAppCategory.GAME -> AppCategory.GAME
        SourceAppCategory.TOOLS_UTILITIES -> AppCategory.TOOLS
        SourceAppCategory.READING_INFORMATION -> AppCategory.OTHER
        SourceAppCategory.CREATIVITY -> AppCategory.OTHER
        SourceAppCategory.OTHER,
        SourceAppCategory.UNKNOWN -> AppCategory.UNKNOWN
    }
}

internal fun SourceAppCategory.toResolution(
    classificationSource: ClassificationSource,
    confidence: Float
): CategoryResolution {
    return CategoryResolution(
        sourceCategory = this,
        reportingCategory = toReportingCategory(),
        classificationSource = classificationSource,
        confidence = confidence
    )
}

internal fun unknownCategoryResolution(): CategoryResolution {
    return CategoryResolution(
        sourceCategory = SourceAppCategory.UNKNOWN,
        reportingCategory = AppCategory.UNKNOWN,
        classificationSource = ClassificationSource.UNKNOWN,
        confidence = 0f
    )
}
