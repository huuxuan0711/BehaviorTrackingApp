package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.xmobile.project2digitalwellbeing.R

internal class DashboardLineChartRenderer(
    chart: LineChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    context: Context
) : LineChartRenderer(chart, animator, viewPortHandler) {

    private val selectedOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white)
    }

    private val selectedInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.weekly_trend_icon_background)
    }

    override fun drawHighlighted(c: Canvas, indices: Array<out Highlight>) {
        super.drawHighlighted(c, indices)

        indices.forEach { highlight ->
            val dataSet = mChart.lineData.getDataSetByIndex(highlight.dataSetIndex) as? LineDataSet ?: return@forEach
            if (!dataSet.isHighlightEnabled) return@forEach

            val entry = dataSet.getEntryForXValue(highlight.x, highlight.y)
            val position = mChart.getTransformer(dataSet.axisDependency)
                .getPixelForValues(entry.x, entry.y * mAnimator.phaseY)
            val x = position.x.toFloat()
            val y = position.y.toFloat()

            if (!mViewPortHandler.isInBounds(x, y)) return@forEach

            c.drawCircle(x, y, 7f, selectedOuterPaint)
            c.drawCircle(x, y, 4.5f, selectedInnerPaint)
        }
    }
}
