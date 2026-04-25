package com.xmobile.project2digitalwellbeing.data.usage.mapper

import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity.AppMetadataEntity
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory

fun AppMetadataEntity.toDomain(): AppMetadata {
    return AppMetadata(
        packageName = packageName,
        appName = appName,
        sourceCategory = SourceAppCategory.valueOf(sourceCategory),
        reportingCategory = AppCategory.valueOf(reportingCategory),
        classificationSource = ClassificationSource.valueOf(classificationSource),
        confidence = confidence
    )
}

fun AppMetadata.toEntity(updatedAtMillis: Long): AppMetadataEntity {
    return AppMetadataEntity(
        packageName = packageName,
        appName = appName,
        sourceCategory = sourceCategory.name,
        reportingCategory = reportingCategory.name,
        classificationSource = classificationSource.name,
        confidence = confidence,
        updatedAtMillis = updatedAtMillis
    )
}
