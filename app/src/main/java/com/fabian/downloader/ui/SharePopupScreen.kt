package com.fabian.downloader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DownloadOption(
    val id: String,
    val title: String,
    val format: String,
    val quality: String,
    val category: String
)

fun extractUrl(text: String): String {
    val regex = Regex("""https?://[^\s]+""")
    val match = regex.find(text)
    return match?.value ?: text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePopupScreen(url: String, viewModel: MainViewModel, onClose: () -> Unit) {
    val cleanUrl = remember(url) { extractUrl(url) }
    
    val musicOptions = listOf(
        DownloadOption("music_fast", "Fast", "M4A", "128", "Music"),
        DownloadOption("music_classic", "Classic MP3 (320K)", "MP3", "320", "Music")
    )
    val videoOptions = listOf(
        DownloadOption("video_fast", "Fast (360p)", "MP4", "360p", "Video"),
        DownloadOption("video_hq", "High quality (720p)", "MP4", "720p", "Video")
    )
    
    // Default to Fast (360p) as shown in the Snaptube screenshot
    var selectedOptionId by remember { mutableStateOf("video_fast") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212), // Sleek pitch black/dark gray container
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Download video as",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // --- MUSIC SECTION ---
            Text(
                text = "Music",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            musicOptions.forEach { option ->
                FormatRow(
                    option = option,
                    icon = Icons.Default.MusicNote,
                    isSelected = selectedOptionId == option.id,
                    onSelect = { selectedOptionId = option.id }
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // --- VIDEO SECTION ---
            Text(
                text = "Video",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            videoOptions.forEach { option ->
                FormatRow(
                    option = option,
                    icon = Icons.Default.PlayCircleOutline,
                    isSelected = selectedOptionId == option.id,
                    onSelect = { selectedOptionId = option.id }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // --- HIGH FIDELITY DOWNLOAD BUTTON ---
            Button(
                onClick = {
                    if (cleanUrl.isNotEmpty()) {
                        val allOptions = musicOptions + videoOptions
                        val selected = allOptions.find { it.id == selectedOptionId }
                        if (selected != null) {
                            viewModel.downloadVideo(
                                url = cleanUrl,
                                quality = selected.quality,
                                format = selected.format,
                                title = "Procesando enlace...",
                                thumbnailUrl = null
                            )
                            onClose()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFCC00), // Vibrant golden yellow
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
            ) {
                Text(
                    text = "Descargar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun FormatRow(
    option: DownloadOption,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = option.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (isSelected) Color(0xFFFFCC00) else Color.Transparent,
                    shape = CircleShape
                )
                .then(
                    if (!isSelected) Modifier.border(1.5.dp, Color.Gray, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
