package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.EnrichedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionExtractorImplTest {

    private val extractor = TransitionExtractorImpl()

    @Test
    fun `extractTransitions aggregates repeated transitions between the same apps`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 21, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 90_000L, 120_000L, 21, false),
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 180_000L, 240_000L, 22, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 300_000L, 360_000L, 22, false)
            )
        )

        val transition = transitions.first { it.fromPackageName == "app.a" && it.toPackageName == "app.b" }
        assertEquals(2, transition.transitionCount)
        assertEquals(45_000L, transition.averageIntervalMillis)
        assertEquals(90_000L, transition.totalIntervalMillis)
    }

    @Test
    fun `extractTransitions ignores consecutive sessions from the same app`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 21, false),
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 80_000L, 100_000L, 21, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 120_000L, 180_000L, 22, false)
            )
        )

        assertEquals(1, transitions.size)
        assertEquals("app.a", transitions.first().fromPackageName)
        assertEquals("app.b", transitions.first().toPackageName)
    }

    @Test
    fun `extractTransitions tracks late night transition count`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 23, true),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 70_000L, 120_000L, 0, true),
                enrichedSession("app.c", "App C", AppCategory.GAME, 180_000L, 240_000L, 10, false)
            )
        )

        val lateNightTransition = transitions.first { it.fromPackageName == "app.a" && it.toPackageName == "app.b" }
        assertEquals(1, lateNightTransition.lateNightTransitionCount)
        val normalTransition = transitions.first { it.fromPackageName == "app.b" && it.toPackageName == "app.c" }
        assertEquals(1, normalTransition.lateNightTransitionCount)
    }

    @Test
    fun `extractTransitions returns empty when fewer than two sessions exist`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 21, false)
            )
        )

        assertTrue(transitions.isEmpty())
    }

    @Test
    fun `extractTransitions sorts input before building edges`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 120_000L, 180_000L, 22, false),
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 60_000L, 21, false),
                enrichedSession("app.c", "App C", AppCategory.GAME, 240_000L, 300_000L, 22, false)
            )
        )

        assertEquals(2, transitions.size)
        assertEquals("app.a", transitions[0].fromPackageName)
        assertEquals("app.b", transitions[0].toPackageName)
        assertEquals("app.b", transitions[1].fromPackageName)
        assertEquals("app.c", transitions[1].toPackageName)
    }

    @Test
    fun `extractTransitions clamps overlap intervals to zero and skips invalid sessions`() {
        val transitions = extractor.extractTransitions(
            sessions = listOf(
                enrichedSession("app.a", "App A", AppCategory.SOCIAL, 0L, 90_000L, 21, false),
                enrichedSession("app.b", "App B", AppCategory.VIDEO, 60_000L, 120_000L, 21, false),
                enrichedSession(" ", "Blank", AppCategory.OTHER, 150_000L, 210_000L, 21, false),
                enrichedSession("app.c", "App C", AppCategory.GAME, 300_000L, 300_000L, 22, false)
            )
        )

        assertEquals(1, transitions.size)
        assertEquals(0L, transitions.first().averageIntervalMillis)
        assertEquals(0L, transitions.first().totalIntervalMillis)
    }

    private fun enrichedSession(
        packageName: String,
        appName: String,
        category: AppCategory,
        startMillis: Long,
        endMillis: Long,
        hourOfDay: Int,
        isLateNight: Boolean
    ): EnrichedSession {
        return EnrichedSession(
            session = AppSession(
                packageName = packageName,
                startTimeMillis = startMillis,
                endTimeMillis = endMillis,
                durationMillis = endMillis - startMillis
            ),
            appName = appName,
            category = category,
            hourOfDay = hourOfDay,
            isLateNight = isLateNight
        )
    }
}
