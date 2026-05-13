package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ActivityUsageDetailAppBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsageDetailAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageDetailAppBinding
    private val viewModel: UsageDetailAppViewModel by viewModels()
    private val transitionAdapter = AppTransitionAdapter()
    private var lastShownErrorMessage: String? = null

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
                    binding.tvTrendText.text = getString(
                        R.string.auto_usage_detail_trend_vs_yesterday,
                        state.todayVsYesterdayPercent
                    )

                    if (state.appName.isNotBlank()) {
                        binding.tvAppTitle.text = state.appName
                    }

                    binding.tvMostActive.text = getString(
                        R.string.auto_most_active_format,
                        state.mostActivePeriod
                    )
                    binding.tvAvgSession.text = getString(
                        R.string.auto_avg_session_format,
                        state.avgSessionFormatted
                    )
                    binding.tvPeakUsage.text = state.peakUsageLabel

                    binding.tvTotalSessions.text = state.totalSessionsToday.toString()
                    binding.tvLongestSession.text = state.longestSessionFormatted
                    binding.tvShortestSession.text = state.shortestSessionFormatted

                    binding.tvTimeStart.text = state.timeStartLabel
                    binding.tvTimeMid.text = state.timeMidLabel
                    binding.tvTimeEnd.text = state.timeEndLabel

                    binding.txtInsight.text = state.insightSummary
                    binding.txtTip.text = state.tipSummary

                    val headerIcon = try {
                        packageManager.getApplicationIcon(state.packageName)
                    } catch (_: Throwable) {
                        null
                    }
                    if (headerIcon != null) {
                        binding.appIcon.setImageDrawable(headerIcon)
                    } else {
                        binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                    }

                    if (state.weekLineChartData.isNotEmpty()) {
                        UsageDetailChartConfigurator.renderLineChart(binding.lineChart, this@UsageDetailAppActivity, state.weekLineChartData)
                    } else {
                        binding.lineChart.clear()
                    }
                    if (state.todayHourlyBarChartData.isNotEmpty()) {
                        UsageDetailChartConfigurator.renderBarChart(binding.barChart, this@UsageDetailAppActivity, state.todayHourlyBarChartData)
                    } else {
                        binding.barChart.clear()
                    }

                    if (state.topTransitions.isEmpty()) {
                        binding.cvAppTransitions.visibility = View.GONE
                    } else {
                        binding.cvAppTransitions.visibility = View.VISIBLE
                        transitionAdapter.submitList(state.topTransitions)
                    }

                    binding.contentScroll.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    showErrorIfNeeded(state.errorMessage)
                }
            }
        }
    }

    private fun showErrorIfNeeded(errorMessage: String?) {
        if (errorMessage.isNullOrBlank() || errorMessage == lastShownErrorMessage) {
            return
        }
        lastShownErrorMessage = errorMessage
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
    }
}
