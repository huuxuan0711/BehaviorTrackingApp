package com.xmobile.project2digitalwellbeing.presentation.settings.preferences

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ActivityPreferencesBinding
import com.xmobile.project2digitalwellbeing.databinding.LayoutPopupLanguageBinding
import com.xmobile.project2digitalwellbeing.domain.insights.model.InsightSensitivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreferencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferencesBinding
    private val viewModel: PreferencesViewModel by viewModels()
    private var suppressSwitchCallbacks = false
    private var pendingNotificationToggle: NotificationToggleTarget? = null
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val target = pendingNotificationToggle ?: return@registerForActivityResult
            pendingNotificationToggle = null
            if (isGranted) {
                when (target) {
                    NotificationToggleTarget.INSIGHT -> viewModel.onInsightNotificationsChanged(true)
                    NotificationToggleTarget.WEEKLY -> viewModel.onWeeklyReportsChanged(true)
                }
            } else {
                suppressSwitchCallbacks = true
                when (target) {
                    NotificationToggleTarget.INSIGHT -> {
                        binding.switchInsight.isChecked = false
                        viewModel.onInsightNotificationsChanged(false)
                    }
                    NotificationToggleTarget.WEEKLY -> {
                        binding.switchWeekly.isChecked = false
                        viewModel.onWeeklyReportsChanged(false)
                    }
                }
                suppressSwitchCallbacks = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.seekLateNight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.onLateNightThresholdChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.onSessionLengthThresholdChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) viewModel.onInsightSensitivityChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.switchCategory.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            viewModel.onTrackAllCategoriesChanged(isChecked)
        }

        binding.switchInsight.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            if (isChecked && !hasNotificationPermission()) {
                pendingNotificationToggle = NotificationToggleTarget.INSIGHT
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                suppressSwitchCallbacks = true
                binding.switchInsight.isChecked = false
                suppressSwitchCallbacks = false
                return@setOnCheckedChangeListener
            }
            viewModel.onInsightNotificationsChanged(isChecked)
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            viewModel.onDarkModeChanged(isChecked)
        }

        binding.switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            if (isChecked && !hasNotificationPermission()) {
                pendingNotificationToggle = NotificationToggleTarget.WEEKLY
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                suppressSwitchCallbacks = true
                binding.switchWeekly.isChecked = false
                suppressSwitchCallbacks = false
                return@setOnCheckedChangeListener
            }
            viewModel.onWeeklyReportsChanged(isChecked)
        }

        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: PreferencesUiState) {
        suppressSwitchCallbacks = true
        binding.seekLateNight.progress = state.lateNightProgress
        binding.tvLateNightValue.text = formatLateNightHour(state.lateNightHour)

        binding.seekSession.progress = state.sessionProgress
        binding.tvSessionValue.text = getString(R.string.auto_text_n_minutes_long, state.sessionMinutes)

        binding.switchCategory.isChecked = state.trackAllCategories
        binding.switchInsight.isChecked = state.insightNotificationsEnabled
        binding.switchDarkMode.isChecked = state.darkModeEnabled
        binding.switchWeekly.isChecked = state.weeklyReportsEnabled
        suppressSwitchCallbacks = false

        binding.seekSensitivity.progress = state.sensitivityProgress
        binding.tvSensitivityValue.text = when (state.sensitivity) {
            InsightSensitivity.LOW -> getString(R.string.auto_low)
            InsightSensitivity.MEDIUM -> getString(R.string.auto_medium)
            InsightSensitivity.HIGH -> getString(R.string.auto_high)
        }

        binding.tvCurrentLanguage.text = when (state.languageCode) {
            "vi" -> getString(R.string.auto_language_vietnamese)
            "fr" -> getString(R.string.auto_language_french)
            "de" -> getString(R.string.auto_language_german)
            else -> getString(R.string.auto_language_english)
        }
    }

    private fun showLanguageDialog() {
        val languages = listOf(
            LanguageUiModel("en", R.string.auto_language_english, R.drawable.england),
            LanguageUiModel("vi", R.string.auto_language_vietnamese, R.drawable.vietnam), // Should use actual flags if available
            LanguageUiModel("fr", R.string.auto_language_french, R.drawable.france),
            LanguageUiModel("de", R.string.auto_language_german, R.drawable.german)
        )

        val dialogBinding = LayoutPopupLanguageBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@PreferencesActivity)
            adapter = LanguageAdapter(languages) { language ->
                viewModel.onLanguageChanged(language.code)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun formatLateNightHour(hour: Int): String {
        return when {
            hour == 0 -> getString(R.string.auto_time_12am)
            hour == 12 -> getString(R.string.auto_time_12pm)
            hour > 12 -> getString(R.string.auto_time_pm_placeholder, hour - 12)
            else -> getString(R.string.auto_time_am_placeholder, hour)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private enum class NotificationToggleTarget {
        INSIGHT,
        WEEKLY
    }
}
