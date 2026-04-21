package com.xmobile.project2digitalwellbeing.presentation.onboarding.permission

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xmobile.project2digitalwellbeing.databinding.ActivityPermissionBinding
import com.xmobile.project2digitalwellbeing.helper.UsageAccessPermissionHelper
import com.xmobile.project2digitalwellbeing.presentation.main.MainActivity

class PermissionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupActions()
    }

    override fun onResume() {
        super.onResume()
        if (UsageAccessPermissionHelper.hasUsageAccessPermission(this)) {
            navigateToMain()
        }
    }

    private fun setupActions() {
        binding.btnOpenSettings.setOnClickListener {
            startActivity(UsageAccessPermissionHelper.createUsageAccessSettingsIntent())
        }

        binding.btnMaybeLater.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
