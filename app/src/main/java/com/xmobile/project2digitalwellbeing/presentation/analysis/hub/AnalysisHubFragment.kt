package com.xmobile.project2digitalwellbeing.presentation.analysis.hub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.xmobile.project2digitalwellbeing.databinding.FragmentAnalysisHubBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.analysis.behavior.BehaviorInsightDetailActivity
import com.xmobile.project2digitalwellbeing.presentation.analysis.latenight.LateNightAnalysisActivity
import com.xmobile.project2digitalwellbeing.presentation.analysis.transition.AppTransitionGraphActivity
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern.UsagePatternDetailActivity
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity

class AnalysisHubFragment : Fragment() {

    private var _binding: FragmentAnalysisHubBinding? = null
    private val binding get() = _binding!!

    private val modulesAdapter by lazy {
        AnalysisModulesAdapter { module ->
            val destination = if (UsageAccessPermissionHelper.hasUsageAccessPermission(requireContext())) {
                module.destination
            } else {
                PermissionActivity::class.java
            }
            startActivity(Intent(requireContext(), destination))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvModules.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = modulesAdapter
        }
        modulesAdapter.submitList(buildModules())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildModules(): List<AnalysisModuleItem> {
        return listOf(
            AnalysisModuleItem(
                title = "Usage Patterns",
                description = "Explore session behavior, switching, and long sessions.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.chart_column,
                destination = UsagePatternDetailActivity::class.java
            ),
            AnalysisModuleItem(
                title = "Late Night",
                description = "Review late-night usage intensity and after-hours peaks.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.moon,
                destination = LateNightAnalysisActivity::class.java
            ),
            AnalysisModuleItem(
                title = "Transitions",
                description = "Inspect how you move between apps across the day.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.rotate_ccw,
                destination = AppTransitionGraphActivity::class.java
            ),
            AnalysisModuleItem(
                title = "Behavior Insight",
                description = "Open the strongest synthesized behavior signal for today.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.lightbulb,
                destination = BehaviorInsightDetailActivity::class.java
            )
        )
    }
}
