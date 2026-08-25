package com.fabian.downloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.downloader.R
import com.fabian.downloader.managers.UpdateInfo
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.ui.components.AppIcons
import com.fabian.downloader.ui.screens.settings.*
import com.fabian.downloader.ui.theme.fabiColors

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ctx.contentResolver.takePersistableUriPermission(uri, takeFlags)
            AppSettings.downloadLocation = uri.toString()
        }
    }

    val fColors = MaterialTheme.fabiColors
    val C_bg = fColors.background
    val C_card = fColors.card
    val C_card2 = fColors.cardSecondary
    val C_border = fColors.border
    val C_accent = fColors.accent
    val C_white = fColors.textPrimary
    val C_gray1 = fColors.textSecondary

    var selectedCategory by remember { mutableStateOf(ctx.getString(R.string.settings_cat_downloads)) }
    var updateFound by remember { mutableStateOf<UpdateInfo?>(null) }

    if (updateFound != null) {
        AlertDialog(
            onDismissRequest = { updateFound = null },
            containerColor = C_card,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(AppIcons.NewReleases, contentDescription = null, tint = C_accent)
                    Text(stringResource(R.string.settings_update_available_title), color = C_white)
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_update_available_msg, updateFound!!.latestVersion),
                        color = C_white
                    )
                    if (updateFound!!.releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.settings_update_changes),
                            style = MaterialTheme.typography.labelMedium,
                            color = C_accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .background(C_card2, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = updateFound!!.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = C_gray1
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateFound!!.downloadUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                        updateFound = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C_accent, contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.settings_update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateFound = null }) {
                    Text(stringResource(R.string.settings_update_later), color = C_gray1)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(C_bg)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = C_white,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Categorías de Configuración
                val categories = listOf(
                    stringResource(R.string.settings_cat_downloads),
                    stringResource(R.string.settings_cat_library),
                    stringResource(R.string.settings_cat_appearance),
                    stringResource(R.string.settings_cat_advanced),
                    stringResource(R.string.settings_cat_system)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) C_accent.copy(alpha = 0.15f) else C_card2)
                                .border(1.5.dp, if (isSelected) C_accent else C_border, RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val icon = when(category) {
                                    stringResource(R.string.settings_cat_downloads) -> AppIcons.Download
                                    stringResource(R.string.settings_cat_library) -> AppIcons.Library
                                    stringResource(R.string.settings_cat_appearance) -> AppIcons.Palette
                                    stringResource(R.string.settings_cat_advanced) -> AppIcons.Build
                                    else -> AppIcons.Settings
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) C_accent else C_gray1,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = category,
                                    color = if (isSelected) C_accent else C_white,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                when (selectedCategory) {
                    stringResource(R.string.settings_cat_downloads) -> {
                        DownloadsSettingsSection(fColors = fColors, launcher = launcher)
                    }
                    stringResource(R.string.settings_cat_library) -> {
                        LibrarySettingsSection(fColors = fColors)
                    }
                    stringResource(R.string.settings_cat_appearance) -> {
                        AppearanceSettingsSection(fColors = fColors)
                    }
                    stringResource(R.string.settings_cat_advanced) -> {
                        AdvancedSettingsSection(fColors = fColors)
                    }
                    stringResource(R.string.settings_cat_system) -> {
                        SystemSettingsSection(
                            fColors = fColors,
                            ctx = ctx,
                            onUpdateFound = { updateFound = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    colorAccent: Color,
    textColor: Color,
    grayColor: Color,
    card2Color: Color,
    borderColor: Color,
    bgColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(32.dp).background(if (checked) colorAccent.copy(alpha = 0.15f) else card2Color, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (checked) colorAccent else grayColor, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = grayColor,
                        fontSize = 11.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = bgColor,
                checkedTrackColor = colorAccent,
                uncheckedThumbColor = grayColor,
                uncheckedTrackColor = card2Color,
                uncheckedBorderColor = borderColor
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: String,
    colorAccent: Color,
    textColor: Color,
    grayColor: Color,
    card2Color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(card2Color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colorAccent, modifier = Modifier.size(16.dp))
            }
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = trailing,
                color = grayColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Icon(AppIcons.ChevronRight, null, tint = grayColor, modifier = Modifier.size(16.dp))
        }
    }
}
