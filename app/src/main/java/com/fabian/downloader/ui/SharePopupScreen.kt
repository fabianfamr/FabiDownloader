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
    
    var selectedOptionId by remember { mutableStateOf("video_fast") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF191919),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Download video as",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Music",
                color = Color.Gray,
                fontSize = 14.sp,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Video",
                color = Color.Gray,
                fontSize = 14.sp,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "More formats",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "All >",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                                title = null,
                                thumbnailUrl = null
                            )
                            onClose()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFCC00),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Download",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = option.title,
            color = Color.White,
            fontSize = 16.sp,
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
                    if (!isSelected) Modifier.border(2.dp, Color.Gray, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
