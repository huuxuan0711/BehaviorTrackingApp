package com.xmobile.project2digitalwellbeing.presentation.settings.hub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xmobile.project2digitalwellbeing.databinding.FragmentSettingHubBinding
import com.xmobile.project2digitalwellbeing.presentation.settings.about.AboutActivity
import com.xmobile.project2digitalwellbeing.presentation.settings.preferences.PreferencesActivity
import com.xmobile.project2digitalwellbeing.presentation.settings.privacy.PrivacyAndDataActivity

class SettingHubFragment : Fragment() {

    private var _binding: FragmentSettingHubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.layoutPrivacy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyAndDataActivity::class.java))
        }

        binding.layoutPreferences.setOnClickListener {
            startActivity(Intent(requireContext(), PreferencesActivity::class.java))
        }

        binding.layoutAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }
}
