package com.xmobile.project2digitalwellbeing.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.xmobile.project2digitalwellbeing.databinding.ActivityMainBinding
import com.xmobile.project2digitalwellbeing.domain.preferences.usecase.ObserveUsageAnalysisPreferencesUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var observeUsageAnalysisPreferencesUseCase: ObserveUsageAnalysisPreferencesUseCase

    private lateinit var binding: ActivityMainBinding
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager(savedInstanceState)
        setupBottomNavigation()
        maybeRequestNotificationPermission()
    }

    private fun setupViewPager(savedInstanceState: Bundle?) {
        binding.viewPagerMain.adapter = MainPagerAdapter(this)
        binding.viewPagerMain.isUserInputEnabled = false
        binding.viewPagerMain.offscreenPageLimit = 3

        binding.viewPagerMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateSelectedTab(position)
            }
        })

        if (savedInstanceState == null) {
            binding.viewPagerMain.setCurrentItem(HOME_PAGE, false)
            updateSelectedTab(HOME_PAGE)
        } else {
            updateSelectedTab(binding.viewPagerMain.currentItem)
        }
    }

    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener {
            showPage(HOME_PAGE)
        }

        binding.navAnalysis.setOnClickListener {
            showPage(ANALYSIS_PAGE)
        }

        binding.navSettings.setOnClickListener {
            showPage(SETTINGS_PAGE)
        }
    }

    private fun showPage(page: Int) {
        if (binding.viewPagerMain.currentItem != page) {
            binding.viewPagerMain.setCurrentItem(page, false)
        } else {
            updateSelectedTab(page)
        }
    }

    private fun updateSelectedTab(selectedPage: Int) {
        val isHomeSelected = selectedPage == HOME_PAGE
        val isAnalysisSelected = selectedPage == ANALYSIS_PAGE
        val isSettingsSelected = selectedPage == SETTINGS_PAGE

        binding.navHome.isSelected = isHomeSelected
        binding.iconHome.isSelected = isHomeSelected
        binding.textHome.isSelected = isHomeSelected

        binding.navAnalysis.isSelected = isAnalysisSelected
        binding.iconAnalysis.isSelected = isAnalysisSelected
        binding.textAnalysis.isSelected = isAnalysisSelected

        binding.navSettings.isSelected = isSettingsSelected
        binding.iconSettings.isSelected = isSettingsSelected
        binding.textSettings.isSelected = isSettingsSelected
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        lifecycleScope.launch {
            val preferences = observeUsageAnalysisPreferencesUseCase().first()
            val shouldRequestNotificationPermission =
                preferences.insightNotificationsEnabled || preferences.weeklyReportsEnabled
            if (shouldRequestNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        private const val HOME_PAGE = 0
        private const val ANALYSIS_PAGE = 1
        private const val SETTINGS_PAGE = 2
    }
}
