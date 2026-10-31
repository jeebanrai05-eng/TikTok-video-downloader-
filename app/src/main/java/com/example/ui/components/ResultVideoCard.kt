package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadQuality
import com.example.data.model.VideoMetadata
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.util.FormatUtils

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AutoAwesome

@Composable
fun ResultVideoCard(
    metadata: VideoMetadata,
    selectedQuality: DownloadQuality,
    onQualitySelected: (DownloadQuality) -> Unit,
    onDownloadClick: (VideoMetadata, DownloadQuality) -> Unit,
    onPreviewClick: (url: String, title: String) -> Unit,
    onGenerateAiClick: (VideoMetadata) -> Unit,
    onDownloadPhotoSlide: (url: String, index: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(22.dp))
            .testTag("result_video_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = metadata.authorAvatar.ifEmpty { "https://picsum.photos/seed/user/100/100" },
                    contentDescription = "Creator Avatar",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.authorNickname,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metadata.authorUsername,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // No watermark tag
                Box(
                    modifier = Modifier
                        .background(NeonMagenta.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonMagenta.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NO WATERMARK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeonMagenta,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video Preview thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable { onPreviewClick(metadata.ultraHdrUrl.ifEmpty { metadata.standardHdUrl }, metadata.title) }
                    .testTag("preview_thumbnail_box")
            ) {
                AsyncImage(
                    model = metadata.coverUrl.ifEmpty { "https://picsum.photos/seed/vid/600/400" },
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Play Button in center
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                        .background(NeonCyan.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video Preview",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(metadata.durationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // HDR badge on thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(HdrGold.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HDR+ ULTRA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Audio track info
            if (metadata.musicTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = NeonMagenta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = metadata.musicTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row (Views, Likes, Comments, Shares)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(icon = Icons.Default.Visibility, value = FormatUtils.formatCount(metadata.playCount), label = "Views")
                StatItem(icon = Icons.Default.Favorite, value = FormatUtils.formatCount(metadata.likeCount), label = "Likes", tint = NeonMagenta)
                StatItem(icon = Icons.Default.ChatBubbleOutline, value = FormatUtils.formatCount(metadata.commentCount), label = "Comments")
                StatItem(icon = Icons.Default.Share, value = FormatUtils.formatCount(metadata.shareCount), label = "Shares")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quality Selection Title
            Text(
                text = "SELECT DOWNLOAD STREAM",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quality Selection Options
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QualityOptionRow(
                    quality = DownloadQuality.ULTRA_8K_120FPS,
                    selected = selectedQuality == DownloadQuality.ULTRA_8K_120FPS,
                    fileSize = metadata.ultra8kSize,
                    badgeText = "8K 120FPS ULTRA",
                    badgeColor = Color(0xFF00E5FF),
                    onClick = { onQualitySelected(DownloadQuality.ULTRA_8K_120FPS) }
                )

                QualityOptionRow(
                    quality = DownloadQuality.HDR_ULTRA,
                    selected = selectedQuality == DownloadQuality.HDR_ULTRA,
                    fileSize = metadata.ultraHdrSize,
                    badgeText = "BEST QUALITY",
                    badgeColor = HdrGold,
                    onClick = { onQualitySelected(DownloadQuality.HDR_ULTRA) }
                )

                QualityOptionRow(
                    quality = DownloadQuality.HD_STANDARD,
                    selected = selectedQuality == DownloadQuality.HD_STANDARD,
                    fileSize = metadata.standardHdSize,
                    badgeText = "FAST SPEED",
                    badgeColor = NeonCyan,
                    onClick = { onQualitySelected(DownloadQuality.HD_STANDARD) }
                )

                QualityOptionRow(
                    quality = DownloadQuality.AUDIO_ONLY,
                    selected = selectedQuality == DownloadQuality.AUDIO_ONLY,
                    fileSize = 3_200_000L,
                    badgeText = "MP3 ONLY",
                    badgeColor = NeonMagenta,
                    onClick = { onQualitySelected(DownloadQuality.AUDIO_ONLY) }
                )
            }

            if (metadata.isPhotoSlide) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📸 PHOTO SLIDES (${metadata.images.size} Photos)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF818CF8),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(HdrGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "8K HDR+ QUALITY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = HdrGold,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            metadata.images.forEachIndexed { index, imgUrl ->
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianSurfaceVariant)
                                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .clickable { onPreviewClick(imgUrl, "Slide #${index + 1}") }
                                    ) {
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = "Slide ${index + 1}",
                                            modifier = Modifier.matchParentSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = { onDownloadPhotoSlide(imgUrl, index + 1) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Save 8K", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Button
            Button(
                onClick = { onDownloadClick(metadata, selectedQuality) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_download_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedQuality) {
                        DownloadQuality.ULTRA_8K_120FPS -> Color(0xFF00E5FF)
                        DownloadQuality.HDR_ULTRA -> HdrGold
                        DownloadQuality.AUDIO_ONLY -> NeonMagenta
                        DownloadQuality.PHOTO_SLIDE_ULTRA -> Color(0xFF818CF8)
                        else -> NeonCyan
                    },
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download ${selectedQuality.badge} (No Watermark)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preview Action
            OutlinedButton(
                onClick = { onPreviewClick(metadata.ultraHdrUrl.ifEmpty { metadata.standardHdUrl }, metadata.title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("preview_video_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Watch In-App Preview")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Viral Generator Button
            Button(
                onClick = { onGenerateAiClick(metadata) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("ai_generate_from_card_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✨ AI Viral Generator (Hooks & Facebook Post)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color = Color.White.copy(alpha = 0.7f)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun QualityOptionRow(
    quality: DownloadQuality,
    selected: Boolean,
    fileSize: Long,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                if (selected) badgeColor else ObsidianCardBorder,
                RoundedCornerShape(12.dp)
            )
            .background(if (selected) badgeColor.copy(alpha = 0.12f) else ObsidianSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("quality_option_${quality.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Radio indicator
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(2.dp, if (selected) badgeColor else Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(badgeColor, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = quality.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            if (fileSize > 0) {
                Text(
                    text = FormatUtils.formatBytes(fileSize),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
