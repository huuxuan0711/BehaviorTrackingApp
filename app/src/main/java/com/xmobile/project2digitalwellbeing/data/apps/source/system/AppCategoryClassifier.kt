package com.xmobile.project2digitalwellbeing.data.apps.source.system

import com.xmobile.project2digitalwellbeing.domain.apps.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.apps.model.SourceAppCategory

internal object AppCategoryClassifier {
    private val packageTokenDelimiters = charArrayOf('.', '_', '-')
    private val appNameTokenDelimiters = charArrayOf(' ', '-', '_')

    fun resolve(
        packageName: String,
        appName: String?,
        systemCategory: SourceAppCategory?
    ): CategoryResolution {
        return resolveFromOverride(packageName)
            ?: resolveFromSystemCategory(systemCategory)
            ?: resolveFromHeuristic(packageName = packageName, appName = appName)
            ?: unknownCategoryResolution()
    }

    private fun resolveFromOverride(packageName: String): CategoryResolution? {
        val sourceCategory = APP_CATEGORY_OVERRIDES[packageName] ?: return null
        return sourceCategory.toResolution(
            classificationSource = ClassificationSource.MANUAL_OVERRIDE,
            confidence = 1.0f
        )
    }

    private fun resolveFromSystemCategory(systemCategory: SourceAppCategory?): CategoryResolution? {
        val sourceCategory = systemCategory?.takeUnless { it == SourceAppCategory.UNKNOWN } ?: return null
        return sourceCategory.toResolution(
            classificationSource = ClassificationSource.SYSTEM_CATEGORY,
            confidence = 0.9f
        )
    }

    private fun resolveFromHeuristic(packageName: String, appName: String?): CategoryResolution? {
        val sourceCategory = inferCategory(packageName = packageName, appName = appName)
            .takeUnless { it == SourceAppCategory.UNKNOWN }
            ?: return null

        return sourceCategory.toResolution(
            classificationSource = ClassificationSource.HEURISTIC,
            confidence = 0.55f
        )
    }

    private fun inferCategory(packageName: String, appName: String?): SourceAppCategory {
        val packageIndex = buildNormalizedIndex(packageName, packageTokenDelimiters)
        val appNameIndex = buildNormalizedIndex(appName.orEmpty(), appNameTokenDelimiters)

        APP_CATEGORY_KEYWORD_RULES.forEach { rule ->
            if (rule.matches(packageIndex = packageIndex, appNameIndex = appNameIndex)) {
                return rule.sourceCategory
            }
        }

        return SourceAppCategory.UNKNOWN
    }

    private fun buildNormalizedIndex(input: String, delimiters: CharArray): NormalizedTextIndex {
        val normalized = input.lowercase()
        val tokens = normalized
            .split(*delimiters)
            .filter { it.isNotBlank() }
            .toSet()

        return NormalizedTextIndex(
            normalized = normalized,
            tokens = tokens
        )
    }

    private data class NormalizedTextIndex(
        val normalized: String,
        val tokens: Set<String>
    )

    private fun CategoryKeywordRule.matches(
        packageIndex: NormalizedTextIndex,
        appNameIndex: NormalizedTextIndex
    ): Boolean {
        return keywords.any { keyword ->
            keyword in packageIndex.tokens ||
                keyword in appNameIndex.tokens ||
                packageIndex.normalized.contains(keyword) ||
                appNameIndex.normalized.contains(keyword)
        }
    }
}
