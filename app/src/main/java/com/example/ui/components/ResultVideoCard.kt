package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ClipPreset
import com.example.data.model.DownloadQuality
import com.example.data.model.MediaPlatform
import com.example.data.model.VideoMetadata
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val maxDuration = metadata.durationSeconds.toFloat().coerceAtLeast(15f)
    var trimStartSec by remember(metadata.id) { mutableStateOf(0f) }
    var trimEndSec by remember(metadata.id) {
        val initialEnd = when (metadata.clipPreset) {
            ClipPreset.SEC_15 -> minOf(15f, maxDuration)
            ClipPreset.SEC_30 -> minOf(30f, maxDuration)
            ClipPreset.SEC_60 -> minOf(60f, maxDuration)
            ClipPreset.SEC_90 -> minOf(90f, maxDuration)
            else -> minOf(45f, maxDuration)
        }
        mutableStateOf(initialEnd)
    }
    var selectedClip by remember(metadata.id) { mutableStateOf(metadata.clipPreset) }

    fun formatDurationTime(sec: Float): String {
        val totalSec = sec.toInt().coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    val currentTrimmedMetadata = metadata.copy(
        clipPreset = selectedClip,
        trimStartSeconds = trimStartSec.toInt(),
        trimEndSeconds = trimEndSec.toInt(),
        durationSeconds = (trimEndSec - trimStartSec).toInt().coerceAtLeast(1)
    )

    fun copyToClipboard(text: String, label: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareDirectly(caption: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

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
            // Platform & Author Header
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

                // Platform Badge
                val badgeColor = when (metadata.platform) {
                    MediaPlatform.YOUTUBE_TO_REELS -> Color(0xFFFF0000)
                    MediaPlatform.FACEBOOK_TO_SHORTS -> Color(0xFF1877F2)
                    MediaPlatform.TIKTOK -> NeonMagenta
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = metadata.platform.badge,
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
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Center Play Button
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

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(metadata.durationSeconds),
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

            // ==========================================
            // INTERACTIVE VIDEO TRIMMER & CLIP CUTTER
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color(0xFF1877F2).copy(alpha = 0.6f)
                        else if (metadata.platform == MediaPlatform.FACEBOOK_TO_SHORTS) NeonMagenta.copy(alpha = 0.6f)
                        else NeonCyan.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    ),
                color = ObsidianSurfaceVariant
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color(0xFF1877F2) else NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "INTERACTIVE VIDEO TRIMMER & CLIPPER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color(0xFF1877F2) else NeonCyan,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "9:16 Vertical Cut",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "भिडियोको मनपर्ने ठाउँ कट गर्नुहोस् (Start & End Time Slider) - Facebook Reels, TikTok र Shorts को लागि तयार क्लिप!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start Time, Duration & End Time Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Time Block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(ObsidianSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("START", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(formatDurationTime(trimStartSec), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "-1s",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        trimStartSec = (trimStartSec - 1f).coerceAtLeast(0f)
                                        selectedClip = ClipPreset.CUSTOM
                                    }.padding(2.dp)
                                )
                                Text(
                                    "+1s",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (trimStartSec + 1f < trimEndSec - 2f) {
                                            trimStartSec += 1f
                                            selectedClip = ClipPreset.CUSTOM
                                        }
                                    }.padding(2.dp)
                                )
                            }
                        }

                        // Center Duration Badge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = HdrGold.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HdrGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "✂️ ${formatDurationTime(trimEndSec - trimStartSec)} Clip",
                                    color = HdrGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Max: ${formatDurationTime(maxDuration)}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }

                        // End Time Block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(ObsidianSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("END", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                            Text(formatDurationTime(trimEndSec), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "-1s",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (trimEndSec - 1f > trimStartSec + 2f) {
                                            trimEndSec -= 1f
                                            selectedClip = ClipPreset.CUSTOM
                                        }
                                    }.padding(2.dp)
                                )
                                Text(
                                    "+1s",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        trimEndSec = (trimEndSec + 1f).coerceAtMost(maxDuration)
                                        selectedClip = ClipPreset.CUSTOM
                                    }.padding(2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // RangeSlider for interactive trimming
                    RangeSlider(
                        value = trimStartSec..trimEndSec,
                        onValueChange = { range ->
                            if (range.endInclusive - range.start >= 3f) {
                                trimStartSec = range.start
                                trimEndSec = range.endInclusive
                                selectedClip = ClipPreset.CUSTOM
                            }
                        },
                        valueRange = 0f..maxDuration,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ObsidianCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("video_trimmer_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ClipPreset.SEC_15 to "15s Viral Reel",
                            ClipPreset.SEC_30 to "30s Short Clip",
                            ClipPreset.SEC_60 to "60s Full Reel",
                            ClipPreset.FULL to "Full Video"
                        ).forEach { (preset, label) ->
                            val isSelected = selectedClip == preset
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSelected) NeonCyan else ObsidianCardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedClip = preset
                                        when (preset) {
                                            ClipPreset.SEC_15 -> {
                                                trimStartSec = 0f
                                                trimEndSec = minOf(15f, maxDuration)
                                            }
                                            ClipPreset.SEC_30 -> {
                                                trimStartSec = 0f
                                                trimEndSec = minOf(30f, maxDuration)
                                            }
                                            ClipPreset.SEC_60 -> {
                                                trimStartSec = 0f
                                                trimEndSec = minOf(60f, maxDuration)
                                            }
                                            ClipPreset.FULL -> {
                                                trimStartSec = 0f
                                                trimEndSec = maxDuration
                                            }
                                            else -> {}
                                        }
                                    },
                                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else ObsidianSurface
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Cut & Download Button
                    Button(
                        onClick = { onDownloadClick(currentTrimmedMetadata, selectedQuality) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_cut_and_download"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color(0xFF1877F2) else NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✂️ Cut & Download (${formatDurationTime(trimStartSec)} - ${formatDurationTime(trimEndSec)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS) Color.White else Color.Black
                        )
                    }

                    // Platform Specific Sharing Buttons
                    if (metadata.platform == MediaPlatform.YOUTUBE_TO_REELS || metadata.facebookReelsReadyCaption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { shareDirectly(metadata.facebookReelsReadyCaption, "Post to Facebook Reels") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post to FB Reels", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { copyToClipboard(metadata.facebookReelsReadyCaption, "Facebook Reels Caption") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Caption", fontSize = 11.sp)
                            }
                        }
                    }

                    if (metadata.platform == MediaPlatform.FACEBOOK_TO_SHORTS || metadata.tiktokShortsReadyCaption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { shareDirectly(metadata.tiktokShortsReadyCaption, "Share Clip to TikTok") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("🎵 Post to TikTok", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { shareDirectly(metadata.tiktokShortsReadyCaption, "Share Clip to YouTube Shorts") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("▶️ Post to Shorts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // DEDICATED AUDIO EXTRACTOR SECTION
            // ==========================================
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
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
                text = "SELECT VIDEO RESOLUTION & BITRATE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quality Options List (All resolutions requested by user: 8K, 4K HDR, 1080p, 720p, 480p)
            QualityOptionRow(
                quality = DownloadQuality.ULTRA_8K_120FPS,
                isSelected = selectedQuality == DownloadQuality.ULTRA_8K_120FPS,
                fileSize = metadata.ultra8kSize,
                onSelect = { onQualitySelected(DownloadQuality.ULTRA_8K_120FPS) }
            )

            QualityOptionRow(
                quality = DownloadQuality.HDR_ULTRA,
                isSelected = selectedQuality == DownloadQuality.HDR_ULTRA,
                fileSize = metadata.ultraHdrSize,
                onSelect = { onQualitySelected(DownloadQuality.HDR_ULTRA) }
            )

            QualityOptionRow(
                quality = DownloadQuality.HD_STANDARD,
                isSelected = selectedQuality == DownloadQuality.HD_STANDARD,
                fileSize = metadata.standardHdSize,
                onSelect = { onQualitySelected(DownloadQuality.HD_STANDARD) }
            )

            QualityOptionRow(
                quality = DownloadQuality.NORMAL_720P,
                isSelected = selectedQuality == DownloadQuality.NORMAL_720P,
                fileSize = metadata.normal720pSize,
                onSelect = { onQualitySelected(DownloadQuality.NORMAL_720P) }
            )

            QualityOptionRow(
                quality = DownloadQuality.LOW_480P,
                isSelected = selectedQuality == DownloadQuality.LOW_480P,
                fileSize = metadata.low480pSize,
                onSelect = { onQualitySelected(DownloadQuality.LOW_480P) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Download Button
            Button(
                onClick = { onDownloadClick(currentTrimmedMetadata, selectedQuality) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_download_selected"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
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
                        .testTag("btn_download_cover"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HD Cover", fontSize = 12.sp)
                }

                Button(
                    onClick = { onGenerateAiClick(metadata) },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("btn_ai_viral_pack"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
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
private fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color = Color.White.copy(alpha = 0.7f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun QualityOptionRow(
    quality: DownloadQuality,
    isSelected: Boolean,
    fileSize: Long,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isSelected) NeonCyan else ObsidianCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .testTag("quality_option_${quality.name}"),
        color = if (isSelected) NeonCyan.copy(alpha = 0.12f) else ObsidianSurfaceVariant
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
