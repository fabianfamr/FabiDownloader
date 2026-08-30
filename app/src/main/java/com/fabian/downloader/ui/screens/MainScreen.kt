package com.fabian.downloader.ui.screens

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config
import com.fabian.downloader.database.AppDatabase
import com.fabian.downloader.database.DownloadRecord
import com.fabian.downloader.services.StorageService
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.PlatformIcons
import com.fabian.downloader.ui.theme.fabiColors
import com.fabian.downloader.ui.viewmodels.MainViewModel
import com.fabian.downloader.utils.PathUtils
import kotlinx.coroutines.launch

enum class AnalyzeState { Idle, Loading, Success }

data class PlatformData(
    val id: String,
    val label: String,
    val color: Color,
    val domain: String,
    val icon: ImageVector
)

@Composable
fun MainScreen(
    database: AppDatabase, 
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val application = ctx.applicationContext as Application

    val viewModel: MainViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application, database) as T
            }
        }
    )

    val storageService = remember { StorageService.getInstance(ctx) }
    val downloadsList by storageService.getAllDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    val recentDownloads by remember(downloadsList) {
        derivedStateOf { 
            downloadsList
                .filter { it.isCompleted }
                .sortedByDescending { it.timestamp }
                .take(3) 
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var urlToDownloadInDialog by remember { mutableStateOf<String?>(null) }
    var lastProcessedClipboardUrl by rememberSaveable { mutableStateOf("") }
    val clipboardManager = remember {
        ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val action = AppSettings.clipboardAction
                    if (action != "disabled" && clipboardManager.hasPrimaryClip()) {
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                            if ((clipText.startsWith("http://") || clipText.startsWith("https://")) && clipText != lastProcessedClipboardUrl) {
                                lastProcessedClipboardUrl = clipText
                                if (action == "auto") {
                                    urlToDownloadInDialog = clipText
                                } else {
                                    clipboardUrl = clipText
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openFile: (DownloadRecord) -> Unit = { record ->
        try {
            val file = PathUtils.getDownloadFile(ctx, record.title, record.id, record.format)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(ctx, "com.fabian.downloader.fileprovider", file)
                val mimeType = when (record.format.uppercase()) {
                    Config.FORMAT_MP4, Config.FORMAT_WEBM -> Config.MIME_VIDEO
                    Config.FORMAT_JPG, Config.FORMAT_PNG, Config.FORMAT_WEBP, "JPEG" -> Config.MIME_IMAGE
                    else -> Config.MIME_AUDIO
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } else {
                scope.launch { snackbarHostState?.showSnackbar(ctx.getString(R.string.main_error_file_not_found)) }
            }
        } catch (e: Exception) {
            scope.launch { snackbarHostState?.showSnackbar(ctx.getString(R.string.main_error_opening_file, e.localizedMessage ?: "")) }
        }
    }

    var searchBarVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        searchBarVisible = true
        contentVisible = true
    }

    val colors = MaterialTheme.fabiColors

    val platforms = remember {
        listOf(
            PlatformData("youtube", "YouTube", Color(0xFFFF0000), "youtube.com", PlatformIcons.YouTube),
            PlatformData("tiktok", "TikTok", Color(0xFF69C9D0), "tiktok.com", PlatformIcons.TikTok),
            PlatformData("instagram", "Instagram", Color(0xFFE1306C), "instagram.com", PlatformIcons.Instagram),
            PlatformData("twitter", "X", Color(0xFFFFFFFF), "x.com", PlatformIcons.X),
            PlatformData("facebook", "Facebook", Color(0xFF1877F2), "facebook.com", PlatformIcons.Facebook),
            PlatformData("reddit", "Reddit", Color(0xFFFF4500), "reddit.com", PlatformIcons.Reddit),
            PlatformData("pinterest", "Pinterest", Color(0xFFE60023), "pinterest.com", PlatformIcons.Pinterest),
            PlatformData("vimeo", "Vimeo", Color(0xFF1AB7EA), "vimeo.com", PlatformIcons.Vimeo),
            PlatformData("soundcloud", "SoundCloud", Color(0xFFFF5500), "soundcloud.com", PlatformIcons.SoundCloud),
            PlatformData("twitch", "Twitch", Color(0xFF9146FF), "twitch.tv", PlatformIcons.Twitch),
            PlatformData("kick", "Kick", Color(0xFF53FC18), "kick.com", PlatformIcons.Kick),
            PlatformData("dailymotion", "Dailymotion", Color(0xFF0066DC), "dailymotion.com", PlatformIcons.Dailymotion),
            PlatformData("bilibili", "Bilibili", Color(0xFF00A1D6), "bilibili.com", PlatformIcons.Bilibili),
            PlatformData("tumblr", "Tumblr", Color(0xFF36465D), "tumblr.com", PlatformIcons.Tumblr),
            PlatformData("vk", "VK", Color(0xFF0077FF), "vk.com", PlatformIcons.VK),
            PlatformData("rumble", "Rumble", Color(0xFF85C742), "rumble.com", PlatformIcons.Rumble),
            PlatformData("threads", "Threads", Color(0xFF000000), "threads.net", PlatformIcons.Threads),
            PlatformData("patreon", "Patreon", Color(0xFFFF424D), "patreon.com", PlatformIcons.Patreon),
            PlatformData("bandcamp", "Bandcamp", Color(0xFF629AA9), "bandcamp.com", PlatformIcons.Bandcamp),
            PlatformData("mixcloud", "Mixcloud", Color(0xFF5000FF), "mixcloud.com", PlatformIcons.Mixcloud),
            PlatformData("discord", "Discord", Color(0xFF5865F2), "discord.com", PlatformIcons.Discord),
            PlatformData("telegram", "Telegram", Color(0xFF26A5E4), "t.me", PlatformIcons.Telegram),
            PlatformData("spotify", "Spotify", Color(0xFF1DB954), "spotify.com", PlatformIcons.Spotify)
        )
    }

    val detectedPlatform = remember(query) {
        val q = query.lowercase()
        platforms.find { p -> 
            q.contains(p.domain) || 
            (p.id == "youtube" && q.contains("youtu.be")) || 
            (p.id == "twitter" && q.contains("twitter.com")) 
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(300, easing = FastOutSlowInEasing))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .graphicsLayer { translationY = floatY },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = stringResource(R.string.main_app_title),
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.main_paste_link_title),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(listOf(colors.accent, Color(0xFF7B61FF)))
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 1.3.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            MainUrlInputSection(
                query = query,
                onQueryChange = { query = it },
                detectedPlatform = detectedPlatform,
                searchBarVisible = searchBarVisible,
                colors = colors,
                scope = scope,
                onAnalyzeSuccess = { targetQuery ->
                    viewModel.saveSearch(targetQuery)
                    urlToDownloadInDialog = targetQuery
                    query = ""
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            MainRecentDownloadsSection(
                recentDownloads = recentDownloads,
                contentVisible = contentVisible,
                colors = colors,
                onNavigateToDownloads = onNavigateToDownloads,
                onOpenFile = openFile
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (urlToDownloadInDialog != null) {
            SharePopupScreen(
                url = urlToDownloadInDialog!!,
                viewModel = viewModel,
                onClose = { urlToDownloadInDialog = null },
                onNavigateToDownloads = {
                    urlToDownloadInDialog = null
                    onNavigateToDownloads()
                }
            )
        }
    }
}
