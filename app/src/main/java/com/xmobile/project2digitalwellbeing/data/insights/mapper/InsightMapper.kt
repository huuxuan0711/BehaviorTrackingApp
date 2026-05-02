package com.xmobile.project2digitalwellbeing.data.insights.mapper

import com.xmobile.project2digitalwellbeing.data.insights.source.local.room.entity.InsightEntity
import com.xmobile.project2digitalwellbeing.domain.insights.model.Insight
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightType

fun InsightEntity.toDomain(): Insight {
    val evidence = evidenceJson
        .split("|")
        .filter { it.contains("=") }
        .associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }

    val relatedPackages = relatedPackagesCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return Insight(
        type = InsightType.valueOf(type),
        score = score,
        confidence = confidence,
        evidence = evidence,
        relatedPackages = relatedPackages
    )
}

fun Insight.toEntity(windowStartMillis: Long, windowEndMillis: Long): InsightEntity {
    val evidenceJson = evidence.entries.joinToString(separator = "|") { "${it.key}=${it.value}" }
    val relatedPackagesCsv = relatedPackages.joinToString(separator = ",")

    return InsightEntity(
        type = type.name,
        score = score,
        confidence = confidence,
        evidenceJson = evidenceJson,
        relatedPackagesCsv = relatedPackagesCsv,
        windowStartMillis = windowStartMillis,
        windowEndMillis = windowEndMillis
    )
}
