package com.xmobile.project2digitalwellbeing.presentation.main

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.xmobile.project2digitalwellbeing.presentation.analysis.hub.AnalysisHubFragment
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.DashboardFragment
import com.xmobile.project2digitalwellbeing.presentation.settings.hub.SettingHubFragment

class MainPagerAdapter(
    activity: AppCompatActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()
            1 -> AnalysisHubFragment()
            2 -> SettingHubFragment()
            else -> DashboardFragment()
        }
    }
}
