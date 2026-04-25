package com.xmobile.project2digitalwellbeing.presentation.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.xmobile.project2digitalwellbeing.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

    companion object {
        private const val HOME_PAGE = 0
        private const val ANALYSIS_PAGE = 1
        private const val SETTINGS_PAGE = 2
    }
}
