package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage
import kotlin.math.ceil

internal object DashboardChartConfigurator {

    fun configure(chart: LineChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            marker = DashboardUsageMarkerView(context)
            renderer = DashboardLineChartRenderer(this, animator, viewPortHandler, context)
            setTouchEnabled(true)
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setViewPortOffsets(56f, 16f, 20f, 44f)
            axisRight.isEnabled = false
            setNoDataText("No hourly data yet")
            axisLeft.apply {
                axisMinimum = 0f
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                textSize = 12f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawZeroLine(false)
                labelCount = 5
                xOffset = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
                }
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                axisMinimum = 0f
                axisMaximum = 23f
                textSize = 12f
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                setDrawGridLines(false)
                setDrawAxisLine(false)
                yOffset = 8f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (value.toInt()) {
                            0 -> "12AM"
                            9 -> "9AM"
                            18 -> "6PM"
                            else -> ""
                        }
                    }
                }
            }
        }
    }

    fun render(chart: LineChart, context: Context, hourlyUsage: List<HourlyUsage>) {
        val entries = hourlyUsage.map { hour ->
            Entry(hour.hourOfDay.toFloat(), hour.totalTimeMillis / (60f * 1000f))
        }

        chart.axisLeft.axisMaximum = entries.resolveAxisMaximum()
        chart.data = LineData(buildDataSet(context, entries)).apply {
            setDrawValues(false)
        }
        chart.invalidate()
    }

    private fun buildDataSet(context: Context, entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, "").apply {
            color = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawCircles(false)
            setDrawCircleHole(false)
            setDrawHighlightIndicators(true)
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(true)
            highLightColor = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
            highlightLineWidth = 1f
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(context, R.drawable.bg_chart_usage_fill)
        }
    }

    private fun List<Entry>.resolveAxisMaximum(): Float {
        val peak = maxOfOrNull { it.y } ?: 0f
        val normalizedPeak = ceil(peak / 25f) * 25f
        return maxOf(100f, normalizedPeak)
    }
}
