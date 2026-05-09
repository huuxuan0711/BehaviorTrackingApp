package com.xmobile.project2digitalwellbeing.data.backup

import android.app.backup.BackupAgent
import android.app.backup.FullBackupDataOutput
import android.os.Build
import com.xmobile.project2digitalwellbeing.data.preferences.local.AppPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        val appPreferencesDataStore = AppPreferencesDataStore(applicationContext)
        
        val prefs = runBlocking {
            appPreferencesDataStore.usageAnalysisPreferences.first()
        }

        val isCloudBackupEnabled = prefs.isCloudBackupEnabled
        
        // On API 28+, we can distinguish between cloud backup and device-to-device transfer.
        val isDeviceTransfer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (data.transportFlags and FLAG_DEVICE_TO_DEVICE_TRANSFER) != 0
        } else {
            // On older versions, we can't easily distinguish, so we'll respect the cloud setting for all.
            false
        }

        if (isCloudBackupEnabled || isDeviceTransfer) {
            // Proceed with backup using the rules defined in XML
            super.onFullBackup(data)
        } else {
            // Skip backup if disabled and not a device transfer
        }
    }

    override fun onBackup(
        oldState: android.os.ParcelFileDescriptor?,
        data: android.app.backup.BackupDataOutput?,
        newState: android.os.ParcelFileDescriptor?
    ) {
        // This is for Key/Value backup, which we are not using.
    }

    override fun onRestore(
        data: android.app.backup.BackupDataInput?,
        appVersionCode: Int,
        newState: android.os.ParcelFileDescriptor?
    ) {
        // This is for Key/Value backup, which we are not using.
    }
}
