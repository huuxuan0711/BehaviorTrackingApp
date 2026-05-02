package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project2digitalwellbeing.databinding.FragmentDashboardBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.analysis.behavior.BehaviorInsightDetailActivity
import com.xmobile.project2digitalwellbeing.presentation.dashboard.daily.DailyOverviewActivity
import com.xmobile.project2digitalwellbeing.presentation.dashboard.session.SessionTimelineActivity
import com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly.WeeklyOverviewActivity
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private val topAppsAdapter = DashboardTopAppsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActions()
        observeUi()
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded) return
        if (UsageAccessPermissionHelper.hasUsageAccessPermission(requireContext())) {
            viewModel.load(forceRefresh = false)
        } else {
            viewModel.onPermissionMissing()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTopApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topAppsAdapter
            setHasFixedSize(true)
        }
        DashboardChartConfigurator.configure(binding.chart, requireContext())
    }

    private fun setupActions() {
        binding.cardDailyOverview.setOnClickListener {
            viewModel.onAction(DashboardAction.OpenDailyOverview, hasUsagePermission())
        }
        binding.cardWeeklyOverview.setOnClickListener {
            viewModel.onAction(DashboardAction.OpenWeeklyOverview, hasUsagePermission())
        }
        binding.cardSessionTimeline.setOnClickListener {
            viewModel.onAction(DashboardAction.OpenSessionTimeline, hasUsagePermission())
        }
        binding.cardKeyInsight.setOnClickListener {
            viewModel.onAction(DashboardAction.OpenBehaviorInsight, hasUsagePermission())
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.effects.collect(::handleEffect)
                }
            }
        }
    }

    private fun handleEffect(effect: DashboardEffect) {
        when (effect) {
            DashboardEffect.OpenDailyOverview ->
                startActivity(Intent(requireContext(), DailyOverviewActivity::class.java))

            DashboardEffect.OpenWeeklyOverview ->
                startActivity(Intent(requireContext(), WeeklyOverviewActivity::class.java))

            DashboardEffect.OpenSessionTimeline ->
                startActivity(Intent(requireContext(), SessionTimelineActivity::class.java))

            DashboardEffect.OpenBehaviorInsight ->
                startActivity(Intent(requireContext(), BehaviorInsightDetailActivity::class.java))

            DashboardEffect.RequestPermission ->
                startActivity(Intent(requireContext(), PermissionActivity::class.java))
        }
    }

    private fun hasUsagePermission(): Boolean {
        return UsageAccessPermissionHelper.hasUsageAccessPermission(requireContext())
    }

    private fun render(state: DashboardUiState) {
        binding.txtTime.text = state.currentDateLabel.ifBlank { "Today" }
        binding.txtKeyInsight.text = state.toInsightSummaryText()

        binding.valueScreenTime.text = (state.dailyUsage?.totalScreenTimeMillis ?: 0L).toDashboardDurationText()
        binding.valueLongestSession.text = state.dailyUsage
            ?.sessions
            ?.maxOfOrNull { it.durationMillis }
            ?.toDashboardDurationText()
            ?: "0m"
        binding.valueLateNight.text = state.hourlyUsage.toLateNightRatioText()

        topAppsAdapter.submitList(state.topApps.toTopAppUiModels(requireContext()))
        DashboardChartConfigurator.render(
            chart = binding.chart,
            context = requireContext(),
            hourlyUsage = state.hourlyUsage
        )
    }
}
