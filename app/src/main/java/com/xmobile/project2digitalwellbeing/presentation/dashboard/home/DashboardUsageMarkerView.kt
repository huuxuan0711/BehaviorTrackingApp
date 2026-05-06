package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.content.Context
import android.graphics.RectF
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ViewDashboardUsageMarkerBinding

internal class DashboardUsageMarkerView(
    context: Context,
    private val rollingCurrentHour: Int? = null
) : MarkerView(
    context,
    R.layout.view_dashboard_usage_marker
) {

    private val binding: ViewDashboardUsageMarkerBinding by lazy {
        ViewDashboardUsageMarkerBinding.bind(findViewById(R.id.marker_root))
    }
    private val chartBounds = RectF()

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val x = e?.x?.toInt() ?: 0
        val hour = if (rollingCurrentHour != null) {
            (rollingCurrentHour - 23 + x + 24) % 24
        } else {
            x
        }
        val minutes = e?.y?.toInt() ?: 0
        binding.tvHour.text = hour.toMarkerHourLabel()
        binding.tvUsage.text = "Usage : ${minutes} min"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        val chartWidth = chartView?.width?.toFloat() ?: 0f
        val markerWidth = width.toFloat()
        val markerHeight = height.toFloat()
        val drawX = when {
            chartBounds.right >= chartWidth - 24f -> -markerWidth + 24f
            chartBounds.left <= 24f -> -24f
            else -> (-markerWidth / 2f) + 12f
        }
        return MPPointF(drawX, -markerHeight - 24f)
    }

    override fun draw(canvas: android.graphics.Canvas, posX: Float, posY: Float) {
        chartBounds.set(posX, posY, posX, posY)
        super.draw(canvas, posX, posY)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        chartBounds.set(posX, posY, posX, posY)
        return super.getOffsetForDrawingAtPoint(posX, posY)
    }

    private fun Int.toMarkerHourLabel(): String {
        val normalized = ((this % 24) + 24) % 24
        val period = if (normalized < 12) "AM" else "PM"
        val hour = normalized % 12
        val displayHour = if (hour == 0) 12 else hour
        return "$displayHour$period"
    }
}
