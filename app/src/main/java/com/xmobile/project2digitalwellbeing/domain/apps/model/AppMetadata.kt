package com.xmobile.project2digitalwellbeing.domain.apps.model

data class AppMetadata(
    val packageName: String,
    val appName: String?,
    val sourceCategory: SourceAppCategory,
    val reportingCategory: AppCategory,
    val classificationSource: ClassificationSource,
    val confidence: Float
) {
    val category: AppCategory
        get() = reportingCategory
}
