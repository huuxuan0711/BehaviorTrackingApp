package com.xmobile.project2digitalwellbeing.presentation.settings.hub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.FragmentSettingHubBinding
import com.xmobile.project2digitalwellbeing.presentation.settings.about.AboutActivity
import com.xmobile.project2digitalwellbeing.presentation.settings.preferences.PreferencesActivity
import com.xmobile.project2digitalwellbeing.presentation.settings.privacy.PrivacyAndDataActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingHubFragment : Fragment() {

    private var _binding: FragmentSettingHubBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingHubViewModel by viewModels()

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
        observeViewModel()
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.preferences.collectLatest { prefs ->
                prefs?.let {
                    if (it.isCloudBackupEnabled) {
                        binding.txtStatusCloud.setText(R.string.auto_active)
                        binding.viewStatusCloud.backgroundTintList =
                            ContextCompat.getColorStateList(requireContext(), R.color.theme_success_accent)
                    } else {
                        binding.txtStatusCloud.setText(R.string.auto_not_configured)
                        binding.viewStatusCloud.backgroundTintList =
                            ContextCompat.getColorStateList(requireContext(), R.color.weekly_overview_text_secondary)
                    }
                }
            }
        }
    }
}
