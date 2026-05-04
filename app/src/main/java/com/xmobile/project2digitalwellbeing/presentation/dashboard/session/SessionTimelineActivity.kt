package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
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
    private var lastShownErrorMessage: String? = null

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
        binding.btnBackWeek.setOnClickListener { viewModel.showPreviousDay() }
        binding.btnNextWeek.setOnClickListener { viewModel.showNextDay() }
        binding.recyclerView.apply {
            layoutManager = object : LinearLayoutManager(this@SessionTimelineActivity) {
                override fun canScrollVertically(): Boolean = false
            }
            adapter = sessionAdapter
            isNestedScrollingEnabled = false
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setHasFixedSize(false)
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
        binding.tvInsight.text = state.insightText.ifBlank {
            "No session insight yet. Meaningful patterns will appear after more usage is recorded."
        }
        binding.btnNextWeek.isEnabled = state.canNavigateNext
        binding.btnNextWeek.alpha = if (state.canNavigateNext) 1f else 0.35f

        val hasSessions = state.sessions.isNotEmpty()
        binding.recyclerView.visibility = if (hasSessions) View.VISIBLE else View.GONE
        binding.txtEmptySessions.visibility = if (hasSessions) View.GONE else View.VISIBLE
        sessionAdapter.submitList(state.sessions)
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
