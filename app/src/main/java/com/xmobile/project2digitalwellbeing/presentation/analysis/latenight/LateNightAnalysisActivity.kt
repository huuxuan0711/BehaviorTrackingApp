package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

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
import com.xmobile.project2digitalwellbeing.databinding.ActivityLateNightAnalysisBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LateNightAnalysisActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLateNightAnalysisBinding
    private val viewModel: LateNightAnalysisViewModel by viewModels()
    private val appAdapter = LateNightAppAdapter()
    private var lastShownErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLateNightAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        LateNightChartConfigurator.configure(binding.barChart, this)
        observeUiState()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@LateNightAnalysisActivity)
            adapter = appAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.contentScroll.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    
                    binding.txtTotalScreenTime.text = state.totalScreenTime
                    binding.txtSessions.text = state.sessionCount
                    binding.txtAvgDuration.text = state.avgDuration
                    binding.txtPeakUsage.text = state.peakUsageLabel
                    binding.txtInsights.text = state.insightText
                    binding.txtRecommendation.text = state.recommendationText
                    
                    appAdapter.submitList(state.topApps)
                    
                    if (state.hourlyUsage.isNotEmpty()) {
                        LateNightChartConfigurator.render(binding.barChart, state.hourlyUsage)
                    }
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
