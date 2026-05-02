package com.xmobile.project2digitalwellbeing.data.apps.mapper

import com.xmobile.project2digitalwellbeing.data.apps.source.local.room.entity.AppMetadataEntity
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppMetadata
import com.xmobile.project2digitalwellbeing.domain.apps.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.SourceAppCategory

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
