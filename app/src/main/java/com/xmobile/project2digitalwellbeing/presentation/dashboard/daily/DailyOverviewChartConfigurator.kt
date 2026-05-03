package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import kotlin.math.ceil

internal object DailyOverviewChartConfigurator {

    fun configure(chart: BarChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setFitBars(true)
            minOffset = 0f
            extraLeftOffset = 12f
            extraTopOffset = 8f
            extraRightOffset = 12f
            extraBottomOffset = 12f
            axisRight.isEnabled = false
            setNoDataText("No hourly data yet")

            axisLeft.apply {
                axisMinimum = 0f
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                textSize = 12f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                labelCount = 5
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
                }
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                axisMinimum = -0.5f
                axisMaximum = 23.5f
                labelCount = 24
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                textSize = 12f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setAvoidFirstLastClipping(false)
                yOffset = 8f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (value.toInt()) {
                            0 -> "12AM"
                            4 -> "4AM"
                            8 -> "8AM"
                            12 -> "12PM"
                            16 -> "4PM"
                            20 -> "8PM"
                            else -> ""
                        }
                    }
                }
            }
        }
    }

    fun render(chart: BarChart, context: Context, hourlyUsage: List<HourlyUsage>) {
        val usageByHour = hourlyUsage.associateBy { it.hourOfDay }
        val entries = (0..23).map { hour ->
            BarEntry(hour.toFloat(), (usageByHour[hour]?.totalTimeMillis ?: 0L) / (60f * 1000f))
        }

        chart.axisLeft.axisMaximum = entries.resolveAxisMaximum()
        chart.data = BarData(buildDataSet(context, entries)).apply {
            barWidth = 0.62f
            setDrawValues(false)
        }
        chart.invalidate()
    }

    private fun buildDataSet(context: Context, entries: List<BarEntry>): BarDataSet {
        val highlightColor = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
        val defaultColor = ContextCompat.getColor(context, R.color.weekly_overview_divider)
        val maxY = entries.maxOfOrNull { it.y } ?: 0f
        return BarDataSet(entries, "Hourly usage").apply {
            colors = entries.map { entry ->
                if (entry.y >= maxY && maxY > 0f) highlightColor else defaultColor
            }
            highLightAlpha = 0
            setDrawValues(false)
        }
    }

    private fun List<BarEntry>.resolveAxisMaximum(): Float {
        val peak = maxOfOrNull { it.y } ?: 0f
        val normalizedPeak = ceil(peak / 15f) * 15f
        return maxOf(60f, normalizedPeak)
    }
}
