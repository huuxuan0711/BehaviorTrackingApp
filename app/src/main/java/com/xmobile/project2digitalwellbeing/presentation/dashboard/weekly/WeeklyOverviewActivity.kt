package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project2digitalwellbeing.databinding.ActivityWeeklyOverviewBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.toTopAppUiModels
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WeeklyOverviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeeklyOverviewBinding
    private val viewModel: WeeklyOverviewViewModel by viewModels()
    private val topAppsAdapter = WeeklyTopAppsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWeeklyOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        observeUi()
    }

    override fun onResume() {
        super.onResume()
        if (UsageAccessPermissionHelper.hasUsageAccessPermission(this)) {
            viewModel.load(forceRefresh = false)
        } else {
            viewModel.onPermissionMissing()
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBackWeek.setOnClickListener { viewModel.showPreviousWeek() }
        binding.btnNextWeek.setOnClickListener { viewModel.showNextWeek() }
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@WeeklyOverviewActivity)
            adapter = topAppsAdapter
            setHasFixedSize(true)
        }
        WeeklyOverviewChartConfigurator.configure(binding.barChart, this)
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.effects.collect(::handleEffect)
                }
            }
        }
    }

    private fun handleEffect(effect: WeeklyOverviewEffect) {
        when (effect) {
            WeeklyOverviewEffect.OpenPermission -> {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            }
        }
    }

    private fun render(state: WeeklyOverviewUiState) {
        binding.txtDate.text = state.dateRangeLabel
        binding.tvAvg.text = state.averageDailyScreenTimeText
        binding.tvMax.text = state.mostUsedDayText
        binding.tvTotal.text = state.totalScreenTimeText
        binding.tvTrend.text = state.errorMessage ?: state.trendText
        binding.btnNextWeek.isEnabled = state.canNavigateNext
        topAppsAdapter.submitList(state.topApps.toTopAppUiModels(this))
        WeeklyOverviewChartConfigurator.render(binding.barChart, this, state.chartBars)
    }
}
