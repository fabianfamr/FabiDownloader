package com.fabian.downloader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.database.AppDatabase
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabiDownloaderApp(
    database: AppDatabase,
    startOnDownloads: Boolean = false,
    onConsumedStartOnDownloads: () -> Unit = {}
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Main, Screen.Downloads, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(startOnDownloads) {
        if (startOnDownloads) {
            navController.navigate(Screen.Downloads.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumedStartOnDownloads()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionContentColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            data.visuals.message, 
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } 
        },
        bottomBar = {
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface
            val outlineColor = MaterialTheme.colorScheme.outline

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surfaceColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    // Top divider line with subtle gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        outlineColor.copy(alpha = 0.3f),
                                        primaryColor.copy(alpha = 0.15f),
                                        outlineColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0)
                    ) {
                        screens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            
                            val animatedWeight by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                label = "navWeight"
                            )

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier
                                            .size(if (isSelected) 24.dp else 22.dp)
                                            .graphicsLayer {
                                                scaleX = 1f + (animatedWeight * 0.1f)
                                                scaleY = 1f + (animatedWeight * 0.1f)
                                            }
                                    )
                                },
                                label = { 
                                    Text(
                                        screen.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = primaryColor,
                                    selectedTextColor = primaryColor,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    indicatorColor = primaryColor.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Main.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                }
            ) {
                composable(Screen.Main.route) {
                    MainScreen(
                        database = database,
                        snackbarHostState = snackbarHostState,
                        onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) }
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadsScreen(database = database)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        database = database,
                        snackbarHostState = snackbarHostState,
                        onNavigateToDownloadSettings = { navController.navigate(Screen.DownloadSettings.route) },
                    )
                }
                composable(
                    Screen.DownloadSettings.route,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(250))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    }
                ) {
                    DownloadSettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
