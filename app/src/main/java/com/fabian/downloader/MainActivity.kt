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
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.ui.FabiDownloaderApp
import com.fabian.downloader.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    lateinit var database: AppDatabase
    private val startOnDownloadsState = mutableStateOf(false)
    private val initialPageState = mutableStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fabian.downloader.ui.AppSettings.init(this)
        database = AppDatabase.getInstance(this)
        
        startOnDownloadsState.value = intent.getBooleanExtra(com.fabian.downloader.utils.Config.EXTRA_NAVIGATE_TO_DOWNLOADS, false)
        initialPageState.value = intent.getIntExtra(com.fabian.downloader.utils.Config.EXTRA_INITIAL_PAGE, 0)
        
        checkAndRequestNotifications()
        enableEdgeToEdge()
        setContent {
            val themePreference by com.fabian.downloader.ui.AppSettings.themePreferenceState
            val dynamicColor by com.fabian.downloader.ui.AppSettings.dynamicColorState
            val accentColorName by com.fabian.downloader.ui.AppSettings.accentColorNameState
            
            MyApplicationTheme(
                themePreference = themePreference,
                dynamicColor = dynamicColor,
                accentColorName = accentColorName
            ) {
                FabiDownloaderApp(
                    database = database,
                    startOnDownloads = startOnDownloadsState.value,
                    initialPage = initialPageState.value,
                    onConsumedStartOnDownloads = {
                        startOnDownloadsState.value = false
                        initialPageState.value = 0
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(com.fabian.downloader.utils.Config.EXTRA_NAVIGATE_TO_DOWNLOADS, false)) {
            startOnDownloadsState.value = true
            initialPageState.value = intent.getIntExtra(com.fabian.downloader.utils.Config.EXTRA_INITIAL_PAGE, 0)
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
