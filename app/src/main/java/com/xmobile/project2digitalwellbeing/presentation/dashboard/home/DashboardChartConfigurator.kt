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
            minOffset = 0f
            extraLeftOffset = 8f
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
                setDrawZeroLine(false)
                labelCount = 5
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = "${value.toInt()}m"
                }
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true // Đảm bảo luôn nhảy theo từng đơn vị giờ
                axisMinimum = 0f
                axisMaximum = 23f
                labelCount = 24 // Cho phép hệ thống xem xét tất cả các vị trí
                textSize = 12f
                textColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
                setDrawGridLines(false)
                setDrawAxisLine(false)
                yOffset = 12f
            }
        }
    }

    fun render(chart: LineChart, context: Context, hourlyUsage: List<HourlyUsage>) {
        val currentHour = hourlyUsage.lastOrNull()?.hourOfDay
        chart.marker = DashboardUsageMarkerView(context, currentHour)

        val entries = hourlyUsage.mapIndexed { index, usage ->
            Entry(index.toFloat(), usage.totalTimeMillis / (60f * 1000f))
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index < 0 || index >= hourlyUsage.size) return ""
                
                // Hiển thị nhãn tại mốc bắt đầu (index 0), mốc kết thúc (index cuối) 
                // và các mốc cách nhau 6 tiếng (6, 12, 18)
                val isMajorTick = index == 0 || index == hourlyUsage.size - 1 || index % 6 == 0
                
                if (!isMajorTick) return ""

                val hour = hourlyUsage[index].hourOfDay
                return when {
                    hour == 0 -> "12AM"
                    hour == 12 -> "12PM"
                    hour > 12 -> "${hour - 12}PM"
                    else -> "${hour}AM"
                }
            }
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
            lineWidth = 2.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
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
