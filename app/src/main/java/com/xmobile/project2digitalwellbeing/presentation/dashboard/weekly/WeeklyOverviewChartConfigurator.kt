package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.xmobile.project2digitalwellbeing.R
import kotlin.math.ceil

internal object WeeklyOverviewChartConfigurator {

    fun configure(chart: BarChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(false)
            minOffset = 0f
            extraLeftOffset = 12f
            extraTopOffset = 8f
            extraRightOffset = 12f
            extraBottomOffset = 12f
            axisRight.isEnabled = false
            setNoDataText("No weekly data yet")

            axisLeft.apply {
                axisMinimum = 0f
                setDrawLabels(false)
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = 7
                axisMinimum = -0.5f
                axisMaximum = 6.5f
                setCenterAxisLabels(false)
                setAvoidFirstLastClipping(false)
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                textSize = 12f
                yOffset = 8f
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }
        }
    }

    fun render(chart: BarChart, context: Context, bars: List<WeeklyChartBarUiModel>) {
        val safeBars = if (bars.size == 7) bars else defaultBars()
        val entries = safeBars.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.durationMinutes)
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return safeBars.getOrNull(index)?.label.orEmpty()
            }
        }
        chart.axisLeft.axisMaximum = entries.resolveAxisMaximum()
        chart.data = BarData(buildDataSet(context, safeBars, entries)).apply {
            barWidth = 0.58f
            setDrawValues(false)
        }
        chart.invalidate()
    }

    private fun buildDataSet(
        context: Context,
        bars: List<WeeklyChartBarUiModel>,
        entries: List<BarEntry>
    ): BarDataSet {
        val highlightColor = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
        val defaultColor = ContextCompat.getColor(context, R.color.weekly_overview_divider)
        return BarDataSet(entries, "Weekly usage").apply {
            colors = bars.map { if (it.isHighlighted) highlightColor else defaultColor }
            highLightAlpha = 0
            setDrawValues(false)
        }
    }

    private fun List<BarEntry>.resolveAxisMaximum(): Float {
        val peak = maxOfOrNull { it.y } ?: 0f
        val normalizedPeak = ceil(peak / 30f) * 30f
        return maxOf(60f, normalizedPeak)
    }

    private fun defaultBars(): List<WeeklyChartBarUiModel> {
        return listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { label ->
            WeeklyChartBarUiModel(label = label, durationMinutes = 0f, isHighlighted = false)
        }
    }
}
