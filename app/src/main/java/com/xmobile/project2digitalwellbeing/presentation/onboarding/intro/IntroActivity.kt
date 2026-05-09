package com.xmobile.project2digitalwellbeing.presentation.onboarding.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.data.preferences.local.AppPreferencesDataStore
import com.xmobile.project2digitalwellbeing.databinding.ActivityIntroBinding
import com.xmobile.project2digitalwellbeing.presentation.onboarding.permission.PermissionActivity
import kotlinx.coroutines.launch

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore

    private lateinit var onboardingItems: List<OnboardingItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appPreferencesDataStore = AppPreferencesDataStore(applicationContext)
        onboardingItems = buildOnboardingItems()
        setupViewPager()
        setupActions()
        updateControls(0)
    }

    private fun buildOnboardingItems(): List<OnboardingItem> {
        return listOf(
            OnboardingItem(
                iconResId = R.drawable.smartphone,
                iconTintRes = ContextCompat.getColor(this, R.color.primary),
                iconBackgroundRes = ContextCompat.getColor(this, R.color.auto_color_eaecf6),
                title = getString(R.string.onboarding_title_habits),
                description = getString(R.string.onboarding_desc_habits)
            ),
            OnboardingItem(
                iconResId = R.drawable.trending_up,
                iconTintRes = ContextCompat.getColor(this, R.color.auto_color_7e57c2),
                iconBackgroundRes = ContextCompat.getColor(this, R.color.auto_color_eceaf6),
                title = getString(R.string.onboarding_title_beyond_time),
                description = getString(R.string.onboarding_desc_beyond_time)
            ),
            OnboardingItem(
                iconResId = R.drawable.target,
                iconTintRes = ContextCompat.getColor(this, R.color.auto_color_66bb6a),
                iconBackgroundRes = ContextCompat.getColor(this, R.color.auto_color_eaf2ef),
                title = getString(R.string.onboarding_title_focus),
                description = getString(R.string.onboarding_desc_focus)
            )
        )
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
        binding.btnNext.text = if (isLastItem) {
            getString(R.string.onboarding_get_started)
        } else {
            getString(R.string.onboarding_next)
        }
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
