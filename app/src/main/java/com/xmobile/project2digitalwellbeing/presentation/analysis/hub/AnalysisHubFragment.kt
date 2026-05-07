package com.xmobile.project2digitalwellbeing.presentation.analysis.hub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.xmobile.project2digitalwellbeing.databinding.FragmentAnalysisHubBinding
import com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory.AppCategoryActivity
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.analysis.latenight.LateNightAnalysisActivity
import com.xmobile.project2digitalwellbeing.presentation.analysis.transition.AppTransitionGraphActivity
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern.UsagePatternDetailActivity
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity

class AnalysisHubFragment : Fragment() {

    private var _binding: FragmentAnalysisHubBinding? = null
    private val binding get() = _binding!!
    private var isLaunching = false

    private val modulesAdapter by lazy {
        AnalysisModulesAdapter { module ->
            if (isLaunching) return@AnalysisModulesAdapter
            val destination = if (UsageAccessPermissionHelper.hasUsageAccessPermission(requireContext())) {
                module.destination
            } else {
                PermissionActivity::class.java
            }
            showLoading(true)
            view?.post {
                if (!isAdded) return@post
                startActivity(Intent(requireContext(), destination))
            }
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
        showLoading(false)
        binding.rvModules.apply {
            layoutManager = GridLayoutManager(requireContext(), 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (modulesAdapter.isSectionHeader(position)) 2 else 1
                    }
                }
            }
            adapter = modulesAdapter
        }
        modulesAdapter.submitList(buildModules())
    }

    override fun onResume() {
        super.onResume()
        showLoading(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isLaunching = false
        _binding = null
    }

    private fun showLoading(show: Boolean) {
        isLaunching = show
        _binding?.loadingOverlay?.visibility = if (show) View.VISIBLE else View.GONE
        _binding?.rvModules?.isEnabled = !show
    }

    private fun buildModules(): List<AnalysisHubItem> {
        return listOf(
            AnalysisHubItem.SectionHeader("App mapping"),
            AnalysisHubItem.Module(
                title = "App Categories",
                description = "Tell the app which apps help you focus and which tend to distract.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.layout_grid,
                destination = AppCategoryActivity::class.java
            ),
            AnalysisHubItem.Module(
                title = "Transitions",
                description = "Follow the app-to-app flow behind distracting loops.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.rotate_ccw,
                destination = AppTransitionGraphActivity::class.java
            ),
            AnalysisHubItem.SectionHeader("Explore insights"),
            AnalysisHubItem.Module(
                title = "Usage Patterns",
                description = "Review long sessions, quick checks, and repeated usage habits.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.chart_column,
                destination = UsagePatternDetailActivity::class.java
            ),
            AnalysisHubItem.Module(
                title = "Late Night",
                description = "See how much phone use happens near bedtime and after midnight.",
                iconResId = com.xmobile.project2digitalwellbeing.R.drawable.moon,
                destination = LateNightAnalysisActivity::class.java
            )
        )
    }
}
