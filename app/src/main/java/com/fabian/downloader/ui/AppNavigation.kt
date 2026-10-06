package com.fabian.downloader.ui

import com.fabian.downloader.ui.screens.*
import com.fabian.downloader.ui.viewmodels.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.database.AppDatabase
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.fabian.downloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabiDownloaderApp(
    database: AppDatabase,
    startOnDownloads: Boolean = false,
    initialPage: Int = 0,
    onConsumedStartOnDownloads: () -> Unit = {}
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Main, Screen.Downloads, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(startOnDownloads) {
        if (startOnDownloads) {
            navController.navigate(Screen.Downloads.route + "?initialPage=$initialPage") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumedStartOnDownloads()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowWidthClass = rememberWindowWidthClass(maxWidth)
        val isWide = windowWidthClass.isWide

        CompositionLocalProvider(LocalWindowWidthClass provides windowWidthClass) {
            val fColors = MaterialTheme.fabiColors
            val C_sheet = fColors.sheet
            val C_border = fColors.border
            val C_accent = fColors.accent
            val C_gray1 = fColors.textSecondary

            Row(modifier = Modifier.fillMaxSize()) {
                // NavigationRail adaptativo para tablets y pantallas anchas (>= 600dp)
                if (isWide) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp)
                            .border(width = 1.dp, color = C_border),
                        containerColor = C_sheet,
                        contentColor = fColors.textPrimary,
                        header = {
                            Column(
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 24.dp)
                                    .statusBarsPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(C_accent.copy(alpha = 0.14f))
                                        .border(1.dp, C_accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                        contentDescription = stringResource(R.string.app_name),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        screens.forEach { screen ->
                            val isSelected = currentRoute?.startsWith(screen.route) == true
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.08f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "railItemScale"
                            )

                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route && currentRoute?.startsWith(screen.route) != true) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = stringResource(screen.titleRes),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(screen.titleRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = C_accent,
                                    selectedTextColor = C_accent,
                                    indicatorColor = C_accent.copy(alpha = 0.16f),
                                    unselectedIconColor = C_gray1,
                                    unselectedTextColor = C_gray1
                                ),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .heightIn(min = 56.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1.5f))
                    }
                }

                // Área principal de contenido y navegación
                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
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
                        // Solo mostramos BottomBar en teléfonos / pantallas compactas (< 600dp)
                        if (!isWide) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                color = C_sheet,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column {
                                    // Línea divisoria superior sutil
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(C_border))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        screens.forEach { screen ->
                                            val isSelected = currentRoute?.startsWith(screen.route) == true
                                            val animatedScale by animateFloatAsState(
                                                targetValue = if (isSelected) 1.05f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "bottomBarScale"
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clickable {
                                                        if (currentRoute != screen.route && currentRoute?.startsWith(screen.route) != true) {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Indicador píldora expresivo de Material 3
                                                Box(
                                                    modifier = Modifier
                                                        .graphicsLayer {
                                                            scaleX = animatedScale
                                                            scaleY = animatedScale
                                                        }
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(if (isSelected) C_accent.copy(alpha = 0.16f) else Color.Transparent)
                                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = screen.icon,
                                                        contentDescription = stringResource(screen.titleRes),
                                                        modifier = Modifier.size(24.dp),
                                                        tint = if (isSelected) C_accent else C_gray1
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = stringResource(screen.titleRes),
                                                    color = if (isSelected) C_accent else C_gray1,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
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
                                val initialIndex = getRouteIndex(initialState.destination.route)
                                val targetIndex = getRouteIndex(targetState.destination.route)
                                val direction = if (targetIndex >= initialIndex) 1 else -1
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f * direction).toInt() },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            },
                            exitTransition = {
                                val initialIndex = getRouteIndex(initialState.destination.route)
                                val targetIndex = getRouteIndex(targetState.destination.route)
                                val direction = if (targetIndex >= initialIndex) 1 else -1
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (-fullWidth * 0.25f * direction).toInt() },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(200))
                            },
                            popEnterTransition = {
                                val initialIndex = getRouteIndex(initialState.destination.route)
                                val targetIndex = getRouteIndex(targetState.destination.route)
                                val direction = if (targetIndex >= initialIndex) 1 else -1
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f * direction).toInt() },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            },
                            popExitTransition = {
                                val initialIndex = getRouteIndex(initialState.destination.route)
                                val targetIndex = getRouteIndex(targetState.destination.route)
                                val direction = if (targetIndex >= initialIndex) 1 else -1
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.25f * direction).toInt() },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(200))
                            }
                        ) {
                            composable(Screen.Main.route) {
                                MainScreen(
                                    database = database,
                                    snackbarHostState = snackbarHostState,
                                    onNavigateToDownloads = {
                                        navController.navigate(Screen.Downloads.route + "?initialPage=1") {
                                            popUpTo(Screen.Main.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(Screen.Main.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            composable(
                                route = Screen.Downloads.route + "?initialPage={initialPage}",
                                arguments = listOf(
                                    navArgument("initialPage") {
                                        type = NavType.IntType
                                        defaultValue = 0
                                    }
                                )
                            ) { backStackEntry ->
                                val page = backStackEntry.arguments?.getInt("initialPage") ?: 0
                                DownloadsScreen(
                                    database = database,
                                    initialPage = page,
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.route)
                                    }
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen()
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
        }
    }
}

private fun getRouteIndex(route: String?): Int {
    if (route == null) return 0
    if (route.startsWith(Screen.Main.route)) return 0
    if (route.startsWith(Screen.Downloads.route)) return 1
    if (route.startsWith(Screen.Settings.route)) return 2
    if (route.startsWith(Screen.DownloadSettings.route)) return 3
    return 0
}
