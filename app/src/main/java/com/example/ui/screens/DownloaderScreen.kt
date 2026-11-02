package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.MediaPlatform
import com.example.data.model.VideoMetadata
import com.example.data.repository.DownloadRepository
import com.example.ui.components.ActiveDownloadCard
import com.example.ui.components.HeroSpeedBanner
import com.example.ui.components.ResultVideoCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun DownloaderScreen(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    selectedPlatform: MediaPlatform,
    onPlatformChange: (MediaPlatform) -> Unit,
    clipboardLink: String?,
    onUseClipboardLink: () -> Unit,
    onDismissClipboard: () -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onExtractClick: () -> Unit,
    onSelectSample: (DownloadRepository.SampleItem) -> Unit,
    extractionState: ExtractionState,
    selectedQuality: DownloadQuality,
    onQualitySelected: (DownloadQuality) -> Unit,
    onDownloadClick: (VideoMetadata, DownloadQuality) -> Unit,
    onPreviewClick: (url: String, title: String) -> Unit,
    onGenerateAiClick: (VideoMetadata) -> Unit,
    onDownloadPhotoSlide: (url: String, index: Int) -> Unit,
    activeDownload: ActiveDownload?,
    onCancelDownload: () -> Unit,
    onDismissActiveDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            HeroSpeedBanner()
        }

        // Platform Switcher Bar (TikTok, YouTube to FB Reels, Facebook to Shorts)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CHOOSE DOWNLOADER & CLIPPER MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaPlatform.values().forEach { platform ->
                            val isSelected = selectedPlatform == platform
                            val activeColor = when (platform) {
                                MediaPlatform.TIKTOK -> NeonMagenta
                                MediaPlatform.YOUTUBE_TO_REELS -> Color(0xFFFF0000)
                                MediaPlatform.FACEBOOK_TO_SHORTS -> Color(0xFF1877F2)
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) activeColor else ObsidianCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onPlatformChange(platform) }
                                    .testTag("platform_tab_${platform.name}"),
                                color = if (isSelected) activeColor.copy(alpha = 0.2f) else ObsidianSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = platform.badge,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clipboard auto-detect chip/banner
        if (clipboardLink != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("clipboard_prompt_card"),
                    color = ObsidianSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentPasteGo, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link detected in clipboard",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan
                            )
                            Text(
                                text = clipboardLink,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onUseClipboardLink,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            modifier = Modifier.testTag("btn_paste_detected_link")
                        ) {
                            Text("Use Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismissClipboard) {
                            Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Active Download in Progress Card
        if (activeDownload != null) {
            item {
                ActiveDownloadCard(
                    download = activeDownload,
                    onCancel = onCancelDownload,
                    onPlay = { filePath -> onPreviewClick(filePath, activeDownload.title) },
                    onDismiss = onDismissActiveDownload
                )
            }
        }

        // Main Link Input Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val promptTitle = when (selectedPlatform) {
                        MediaPlatform.YOUTUBE_TO_REELS -> "PASTE YOUTUBE VIDEO / SHORTS LINK"
                        MediaPlatform.FACEBOOK_TO_SHORTS -> "PASTE FACEBOOK VIDEO / REEL LINK"
                        MediaPlatform.TIKTOK -> "PASTE TIKTOK VIDEO OR PHOTO SLIDES LINK"
                    }

                    val placeholderText = when (selectedPlatform) {
                        MediaPlatform.YOUTUBE_TO_REELS -> "Paste YouTube link (auto-formats for Facebook Reels)..."
                        MediaPlatform.FACEBOOK_TO_SHORTS -> "Paste Facebook video link (ready for TikTok & Shorts)..."
                        MediaPlatform.TIKTOK -> "https://www.tiktok.com/@user/video/..."
                    }

                    Text(
                        text = promptTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_url_input"),
                        placeholder = {
                            Text(
                                placeholderText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = onClearClick) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onExtractClick()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = ObsidianSurfaceVariant,
                            unfocusedContainerColor = ObsidianSurfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons (Paste & Fetch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPasteClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("paste_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonMagenta.copy(alpha = 0.5f))))
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Link", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onExtractClick()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("extract_video_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch Video", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Quick 1-Tap Trending Samples (For instant testing without leaving app)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔥 QUICK DEMO VIDEOS (1-TAP TEST)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = HdrGold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Instant 8K/4K HDR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DownloadRepository.ALL_SAMPLES.forEachIndexed { index, sample ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectSample(sample) }
                                .testTag("sample_item_$index"),
                            color = ObsidianSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (index == 0) NeonMagenta else if (index == 1) Color(0xFFFF0000) else Color(0xFF1877F2), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sample.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${sample.category} • ${sample.author}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // High Speed Analyzing Card
        if (extractionState is ExtractionState.Analyzing) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .testTag("analyzing_state_card"),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "High-Speed Processor Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = extractionState.step,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { extractionState.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = Color(0xFF242738)
                        )
                    }
                }
            }
        }

        // Extraction Error Card
        if (extractionState is ExtractionState.Error) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("extraction_error_card"),
                    color = ObsidianSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unable to Resolve Video",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ErrorRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = extractionState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Success: Video Metadata and Quality Download Card
        if (extractionState is ExtractionState.Success) {
            item {
                ResultVideoCard(
                    metadata = extractionState.metadata,
                    selectedQuality = selectedQuality,
                    onQualitySelected = onQualitySelected,
                    onDownloadClick = onDownloadClick,
                    onPreviewClick = onPreviewClick,
                    onGenerateAiClick = onGenerateAiClick,
                    onDownloadPhotoSlide = onDownloadPhotoSlide
                )
            }
        }
    }
}
