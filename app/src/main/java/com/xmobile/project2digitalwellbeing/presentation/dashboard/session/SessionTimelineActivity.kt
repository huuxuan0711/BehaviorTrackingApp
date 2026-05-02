package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

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
import com.xmobile.project2digitalwellbeing.databinding.ActivitySessionTimelineBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionTimelineBinding
    private val viewModel: SessionTimelineViewModel by viewModels()
    private val sessionAdapter = SessionTimelineAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySessionTimelineBinding.inflate(layoutInflater)
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
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SessionTimelineActivity)
            adapter = sessionAdapter
            setHasFixedSize(true)
        }
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

    private fun handleEffect(effect: SessionTimelineEffect) {
        when (effect) {
            SessionTimelineEffect.OpenPermission -> {
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            }
        }
    }

    private fun render(state: SessionTimelineUiState) {
        binding.txtDate.text = state.dateRangeLabel
        binding.tvInsight.text = state.errorMessage ?: state.insightText
        binding.btnNextWeek.isEnabled = state.canNavigateNext
        sessionAdapter.submitList(state.sessions)
    }
}
