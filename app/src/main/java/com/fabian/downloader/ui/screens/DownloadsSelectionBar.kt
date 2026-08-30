package com.fabian.downloader.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabian.downloader.R
import com.fabian.downloader.ui.components.AppIcons

@Composable
fun DownloadsSelectionBar(
    selectedCount: Int,
    onCancelSelection: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    accentDimColor: Color,
    accentGlowColor: Color,
    accentColor: Color,
    textColor: Color,
    errorColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = accentDimColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accentGlowColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelSelection) {
                Icon(AppIcons.Close, stringResource(R.string.downloads_cancel_selection), tint = accentColor)
            }
            Text(
                text = stringResource(R.string.downloads_selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            IconButton(onClick = onShareSelected) {
                Icon(AppIcons.Share, stringResource(R.string.downloads_share_icon), tint = accentColor)
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(AppIcons.Delete, stringResource(R.string.downloads_delete_button), tint = errorColor)
            }
        }
    }
}
