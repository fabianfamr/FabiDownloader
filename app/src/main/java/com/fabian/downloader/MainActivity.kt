package com.fabian.downloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.ui.FabiDownloaderApp
import com.fabian.downloader.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    lateinit var database: AppDatabase
    private val startOnDownloadsState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fabian.downloader.ui.AppSettings.init(this)
        database = AppDatabase.getInstance(this)
        
        startOnDownloadsState.value = intent.getBooleanExtra("navigate_to_downloads", false)
        
        checkAndRequestNotifications()
        enableEdgeToEdge()
        setContent {
            val themePreference = com.fabian.downloader.ui.AppSettings.themePreference
            MyApplicationTheme(themePreference = themePreference) {
                FabiDownloaderApp(database, startOnDownloadsState.value) {
                    startOnDownloadsState.value = false
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("navigate_to_downloads", false)) {
            startOnDownloadsState.value = true
        }
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
