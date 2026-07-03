package com.fabian.downloader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            AppSettings.downloadLocation = uri.toString()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ajustes de Descarga",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp) // Centrado y con ancho máximo agradable en pantallas grandes
            ) {
                DownloadSettingsContent(
                    qualityOptions = AppSettings.qualityOptions,
                    selectedQuality = AppSettings.selectedQuality,
                    onQualityChange = { AppSettings.selectedQuality = it },
                    videoFormats = AppSettings.videoFormats,
                    selectedVideoFormat = AppSettings.selectedVideoFormat,
                    onVideoFormatChange = { AppSettings.selectedVideoFormat = it },
                    audioFormats = AppSettings.audioFormats,
                    selectedAudioFormat = AppSettings.selectedAudioFormat,
                    onAudioFormatChange = { AppSettings.selectedAudioFormat = it },
                    downloadLocation = AppSettings.downloadLocation,
                    onPickLocation = { launcher.launch(null) },
                    onLocationChange = { AppSettings.downloadLocation = it },
                    maxSpeed = AppSettings.maxSpeed,
                    onSpeedChange = { AppSettings.maxSpeed = it },
                    speedOptions = AppSettings.speedOptions
                )
            }
        }
    }
}
