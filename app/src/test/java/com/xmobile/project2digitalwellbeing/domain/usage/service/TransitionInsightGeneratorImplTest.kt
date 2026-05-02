package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.TransitionFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransitionInsightGeneratorImplTest {

    private val generator = TransitionInsightGeneratorImpl()

    @Test
    fun `filterTransitions keeps only productive to productive edges`() {
        val filtered = generator.filterTransitions(
            transitions = sampleTransitions(),
            filter = TransitionFilter.PRODUCTIVE
        )

        assertEquals(1, filtered.size)
        assertEquals("app.docs", filtered.first().fromPackageName)
        assertEquals("app.notes", filtered.first().toPackageName)
    }

    @Test
    fun `filterTransitions keeps only distracting to distracting edges`() {
        val filtered = generator.filterTransitions(
            transitions = sampleTransitions(),
            filter = TransitionFilter.DISTRACTING
        )

        assertEquals(1, filtered.size)
        assertEquals("app.youtube", filtered.first().fromPackageName)
        assertEquals("app.tiktok", filtered.first().toPackageName)
    }

    @Test
    fun `generateInsight returns dominant transition summary for selected filter`() {
        val insight = generator.generateInsight(
            transitions = sampleTransitions(),
            filter = TransitionFilter.DISTRACTING
        )

        assertNotNull(insight)
        assertEquals(TransitionFilter.DISTRACTING, insight?.filter)
        assertEquals("Most common distracting switch", insight?.title)
        assertEquals("app.youtube", insight?.dominantTransition?.fromPackageName)
        assertEquals("app.tiktok", insight?.dominantTransition?.toPackageName)
        assertEquals(0.6f, insight?.lateNightTransitionRatio ?: 0f, 0.0001f)
        assertEquals(5, insight?.totalTransitionCount)
        assertEquals(12_000L, insight?.averageIntervalMillis)
        assertEquals(87, insight?.score)
        assertEquals(0.6491f, insight?.confidence ?: 0f, 0.0001f)
    }

    @Test
    fun `generateInsight returns null when filter has no matching transitions`() {
        val insight = generator.generateInsight(
            transitions = listOf(
                transition(
                    fromPackage = "app.chrome",
                    fromCategory = AppCategory.BROWSER,
                    toPackage = "app.whatsapp",
                    toCategory = AppCategory.COMMUNICATION,
                    transitionCount = 3
                )
            ),
            filter = TransitionFilter.PRODUCTIVE
        )

        assertNull(insight)
    }

    @Test
    fun `filterTransitions keeps mixed distracting edges when either side is distracting`() {
        val filtered = generator.filterTransitions(
            transitions = sampleTransitions() + transition(
                fromPackage = "app.chrome",
                fromAppName = "Chrome",
                fromCategory = AppCategory.BROWSER,
                toPackage = "app.youtube",
                toAppName = "YouTube",
                toCategory = AppCategory.VIDEO,
                transitionCount = 1
            ),
            filter = TransitionFilter.DISTRACTING_MIXED
        )

        assertEquals(2, filtered.size)
    }

    private fun sampleTransitions(): List<AppTransitionStat> {
        return listOf(
            transition(
                fromPackage = "app.docs",
                fromAppName = "Docs",
                fromCategory = AppCategory.PRODUCTIVITY,
                toPackage = "app.notes",
                toAppName = "Notes",
                toCategory = AppCategory.TOOLS,
                transitionCount = 4,
                totalIntervalMillis = 36_000L,
                lateNightTransitionCount = 1,
                lastTransitionTimestampMillis = 200_000L
            ),
            transition(
                fromPackage = "app.youtube",
                fromAppName = "YouTube",
                fromCategory = AppCategory.VIDEO,
                toPackage = "app.tiktok",
                toAppName = "TikTok",
                toCategory = AppCategory.SOCIAL,
                transitionCount = 5,
                totalIntervalMillis = 60_000L,
                lateNightTransitionCount = 3,
                lastTransitionTimestampMillis = 300_000L
            ),
            transition(
                fromPackage = "app.chrome",
                fromAppName = "Chrome",
                fromCategory = AppCategory.BROWSER,
                toPackage = "app.docs",
                toAppName = "Docs",
                toCategory = AppCategory.PRODUCTIVITY,
                transitionCount = 2,
                totalIntervalMillis = 20_000L,
                lateNightTransitionCount = 0,
                lastTransitionTimestampMillis = 150_000L
            )
        )
    }

    private fun transition(
        fromPackage: String,
        fromAppName: String? = null,
        fromCategory: AppCategory,
        toPackage: String,
        toAppName: String? = null,
        toCategory: AppCategory,
        transitionCount: Int,
        totalIntervalMillis: Long = 0L,
        lateNightTransitionCount: Int = 0,
        lastTransitionTimestampMillis: Long = 0L
    ): AppTransitionStat {
        return AppTransitionStat(
            fromPackageName = fromPackage,
            fromAppName = fromAppName,
            fromCategory = fromCategory,
            toPackageName = toPackage,
            toAppName = toAppName,
            toCategory = toCategory,
            transitionCount = transitionCount,
            averageIntervalMillis = if (transitionCount == 0) 0L else totalIntervalMillis / transitionCount,
            totalIntervalMillis = totalIntervalMillis,
            lateNightTransitionCount = lateNightTransitionCount,
            lastTransitionTimestampMillis = lastTransitionTimestampMillis
        )
    }
}
