package com.fabian.downloader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.theme.FabiColorScheme
import com.fabian.downloader.utils.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainUrlInputSection(
    query: String,
    onQueryChange: (String) -> Unit,
    detectedPlatform: PlatformData?,
    searchBarVisible: Boolean,
    colors: FabiColorScheme,
    scope: CoroutineScope,
    onAnalyzeSuccess: (String) -> Unit
) {
    val ctx = LocalContext.current
    var analyzeState by remember { mutableStateOf(AnalyzeState.Idle) }
    val clipboardManager = remember {
        ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }

    AnimatedVisibility(
        visible = searchBarVisible,
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(300, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { 
                    onQueryChange(it)
                    analyzeState = AnalyzeState.Idle
                },
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardSecondary)
                    .border(
                        width = 1.5.dp,
                        color = if (detectedPlatform != null) detectedPlatform.color.copy(alpha = 0.4f) else colors.border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("submit_link_input"),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 40.dp, end = if (query.isNotEmpty()) 90.dp else 78.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.main_input_placeholder),
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
            ) {
                Icon(
                    imageVector = if (detectedPlatform != null) detectedPlatform.icon else AppIcons.Link,
                    contentDescription = null,
                    tint = if (detectedPlatform != null) detectedPlatform.color else colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("clear_link_button")
                    ) {
                        Icon(
                            imageVector = AppIcons.Clear,
                            contentDescription = stringResource(R.string.main_clear_button),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentDim)
                        .border(1.dp, colors.accentGlow, RoundedCornerShape(10.dp))
                        .clickable {
                            try {
                                if (clipboardManager.hasPrimaryClip()) {
                                    val clipData = clipboardManager.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                                        if (clipText.isNotEmpty()) {
                                            onQueryChange(clipText)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.ContentPaste,
                            contentDescription = stringResource(R.string.main_paste_button),
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.main_paste_button),
                            color = colors.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = searchBarVisible,
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(300, easing = FastOutSlowInEasing))
    ) {
        val isQueryValid = query.isNotEmpty() && (query.startsWith("http") || query.contains("."))
        Button(
            onClick = {
                if (isQueryValid && analyzeState == AnalyzeState.Idle) {
                    scope.launch {
                        analyzeState = AnalyzeState.Loading
                        delay(200)
                        analyzeState = AnalyzeState.Success
                        delay(150)
                        val targetQuery = query
                        onAnalyzeSuccess(targetQuery)
                        analyzeState = AnalyzeState.Idle
                    }
                } else if (query.isNotEmpty() && analyzeState == AnalyzeState.Idle) {
                    ToastUtils.showShort(ctx, R.string.main_invalid_link)
                }
            },
            enabled = query.isNotEmpty() && analyzeState != AnalyzeState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_link_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (analyzeState) {
                    AnalyzeState.Success -> colors.success
                    AnalyzeState.Loading -> colors.accent.copy(alpha = 0.5f)
                    else -> if (isQueryValid) colors.accent else colors.cardSecondary
                },
                contentColor = if (isQueryValid || analyzeState != AnalyzeState.Idle) Color(0xFF0A0A0C) else colors.textSecondary,
                disabledContainerColor = colors.cardSecondary,
                disabledContentColor = colors.textSecondary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (analyzeState == AnalyzeState.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF0A0A0C),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.main_analyzing_state),
                            color = Color(0xFF0A0A0C),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (analyzeState == AnalyzeState.Success) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0A0A0C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.main_link_detected_state),
                            color = Color(0xFF0A0A0C),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Download,
                            contentDescription = null,
                            tint = if (isQueryValid) Color(0xFF0A0A0C) else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.main_analyze_button),
                            color = if (isQueryValid) Color(0xFF0A0A0C) else colors.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
