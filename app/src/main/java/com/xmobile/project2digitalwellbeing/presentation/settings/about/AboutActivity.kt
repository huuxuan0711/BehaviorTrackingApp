package com.xmobile.project2digitalwellbeing.presentation.settings.about

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xmobile.project2digitalwellbeing.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindAppInfo()
    }

    private fun bindAppInfo() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: "-"
        val appLocale = AppCompatDelegate.getApplicationLocales()[0]
            ?: ConfigurationCompat.getLocales(resources.configuration)[0]
            ?: Locale.getDefault()
        val lastUpdate = SimpleDateFormat("MMMM d, yyyy", appLocale)
            .format(Date(packageInfo.lastUpdateTime))

        findViewById<TextView>(R.id.txtVersion).text = versionName
        findViewById<TextView>(R.id.txtLastUpdate).text = lastUpdate
    }
}
