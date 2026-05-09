package com.xmobile.project2digitalwellbeing.presentation.settings.privacy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.xmobile.project2digitalwellbeing.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PrivacyAndDataActivity : AppCompatActivity() {

    private val viewModel: PrivacyAndDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_and_data)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        findViewById<SwitchCompat>(R.id.switchCloud).setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCloudBackup(isChecked)
        }

        findViewById<View>(R.id.btnExportData).setOnClickListener {
            viewModel.exportData { jsonData ->
                shareExportedData(jsonData)
            }
        }

        findViewById<View>(R.id.btnDeleteData).setOnClickListener {
            showDeleteConfirmationDialog()
        }

        findViewById<View>(R.id.btnResetAnalysis).setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.auto_delete_all_stored_data)
            .setMessage(R.string.delete_data_confirmation_message)
            .setPositiveButton(R.string.delete_data_positive_button) { _, _ ->
                viewModel.deleteAllData()
                Toast.makeText(this, "All data deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.auto_reset_behavior_analysis)
            .setMessage(R.string.reset_analysis_confirmation_message)
            .setPositiveButton(R.string.reset_analysis_positive_button) { _, _ ->
                viewModel.resetAnalysis()
                Toast.makeText(this, "Analysis reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    private fun shareExportedData(json: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "Digital Wellbeing Data Export")
        }
        startActivity(Intent.createChooser(intent, "Share Usage Data"))
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.preferences.collectLatest { prefs ->
                prefs?.let {
                    findViewById<SwitchCompat>(R.id.switchCloud).isChecked = it.isCloudBackupEnabled
                }
            }
        }
    }
}
