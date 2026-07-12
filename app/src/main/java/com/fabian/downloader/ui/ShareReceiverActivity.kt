package com.fabian.downloader.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.MainActivity
import com.fabian.downloader.ui.MainViewModel
import com.fabian.downloader.ui.SharePopupScreen
import com.fabian.downloader.ui.theme.MyApplicationTheme

class ShareReceiverActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
            android.widget.Toast.makeText(this, getString(com.fabian.downloader.R.string.downloads_toast_no_valid_link), android.widget.Toast.LENGTH_SHORT).show()
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
        
        checkAndRequestNotifications()
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                SharePopupScreen(
                    url = sharedText, 
                    viewModel = viewModel,
                    onClose = { finish() },
                    onNavigateToDownloads = {
                        val intent = Intent(this@ShareReceiverActivity, MainActivity::class.java).apply {
                            putExtra(com.fabian.downloader.utils.Config.EXTRA_NAVIGATE_TO_DOWNLOADS, true)
                            putExtra(com.fabian.downloader.utils.Config.EXTRA_INITIAL_PAGE, 1) // Go to "En progreso" tab
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    private fun checkAndRequestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
