package com.fabian.downloader.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fabian.downloader.configs.Config
import com.fabian.downloader.ui.theme.*
import com.fabian.downloader.services.ExtractionService.ExtractedPlaylist
import com.fabian.downloader.services.ExtractionService.PlaylistItem

@Composable
fun PlaylistBatchView(
    playlist: ExtractedPlaylist,
    onStartBatchDownload: (selectedItems: List<PlaylistItem>, quality: String, format: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(Config.FORMAT_MP4) }
    var selectedQuality by remember { mutableStateOf("720p") }
    
    val selectedItemIds = remember { mutableStateListOf<String>().apply { addAll(playlist.items.map { it.id }) } }
    val isAllSelected = selectedItemIds.size == playlist.items.size

    val qualityOptions = if (selectedFormat == Config.FORMAT_MP4) {
        listOf("1080p", "720p", "480p", "360p")
    } else {
        listOf("320k", "192k", "128k")
    }

    val fColors = MaterialTheme.fabiColors

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // PLAYLIST HEADER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = fColors.card),
            border = BorderStroke(1.dp, fColors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(playlist.brandColorHex)).copy(alpha = 0.2f)
                            } catch (e: Exception) {
                                Color(0xFF7B61FF).copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = try {
                            Color(android.graphics.Color.parseColor(playlist.brandColorHex))
                        } catch (e: Exception) {
                            Color(0xFF7B61FF)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF7B61FF).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "LISTA (${playlist.items.size} VÍDEOS)",
                                color = Color(0xFF9E8BFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.title,
                        color = fColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playlist.author,
                        color = fColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FORMAT & QUALITY SELECTORS
        Text(
            text = "OPCIONES DE DESCARGA PARA EL LOTE",
            color = fColors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(Config.FORMAT_MP4, "Vídeo (MP4)", Icons.Default.Videocam),
                Triple(Config.FORMAT_MP3, "Audio (MP3)", Icons.Default.Audiotrack),
                Triple(Config.FORMAT_M4A, "Audio (M4A)", Icons.Default.MusicNote)
            ).forEach { (fmt, label, icon) ->
                val isSelected = selectedFormat == fmt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) fColors.accent else fColors.card)
                        .border(
                            1.dp,
                            if (isSelected) fColors.accent else fColors.border,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedFormat = fmt
                            if (fmt != Config.FORMAT_MP4 && selectedQuality.contains("p")) {
                                selectedQuality = "320k"
                            } else if (fmt == Config.FORMAT_MP4 && selectedQuality.contains("k")) {
                                selectedQuality = "720p"
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) fColors.background else fColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            color = if (isSelected) fColors.background else fColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            qualityOptions.forEach { q ->
                val isSelected = selectedQuality == q
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedQuality = q },
                    label = { Text(q, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = fColors.accentDim,
                        selectedLabelColor = fColors.accent,
                        containerColor = fColors.card,
                        labelColor = fColors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = fColors.border,
                        selectedBorderColor = fColors.accent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BATCH SELECTION HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (isAllSelected) {
                        selectedItemIds.clear()
                    } else {
                        selectedItemIds.clear()
                        selectedItemIds.addAll(playlist.items.map { it.id })
                    }
                }
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            selectedItemIds.clear()
                            selectedItemIds.addAll(playlist.items.map { it.id })
                        } else {
                            selectedItemIds.clear()
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = fColors.accent,
                        uncheckedColor = fColors.textMuted
                    )
                )
                Text(
                    text = if (isAllSelected) "Desmarcar todos" else "Seleccionar todos",
                    color = fColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = fColors.card,
                border = BorderStroke(1.dp, fColors.border)
            ) {
                Text(
                    text = "${selectedItemIds.size} de ${playlist.items.size} seleccionados",
                    color = fColors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PLAYLIST ITEMS LIST
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            playlist.items.forEachIndexed { index, item ->
                val isChecked = selectedItemIds.contains(item.id)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isChecked) {
                                selectedItemIds.remove(item.id)
                            } else {
                                selectedItemIds.add(item.id)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) fColors.accentDim else fColors.cardSecondary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isChecked) fColors.accent.copy(alpha = 0.5f) else fColors.border
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedItemIds.add(item.id) else selectedItemIds.remove(item.id)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = fColors.accent,
                                uncheckedColor = fColors.textMuted
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(fColors.border)
                        ) {
                            if (item.thumbnailUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = fColors.textSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }

                            if (item.durationText.isNotEmpty()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = item.durationText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.title}",
                                color = fColors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.uploader.isNotEmpty()) {
                                Text(
                                    text = item.uploader,
                                    color = fColors.textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DOWNLOAD BATCH CTA BUTTON
        Button(
            onClick = {
                val selectedItems = playlist.items.filter { selectedItemIds.contains(it.id) }
                if (selectedItems.isNotEmpty()) {
                    onStartBatchDownload(selectedItems, selectedQuality, selectedFormat)
                }
            },
            enabled = selectedItemIds.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = fColors.accent,
                disabledContainerColor = fColors.border,
                contentColor = fColors.background,
                disabledContentColor = fColors.textDisabled
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "DESCARGAR LOTE (${selectedItemIds.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
