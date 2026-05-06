package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.xmobile.project2digitalwellbeing.databinding.ActivityUsageDetailAppBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsageDetailAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageDetailAppBinding
    private val viewModel: UsageDetailAppViewModel by viewModels()
    private val transitionAdapter = AppTransitionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUsageDetailAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.rvAppTransitions.adapter = transitionAdapter

        UsageDetailChartConfigurator.configureLineChart(binding.lineChart, this)
        UsageDetailChartConfigurator.configureBarChart(binding.barChart, this)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.tvTodayTotal.text = state.todayTotalFormatted
                    binding.tvTrendText.text = if (state.todayVsYesterdayPercent >= 0) "+${state.todayVsYesterdayPercent}% vs yesterday" else "${state.todayVsYesterdayPercent}% vs yesterday"

                    if (state.appName.isNotBlank()) {
                        binding.tvAppTitle.text = state.appName
                    }

                    binding.tvMostActive.text = "Most active: ${state.mostActivePeriod}"
                    binding.tvAvgSession.text = "Avg session: ${state.avgSessionFormatted}"
                    binding.tvPeakUsage.text = state.peakUsageLabel

                    binding.tvTotalSessions.text = state.totalSessionsToday.toString()
                    binding.tvLongestSession.text = state.longestSessionFormatted
                    binding.tvShortestSession.text = state.shortestSessionFormatted

                    binding.tvTimeStart.text = state.timeStartLabel
                    binding.tvTimeMid.text = state.timeMidLabel
                    binding.tvTimeEnd.text = state.timeEndLabel

                    binding.txtInsight.text = state.insightSummary
                    binding.txtTip.text = state.tipSummary

                    if (state.appIcon != null) {
                        binding.appIcon.setImageDrawable(state.appIcon)
                    }

                    if (state.weekLineChartData.isNotEmpty()) {
                        UsageDetailChartConfigurator.renderLineChart(binding.lineChart, this@UsageDetailAppActivity, state.weekLineChartData)
                    }
                    if (state.todayHourlyBarChartData.isNotEmpty()) {
                        UsageDetailChartConfigurator.renderBarChart(binding.barChart, this@UsageDetailAppActivity, state.todayHourlyBarChartData)
                    }

                    if (state.topTransitions.isEmpty()) {
                        binding.cvAppTransitions.visibility = View.GONE
                    } else {
                        binding.cvAppTransitions.visibility = View.VISIBLE
                        transitionAdapter.submitList(state.topTransitions)
                    }
                }
            }
        }
    }
}