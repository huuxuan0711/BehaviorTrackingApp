package com.xmobile.project2digitalwellbeing.data.usage.source.system

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.ClassificationSource
import com.xmobile.project2digitalwellbeing.domain.usage.model.SourceAppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryClassifierTest {

    @Test
    fun `override takes precedence over system category`() {
        val resolution = AppCategoryClassifier.resolve(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            systemCategory = SourceAppCategory.PRODUCTIVITY
        )

        assertEquals(SourceAppCategory.VIDEO_STREAMING, resolution.sourceCategory)
        assertEquals(AppCategory.VIDEO, resolution.reportingCategory)
        assertEquals(ClassificationSource.MANUAL_OVERRIDE, resolution.classificationSource)
        assertEquals(1.0f, resolution.confidence)
    }

    @Test
    fun `system category is used when override is missing`() {
        val resolution = AppCategoryClassifier.resolve(
            packageName = "com.example.unknownapp",
            appName = "Example",
            systemCategory = SourceAppCategory.GAME
        )

        assertEquals(SourceAppCategory.GAME, resolution.sourceCategory)
        assertEquals(AppCategory.GAME, resolution.reportingCategory)
        assertEquals(ClassificationSource.SYSTEM_CATEGORY, resolution.classificationSource)
        assertEquals(0.9f, resolution.confidence)
    }

    @Test
    fun `heuristic falls back to package and app name`() {
        val resolution = AppCategoryClassifier.resolve(
            packageName = "com.example.customclient",
            appName = "Telegram Lite",
            systemCategory = null
        )

        assertEquals(SourceAppCategory.MESSAGING, resolution.sourceCategory)
        assertEquals(AppCategory.COMMUNICATION, resolution.reportingCategory)
        assertEquals(ClassificationSource.HEURISTIC, resolution.classificationSource)
        assertEquals(0.55f, resolution.confidence)
    }

    @Test
    fun `unknown is returned when no signal matches`() {
        val resolution = AppCategoryClassifier.resolve(
            packageName = "com.acme.novapulse",
            appName = "Nova Pulse",
            systemCategory = null
        )

        assertEquals(SourceAppCategory.UNKNOWN, resolution.sourceCategory)
        assertEquals(AppCategory.UNKNOWN, resolution.reportingCategory)
        assertEquals(ClassificationSource.UNKNOWN, resolution.classificationSource)
        assertEquals(0f, resolution.confidence)
    }
}
