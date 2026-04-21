package com.xmobile.project2digitalwellbeing.presentation.onboarding.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.data.local.AppPreferencesDataStore
import com.xmobile.project2digitalwellbeing.databinding.ActivityIntroBinding
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import kotlinx.coroutines.launch

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore

    private val onboardingItems = listOf(
        OnboardingItem(
            iconResId = R.drawable.smartphone,
            iconTintRes = android.graphics.Color.parseColor("#5C6BC0"),
            iconBackgroundRes = android.graphics.Color.parseColor("#EAECF6"),
            title = "Understand Your\nDigital Habits",
            description = "See how you spend time on your phone and discover hidden usage patterns."
        ),
        OnboardingItem(
            iconResId = R.drawable.trending_up,
            iconTintRes = android.graphics.Color.parseColor("#7E57C2"),
            iconBackgroundRes = android.graphics.Color.parseColor("#ECEAF6"),
            title = "Beyond Screen Time",
            description = "Analyze usage sessions, app transitions, and behavioral patterns."
        ),
        OnboardingItem(
            iconResId = R.drawable.target,
            iconTintRes = android.graphics.Color.parseColor("#66BB6A"),
            iconBackgroundRes = android.graphics.Color.parseColor("#EAF2EF"),
            title = "Improve Your Focus",
            description = "Receive insights that help you build healthier digital habits."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appPreferencesDataStore = AppPreferencesDataStore(applicationContext)
        setupViewPager()
        setupActions()
        updateControls(0)
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(onboardingItems)
        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateControls(position)
            }
        })
    }

    private fun setupActions() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            val isLastItem = currentItem == onboardingItems.lastIndex

            if (isLastItem) {
                completeIntroAndNavigate()
            } else {
                binding.viewPager.currentItem = currentItem + 1
            }
        }

        binding.btnSkip.setOnClickListener {
            binding.viewPager.currentItem = onboardingItems.lastIndex
        }
    }

    private fun updateControls(position: Int) {
        val isLastItem = position == onboardingItems.lastIndex
        binding.btnNext.text = if (isLastItem) "Get Started" else "Next"
        binding.btnSkip.visibility = if (isLastItem) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun completeIntroAndNavigate() {
        lifecycleScope.launch {
            appPreferencesDataStore.setIntroCompleted(true)
            startActivity(Intent(this@IntroActivity, PermissionActivity::class.java))
            finish()
        }
    }
}
