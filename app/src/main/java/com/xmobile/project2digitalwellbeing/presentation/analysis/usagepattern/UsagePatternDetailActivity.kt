package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

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
import com.xmobile.project2digitalwellbeing.databinding.ActivityUsagePatternDetailBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsagePatternDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsagePatternDetailBinding
    private val viewModel: UsagePatternDetailViewModel by viewModels()
    private val topAppsAdapter = UsagePatternTopAppsAdapter()
    private var lastShownErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUsagePatternDetailBinding.inflate(layoutInflater)
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
        if (!UsageAccessPermissionHelper.hasUsageAccessPermission(this)) {
            startActivity(android.content.Intent(this, PermissionActivity::class.java))
            finish()
            return
        }
        viewModel.load()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.rvTopApps.apply {
            layoutManager = LinearLayoutManager(this@UsagePatternDetailActivity)
            adapter = topAppsAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: UsagePatternDetailUiState) {
        binding.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.tvAvgSession.text = state.averageSessionText
        binding.tvLongestSession.text = state.longestSessionText
        binding.tvTotalSessions.text = state.totalSessionText
        binding.tvSwitchCount.text = state.switchCountText
        binding.tvAvgSwitchTime.text = state.averageSwitchIntervalText
        binding.tvShortSessions.text = state.shortSessionText
        binding.tvMediumSessions.text = state.mediumSessionText
        binding.tvLongSessions.text = state.longSessionText
        binding.txtInsight.text = state.insightText

        binding.rvTopApps.visibility = if (state.topApps.isEmpty()) View.GONE else View.VISIBLE
        topAppsAdapter.submitList(state.topApps)

        binding.progressShort.post {
            setProgressWidth(binding.progressShort, state.shortSessionRatio)
            setProgressWidth(binding.progressMedium, state.mediumSessionRatio)
            setProgressWidth(binding.progressLong, state.longSessionRatio)
        }

        showErrorIfNeeded(state.errorMessage)
    }

    private fun setProgressWidth(view: View, ratio: Float) {
        val parentView = view.parent as? View ?: return
        val targetWidth = (parentView.width * ratio.coerceIn(0f, 1f)).toInt().coerceAtLeast(0)
        view.layoutParams = view.layoutParams.apply { width = targetWidth }
    }

    private fun showErrorIfNeeded(errorMessage: String?) {
        if (errorMessage.isNullOrBlank() || errorMessage == lastShownErrorMessage) {
            return
        }
        lastShownErrorMessage = errorMessage
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
    }
}
