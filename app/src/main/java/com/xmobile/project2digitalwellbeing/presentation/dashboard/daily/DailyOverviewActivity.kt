package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.xmobile.project2digitalwellbeing.databinding.ActivityDailyOverviewBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.toTopAppUiModels
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DailyOverviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyOverviewBinding
    private val viewModel: DailyOverviewViewModel by viewModels()
    private val topAppsAdapter = DailyTopAppsAdapter()
    private val datePicker by lazy { DailyOverviewDatePicker(this) }
    private var lastShownErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDailyOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        if (UsageAccessPermissionHelper.hasUsageAccessPermission(this)) {
            viewModel.load(forceRefresh = false)
        } else {
            viewModel.onPermissionMissing()
        }
    }

    override fun onDestroy() {
        datePicker.dismiss()
        super.onDestroy()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.layoutDateSelector.setOnClickListener { viewModel.onDateSelectorClicked() }
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@DailyOverviewActivity)
            adapter = topAppsAdapter
            setHasFixedSize(true)
        }
        DailyOverviewChartConfigurator.configure(binding.barChart, this)
    }

    private fun observeUiState() {
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

    private fun handleEffect(effect: DailyOverviewEffect) {
        when (effect) {
            is DailyOverviewEffect.ShowDatePicker -> {
                datePicker.show(
                    anchor = binding.layoutDateSelector,
                    selectedDate = effect.selectedDate,
                    onDateSelected = viewModel::onDateSelected
                )
            }

            DailyOverviewEffect.OpenPermission -> {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            }
        }
    }

    private fun render(state: DailyOverviewUiState) {
        binding.txtDateSelector.text = state.dateLabel
        binding.txtTotalScreenTime.text = state.totalScreenTimeText
        binding.txtCompare.text = state.compareText
        binding.txtSessionCount.text = state.sessionCountText
        binding.txtValueLongestSession.text = state.longestSessionText
        binding.txtInsights.text = state.insightText

        val topApps = state.topApps.toTopAppUiModels(this)
        val hasTopApps = topApps.isNotEmpty()
        binding.rvApps.visibility = if (hasTopApps) View.VISIBLE else View.GONE
        binding.txtEmptyApps.visibility = if (hasTopApps) View.GONE else View.VISIBLE
        topAppsAdapter.submitList(topApps)

        val hasHourlyUsage = state.hourlyUsage.any { it.totalTimeMillis > 0L }
        binding.barChart.visibility = if (hasHourlyUsage) View.VISIBLE else View.GONE
        binding.txtEmptyChart.visibility = if (hasHourlyUsage) View.GONE else View.VISIBLE
        if (hasHourlyUsage) {
            DailyOverviewChartConfigurator.render(binding.barChart, this, state.hourlyUsage)
        } else {
            binding.barChart.clear()
        }

        showErrorIfNeeded(state.errorMessage)
    }

    private fun showErrorIfNeeded(errorMessage: String?) {
        if (errorMessage.isNullOrBlank() || errorMessage == lastShownErrorMessage) {
            return
        }
        lastShownErrorMessage = errorMessage
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
    }
}
