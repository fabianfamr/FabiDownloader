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
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.ui.FabiDownloaderApp
import com.fabian.downloader.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    lateinit var database: AppDatabase
    private val startOnDownloadsState = mutableStateOf(false)
    private val initialPageState = mutableStateOf(0)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission result if needed
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("fabi_downloader_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "Sistema") ?: "Sistema"
        super.attachBaseContext(com.fabian.downloader.utils.LocaleHelper.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fabian.downloader.ui.AppSettings.init(this)
        database = AppDatabase.getInstance(this)
        
        startOnDownloadsState.value = intent.getBooleanExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, false)
        initialPageState.value = intent.getIntExtra(Config.EXTRA_INITIAL_PAGE, 0)
        
        checkAndRequestPermissions()
        com.fabian.downloader.utils.PathUtils.ensureFabiDirectories(this)
        enableEdgeToEdge()
        setContent {
            val themePreference by com.fabian.downloader.ui.AppSettings.themePreferenceState
            val dynamicColor by com.fabian.downloader.ui.AppSettings.dynamicColorState
            val accentColorName by com.fabian.downloader.ui.AppSettings.accentColorNameState
            val amoledMode by com.fabian.downloader.ui.AppSettings.amoledModeState
            val language by com.fabian.downloader.ui.AppSettings.languageState

            val currentContext = androidx.compose.ui.platform.LocalContext.current
            val localizedContext = androidx.compose.runtime.remember(language, currentContext) {
                com.fabian.downloader.utils.LocaleHelper.applyLocale(currentContext, language)
            }

            val activityRegistryOwner = (currentContext as? androidx.activity.result.ActivityResultRegistryOwner)
                ?: (this@MainActivity as? androidx.activity.result.ActivityResultRegistryOwner)

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration,
                *(if (activityRegistryOwner != null) arrayOf(
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides activityRegistryOwner
                ) else emptyArray())
            ) {
                MyApplicationTheme(
                    themePreference = themePreference,
                    dynamicColor = dynamicColor,
                    accentColorName = accentColorName,
                    amoledMode = amoledMode
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, false)) {
            startOnDownloadsState.value = true
            initialPageState.value = intent.getIntExtra(Config.EXTRA_INITIAL_PAGE, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        com.fabian.downloader.utils.PathUtils.ensureFabiDirectories(this)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No llamamos onAppClosed() aquí para permitir que DownloadForegroundService 
        // mantenga las descargas ejecutándose activamente en segundo plano.
    }
}
