package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveDownload
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.FormatUtils

@Composable
fun ActiveDownloadCard(
    download: ActiveDownload,
    onCancel: () -> Unit,
    onPlay: (filePath: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedProgress by animateFloatAsState(
        targetValue = download.progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "DownloadProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                if (download.isCompleted) SuccessGreen.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .testTag("active_download_card"),
        color = ObsidianSurface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (download.isCompleted) SuccessGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (download.isCompleted) Icons.Default.Check else Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (download.isCompleted) SuccessGreen else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (download.isCompleted) "Download Completed" else "Downloading Without Watermark...",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${download.quality.badge} • ${download.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!download.isCompleted) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("cancel_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel Download",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (download.isCompleted) SuccessGreen else NeonCyan,
                trackColor = Color(0xFF242738),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Percentage
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (download.isCompleted) SuccessGreen else NeonCyan
                )

                // Speed indicator
                if (!download.isCompleted && download.speedBytesPerSec > 0) {
                    Text(
                        text = "⚡ ${FormatUtils.formatSpeed(download.speedBytesPerSec)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = HdrGold
                    )
                }

                // Data transferred
                val transferredText = if (download.totalBytes > 0) {
                    "${FormatUtils.formatBytes(download.bytesDownloaded)} / ${FormatUtils.formatBytes(download.totalBytes)}"
                } else {
                    FormatUtils.formatBytes(download.bytesDownloaded)
                }
                Text(
                    text = transferredText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Completed Actions
            if (download.isCompleted && download.localFilePath != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlay(download.localFilePath) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_downloaded_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Video", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            FormatUtils.shareFile(
                                context = context,
                                filePath = download.localFilePath,
                                mimeType = if (download.quality.name == "AUDIO_ONLY") "audio/mp3" else "video/mp4"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_downloaded_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}
