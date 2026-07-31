package com.fabian.downloader.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.MainActivity
import com.fabian.downloader.ui.viewmodels.MainViewModel
import com.fabian.downloader.ui.screens.SharePopupScreen
import com.fabian.downloader.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue

class ShareReceiverActivity : ComponentActivity() {
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission result
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("fabi_downloader_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "Sistema") ?: "Sistema"
        if (!lang.contains("Sistema")) {
            val locale = if (lang.contains("English")) java.util.Locale("en") else java.util.Locale("es")
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fabian.downloader.ui.AppSettings.init(this)
        
        val action = intent.action
        val type = intent.type
        var sharedText = ""
        
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val fullText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                // Extraer solo la URL del texto compartido
                val urlRegex = Regex("""https?://[^\s]+""")
                val match = urlRegex.find(fullText)
                sharedText = match?.value ?: ""
                
                if (sharedText.isEmpty() && fullText.isNotEmpty()) {
                    // Si no hay URL pero hay texto, tal vez sea una URL mal formateada
                    sharedText = fullText.trim()
                }
            }
        }
        
        if (sharedText.isEmpty()) {
            com.fabian.downloader.utils.ToastUtils.showShort(this, com.fabian.downloader.R.string.downloads_toast_no_valid_link)
            finish()
            return
        }
        
        val database = AppDatabase.getInstance(this)
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(application, database) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        
        checkAndRequestPermissions()
        enableEdgeToEdge()
        
        setContent {
            val themePreference by com.fabian.downloader.ui.AppSettings.themePreferenceState
            val dynamicColor by com.fabian.downloader.ui.AppSettings.dynamicColorState
            val accentColorName by com.fabian.downloader.ui.AppSettings.accentColorNameState
            val amoledMode by com.fabian.downloader.ui.AppSettings.amoledModeState

            MyApplicationTheme(
                themePreference = themePreference,
                dynamicColor = dynamicColor,
                accentColorName = accentColorName,
                amoledMode = amoledMode
            ) {
                SharePopupScreen(
                    url = sharedText, 
                    viewModel = viewModel,
                    onClose = { finish() },
                    onNavigateToDownloads = {
                        val intent = Intent(this@ShareReceiverActivity, MainActivity::class.java).apply {
                            putExtra(Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
                            putExtra(Config.EXTRA_INITIAL_PAGE, 1) // Go to "En progreso" tab
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
