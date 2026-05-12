package com.xmobile.project2digitalwellbeing.presentation.analysis.transition

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.core.graphics.drawable.toBitmap
import android.util.Base64
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
import com.xmobile.project2digitalwellbeing.databinding.ActivityAppTransitionGraphBinding
import com.xmobile.project2digitalwellbeing.domain.tracking.model.TransitionFilter
import com.xmobile.project2digitalwellbeing.domain.usage.usecase.AnalysisTimeRange
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class AppTransitionGraphActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppTransitionGraphBinding
    private val viewModel: AppTransitionGraphViewModel by viewModels()
    private var lastShownErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAppTransitionGraphBinding.inflate(layoutInflater)
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
            viewModel.load()
        } else {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnToday.setOnClickListener { viewModel.setTimeRange(AnalysisTimeRange.TODAY) }
        binding.btnWeek.setOnClickListener { viewModel.setTimeRange(AnalysisTimeRange.WEEK) }
        binding.btnAll.setOnClickListener { viewModel.setFilter(TransitionFilter.ALL) }
        binding.btnProductive.setOnClickListener { viewModel.setFilter(TransitionFilter.PRODUCTIVE_MIXED) }
        binding.btnDistracting.setOnClickListener { viewModel.setFilter(TransitionFilter.DISTRACTING_MIXED) }

        with(binding.webGraph.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webGraph.setBackgroundColor(0x00000000)
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: AppTransitionGraphUiState) {
        binding.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        setSelectedState(binding.btnToday, state.timeRange == AnalysisTimeRange.TODAY)
        setSelectedState(binding.btnWeek, state.timeRange == AnalysisTimeRange.WEEK)
        setSelectedState(binding.btnAll, state.filter == TransitionFilter.ALL)
        setSelectedState(binding.btnProductive, state.filter == TransitionFilter.PRODUCTIVE_MIXED)
        setSelectedState(binding.btnDistracting, state.filter == TransitionFilter.DISTRACTING_MIXED)
        binding.tvInsight.text = state.insightText

        val iconDataByNodeId = state.nodes.associate { node ->
            node.id to resolveIconDataUrl(node.id)
        }
        val html = AppTransitionGraphHtmlRenderer.render(this, state.nodes, state.edges, iconDataByNodeId)
        binding.webGraph.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        showErrorIfNeeded(state.errorMessage)
    }

    private fun setSelectedState(view: View, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_filter_selected else R.drawable.bg_filter_outline)
        if (view is androidx.appcompat.widget.AppCompatButton) {
            view.setTextColor(
                if (selected) getColor(android.R.color.white) else getColor(R.color.weekly_overview_text_secondary)
            )
        }
    }

    private fun showErrorIfNeeded(errorMessage: String?) {
        if (errorMessage.isNullOrBlank() || errorMessage == lastShownErrorMessage) {
            return
        }
        lastShownErrorMessage = errorMessage
        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
    }

    private fun resolveIconDataUrl(packageName: String): String {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            if (isSystemDefaultIcon(drawable)) {
                return ""
            }
            val bitmap = drawable.toBitmap(width = 48, height = 48)
            val stream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            "data:image/png;base64,$encoded"
        } catch (_: Throwable) {
            ""
        }
    }

    private fun isSystemDefaultIcon(drawable: android.graphics.drawable.Drawable): Boolean {
        val defaultState = packageManager.defaultActivityIcon.constantState
        val currentState = drawable.constantState
        return defaultState != null && currentState != null && defaultState == currentState
    }

    override fun onDestroy() {
        destroyGraphWebView()
        super.onDestroy()
    }

    private fun destroyGraphWebView() {
        val webView = binding.webGraph
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.removeAllViews()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
