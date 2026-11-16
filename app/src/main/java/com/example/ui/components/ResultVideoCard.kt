package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadQuality
import com.example.data.model.VideoMetadata
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.util.FormatUtils

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
            // Creator & TikTok Header
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

                Box(
                    modifier = Modifier
                        .background(NeonMagenta.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonMagenta.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (metadata.isPhotoSlide) "📸 Photo Slides" else "🎵 TikTok HD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video Preview thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable {
                        val previewUrl = metadata.ultraHdrUrl.ifEmpty { metadata.standardHdUrl }
                        if (previewUrl.isNotBlank()) {
                            onPreviewClick(previewUrl, metadata.title)
                        }
                    }
                    .testTag("preview_thumbnail_box")
            ) {
                AsyncImage(
                    model = metadata.coverUrl.ifEmpty { metadata.images.firstOrNull() ?: "https://picsum.photos/seed/vid/600/400" },
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                if (!metadata.isPhotoSlide || metadata.standardHdUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.5.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play preview",
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (metadata.isPhotoSlide) "${metadata.images.size} Photos" else FormatUtils.formatDuration(metadata.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Video Title
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(Icons.Default.Visibility, FormatUtils.formatCount(metadata.playCount))
                MetricItem(Icons.Default.Favorite, FormatUtils.formatCount(metadata.likeCount), tint = NeonMagenta)
                MetricItem(Icons.Default.ChatBubbleOutline, FormatUtils.formatCount(metadata.commentCount))
                MetricItem(Icons.Default.Share, FormatUtils.formatCount(metadata.shareCount))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Carousel Slides Gallery (when TikTok photo post detected)
            if (metadata.isPhotoSlide) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, HdrGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                    color = ObsidianSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Collections, contentDescription = null, tint = HdrGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PHOTO CAROUSEL (${metadata.images.size} SLIDES)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = HdrGold
                                )
                            }
                            Button(
                                onClick = { onDownloadClick(metadata, DownloadQuality.PHOTO_SLIDE_ULTRA) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HdrGold, contentColor = Color.Black)
                            ) {
                                Text("Save All", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            metadata.images.forEachIndexed { idx, imgUrl ->
                                val slideNumber = idx + 1
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianSurface)
                                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                                        .clickable { onDownloadPhotoSlide(imgUrl, slideNumber) }
                                        .padding(6.dp)
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Photo Slide $slideNumber",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Save #$slideNumber",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NeonCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Dedicated Audio Extractor Section
            if (metadata.audioUrl.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    color = ObsidianSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AUDIO EXTRACTOR (320kbps MP3)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = NeonCyan,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Music/TikSave",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = metadata.musicTitle.ifEmpty { "Original Sound Soundtrack" },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onDownloadClick(metadata, DownloadQuality.AUDIO_ONLY) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, ObsidianCardBorder)
                            ) {
                                Text("🎵 Original", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onDownloadClick(metadata, DownloadQuality.AUDIO_HIGH_SOUND) },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                            ) {
                                Text("🔊 High Sound", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onDownloadClick(metadata, DownloadQuality.AUDIO_VFX_SOUND) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, ObsidianCardBorder)
                            ) {
                                Text("🎙️ VFX/Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Quality Selection Header
            Text(
                text = "SELECT DOWNLOAD RESOLUTION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            metadata.availableQualities.forEach { q ->
                val size = when (q) {
                    DownloadQuality.ULTRA_8K_120FPS -> metadata.ultra8kSize
                    DownloadQuality.HDR_ULTRA -> metadata.ultraHdrSize
                    DownloadQuality.HD_STANDARD -> metadata.standardHdSize
                    DownloadQuality.NORMAL_720P -> metadata.normal720pSize
                    DownloadQuality.LOW_480P -> metadata.low480pSize
                    DownloadQuality.AUDIO_ONLY, DownloadQuality.AUDIO_HIGH_SOUND, DownloadQuality.AUDIO_VFX_SOUND -> 4_200_000L
                    DownloadQuality.PHOTO_SLIDE_ULTRA, DownloadQuality.COVER_IMAGE -> 1_800_000L
                }
                QualityOptionRow(
                    quality = q,
                    isSelected = selectedQuality == q,
                    fileSize = size,
                    onSelect = { onQualitySelected(q) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Download Button
            Button(
                onClick = { onDownloadClick(metadata, selectedQuality) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_download_selected"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF041417)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DOWNLOAD ${selectedQuality.badge.uppercase()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDownloadClick(metadata, DownloadQuality.COVER_IMAGE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_download_cover"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HD Cover", fontSize = 12.sp)
                }

                Button(
                    onClick = { onGenerateAiClick(metadata) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("btn_ai_viral_pack"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Viral Hooks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    value: String,
    tint: Color = Color.White.copy(alpha = 0.7f)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun QualityOptionRow(
    quality: DownloadQuality,
    isSelected: Boolean,
    fileSize: Long,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else ObsidianCardBorder,
        animationSpec = tween(200),
        label = "quality_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.13f) else ObsidianSurfaceVariant,
        animationSpec = tween(200),
        label = "quality_bg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("quality_option_${quality.name}"),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quality.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) NeonCyan else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quality.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Text(
                    text = quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            if (fileSize > 0) {
                Text(
                    text = FormatUtils.formatBytes(fileSize),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
