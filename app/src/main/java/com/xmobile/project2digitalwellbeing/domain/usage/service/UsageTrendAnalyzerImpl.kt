package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.DailyUsage
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrend
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageTrendDirection
import com.xmobile.project2digitalwellbeing.domain.usage.model.WeeklyUsage
import javax.inject.Inject
import kotlin.math.abs

class UsageTrendAnalyzerImpl @Inject constructor() : UsageTrendAnalyzer {

    override fun compareDaily(current: DailyUsage, previous: DailyUsage?): UsageTrend {
        return buildTrend(
            currentValueMillis = current.totalScreenTimeMillis,
            previousValueMillis = previous?.totalScreenTimeMillis ?: 0L,
            periodLabel = "daily"
        )
    }

    override fun compareWeekly(current: WeeklyUsage, previous: WeeklyUsage?): UsageTrend {
        return buildTrend(
            currentValueMillis = current.totalScreenTimeMillis,
            previousValueMillis = previous?.totalScreenTimeMillis ?: 0L,
            periodLabel = "weekly"
        )
    }

    private fun buildTrend(
        currentValueMillis: Long,
        previousValueMillis: Long,
        periodLabel: String
    ): UsageTrend {
        val deltaMillis = currentValueMillis - previousValueMillis
        val deltaRatio = safeRatio(deltaMillis, previousValueMillis)
        val direction = when {
            abs(deltaMillis) < 60_000L -> UsageTrendDirection.FLAT
            deltaMillis > 0L -> UsageTrendDirection.UP
            else -> UsageTrendDirection.DOWN
        }

        return UsageTrend(
            currentValueMillis = currentValueMillis,
            previousValueMillis = previousValueMillis,
            deltaMillis = deltaMillis,
            deltaRatio = deltaRatio,
            direction = direction,
            summary = buildSummary(
                direction = direction,
                deltaRatio = deltaRatio,
                periodLabel = periodLabel
            )
        )
    }

    private fun buildSummary(
        direction: UsageTrendDirection,
        deltaRatio: Float,
        periodLabel: String
    ): String {
        val ratioPercent = (abs(deltaRatio) * 100f).toInt()
        return when (direction) {
            UsageTrendDirection.UP -> "$periodLabel usage is up $ratioPercent% from the previous period."
            UsageTrendDirection.DOWN -> "$periodLabel usage is down $ratioPercent% from the previous period."
            UsageTrendDirection.FLAT -> "$periodLabel usage is stable compared with the previous period."
        }
    }

    private fun safeRatio(deltaMillis: Long, previousValueMillis: Long): Float {
        if (previousValueMillis <= 0L) {
            return if (deltaMillis > 0L) 1f else 0f
        }
        return deltaMillis.toFloat() / previousValueMillis.toFloat()
    }
}
