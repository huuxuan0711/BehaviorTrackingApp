package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.content.Context
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.DashboardUsageMarkerView
import kotlin.math.ceil

internal object UsageDetailChartConfigurator {

    fun configureLineChart(chart: LineChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            extraLeftOffset = 0f
            extraTopOffset = 0f
            extraRightOffset = 0f
            extraBottomOffset = 0f
            xAxis.isEnabled = false
            setNoDataText("")
        }
    }

    fun configureBarChart(chart: BarChart, context: Context) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            axisRight.isEnabled = false
            axisLeft.isEnabled = false
            setNoDataText("No hourly data")

            // Use custom renderer for rounded top corners
            val pxRadius = 12f * resources.displayMetrics.density
            renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, pxRadius)

            // Custom marker to show detailed time on top of bar
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            marker = DashboardUsageMarkerView(context, currentHour)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
                axisMinimum = -0.5f
                axisMaximum = 23.5f
            }
        }
    }

    fun renderLineChart(chart: LineChart, context: Context, values: List<Float>) {
        val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "").apply {
            color = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
            lineWidth = 2.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(context, R.drawable.bg_chart_usage_fill)
        }
        chart.data = LineData(dataSet)
        
        // Fixed scale for percentages (0-100%) to make charts comparable between apps
        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 100f
        }

        chart.invalidate()
    }

    fun renderBarChart(chart: BarChart, context: Context, values: List<Float>) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "").apply {
            color = android.graphics.Color.parseColor("#E0E1F6")
            highLightColor = android.graphics.Color.parseColor("#6366F1")
            highLightAlpha = 255
            setDrawValues(false)
        }
        chart.data = BarData(dataSet).apply {
            barWidth = 0.8f
        }
        
        // Adjust y-axis to accommodate data
        val maxVal = values.maxOrNull() ?: 1f
        chart.axisLeft.axisMaximum = if (maxVal == 0f) 1f else maxVal * 1.1f
        chart.axisLeft.axisMinimum = 0f
        
        chart.invalidate()
    }
}
