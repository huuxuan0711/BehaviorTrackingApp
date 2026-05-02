package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppTransitionStat
import com.xmobile.project2digitalwellbeing.domain.usage.model.Insight
import com.xmobile.project2digitalwellbeing.domain.usage.model.InsightType
import com.xmobile.project2digitalwellbeing.domain.usage.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.model.TransitionInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightComposerImplTest {

    private val composer = InsightComposerImpl()

    @Test
    fun `compose generates late night distracting switching insight`() {
        val composed = composer.compose(
            usageInsights = listOf(
                insight(InsightType.LATE_NIGHT_SWITCHING),
                insight(InsightType.FREQUENT_SWITCHING)
            ),
            transitionInsight = transitionInsight(
                filter = TransitionFilter.DISTRACTING,
                lateNightTransitionRatio = 0.65f,
                totalTransitionCount = 12,
                score = 78,
                confidence = 0.82f
            )
        )

        assertEquals(2, composed.size)
        assertEquals("Late-night distracting switching", composed.first().title)
        assertEquals(listOf("app.youtube", "app.tiktok"), composed.first().relatedPackages)
    }

    @Test
    fun `compose returns empty when transition insight is missing`() {
        val composed = composer.compose(
            usageInsights = listOf(insight(InsightType.FREQUENT_SWITCHING)),
            transitionInsight = null
        )

        assertTrue(composed.isEmpty())
    }

    @Test
    fun `compose skips late night composition when transition filter is not distracting`() {
        val composed = composer.compose(
            usageInsights = listOf(insight(InsightType.LATE_NIGHT_SWITCHING)),
            transitionInsight = transitionInsight(
                filter = TransitionFilter.PRODUCTIVE,
                lateNightTransitionRatio = 0.8f,
                totalTransitionCount = 12,
                score = 70,
                confidence = 0.8f
            )
        )

        assertEquals(0, composed.size)
    }

    private fun insight(type: InsightType): Insight {
        return Insight(
            type = type,
            score = 70,
            confidence = 0.8f,
            evidence = emptyMap(),
            relatedPackages = emptyList()
        )
    }

    private fun transitionInsight(
        filter: TransitionFilter,
        lateNightTransitionRatio: Float,
        totalTransitionCount: Int,
        score: Int,
        confidence: Float
    ): TransitionInsight {
        return TransitionInsight(
            filter = filter,
            title = "test",
            summary = "test",
            score = score,
            confidence = confidence,
            totalTransitionCount = totalTransitionCount,
            lateNightTransitionRatio = lateNightTransitionRatio,
            averageIntervalMillis = 14_000L,
            dominantTransition = AppTransitionStat(
                fromPackageName = "app.youtube",
                fromAppName = "YouTube",
                fromCategory = AppCategory.VIDEO,
                toPackageName = "app.tiktok",
                toAppName = "TikTok",
                toCategory = AppCategory.SOCIAL,
                transitionCount = 5,
                averageIntervalMillis = 12_000L,
                totalIntervalMillis = 60_000L,
                lateNightTransitionCount = 3,
                lastTransitionTimestampMillis = 300_000L
            )
        )
    }
}
