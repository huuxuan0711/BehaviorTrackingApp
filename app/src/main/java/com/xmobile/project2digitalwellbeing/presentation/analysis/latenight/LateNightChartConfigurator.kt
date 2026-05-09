package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp.RoundedBarChartRenderer

internal object LateNightChartConfigurator {

    private val LABELS = listOf("10PM", "11PM", "12AM", "1AM", "2AM", "3AM", "4AM", "5AM")

    fun configure(chart: BarChart, context: Context) {
        val secondaryTextColor = ContextCompat.getColor(context, R.color.weekly_overview_text_secondary)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            axisRight.isEnabled = false
            setNoDataText(context.getString(R.string.auto_no_usage_data))
            extraBottomOffset = 12f // Thêm khoảng trống ở dưới để nhãn không bị cắt
            
            axisLeft.apply {
                isEnabled = true
                axisMinimum = 0f
                axisMaximum = 60f
                labelCount = 5
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = secondaryTextColor
                textSize = 12f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = 
                        context.getString(R.string.auto_unit_m_placeholder, value.toInt())
                }
            }
            
            val pxRadius = 12f * resources.displayMetrics.density
            renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, pxRadius)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textSize = 12f
                textColor = secondaryTextColor
                yOffset = 8f // Giảm từ 12f xuống 8f để dịch nhãn lên gần cột hơn

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val idx = value.toInt()
                        return if (idx in LABELS.indices) LABELS[idx] else ""
                    }
                }
                axisMinimum = -0.6f
                axisMaximum = 7.6f
                labelCount = LABELS.size
                granularity = 1f
                setAvoidFirstLastClipping(false)
            }
        }
    }

    fun render(chart: BarChart, context: Context, values: List<Float>) {
        val peakValue = values.maxOrNull() ?: 0f
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        
        val peakColor = ContextCompat.getColor(context, R.color.auto_color_fb8c00)
        val defaultColor = ContextCompat.getColor(context, R.color.primary)

        val colors = values.map { value ->
            if (value > 0f && value == peakValue) {
                peakColor
            } else {
                defaultColor
            }
        }

        val dataSet = BarDataSet(entries, "").apply {
            setColors(colors)
            setDrawValues(false)
        }
        chart.data = BarData(dataSet).apply {
            barWidth = 0.7f
        }
        chart.invalidate()
    }
}
