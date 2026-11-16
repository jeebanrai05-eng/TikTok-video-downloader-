package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Link
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
import com.example.ui.theme.UltraPurple

@Composable
fun DownloaderScreen(
    urlInput: String,
    onUrlChange: (String) -> Unit,
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
        item(key = "hero_banner") {
            HeroSpeedBanner()
        }

        // Clipboard auto-detect banner with spring animation
        item(key = "clipboard_banner") {
            AnimatedVisibility(
                visible = clipboardLink != null,
                enter = fadeIn() + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut() + shrinkVertically()
            ) {
                clipboardLink?.let { link ->
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
                                Icon(
                                    Icons.Default.ContentPasteGo,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TikTok link detected in clipboard",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )
                                Text(
                                    text = link,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = onUseClipboardLink,
                                shape = RoundedCornerShape(10.dp),
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
        }

        // Active Download in Progress Card
        item(key = "active_download") {
            AnimatedVisibility(
                visible = activeDownload != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeDownload?.let { dl ->
                    ActiveDownloadCard(
                        download = dl,
                        onCancel = onCancelDownload,
                        onPlay = { filePath -> onPreviewClick(filePath, dl.title) },
                        onDismiss = onDismissActiveDownload
                    )
                }
            }
        }

        // Main TikTok Link Input Card
        item(key = "url_input_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(ObsidianCardBorder, ObsidianCardBorder.copy(alpha = 0.4f))),
                        RoundedCornerShape(22.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PASTE TIKTOK VIDEO OR PHOTO LINK",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonMagenta.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "🎵 TikTok Only",
                                color = NeonMagenta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_url_input"),
                        placeholder = {
                            Text(
                                "https://www.tiktok.com/@user/video/... or vt.tiktok.com/...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.42f),
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
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonMagenta.copy(alpha = 0.6f)))
                            )
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
                                contentColor = Color(0xFF041417)
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

        // Quick 1-Tap Trending TikTok Samples
        item(key = "quick_samples") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔥 QUICK TIKTOK DEMOS (1-TAP TEST)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = HdrGold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "8K / Photo Slides",
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
                        val dotColor = when (index) {
                            0 -> NeonMagenta
                            1 -> HdrGold
                            else -> UltraPurple
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
                                .clickable { onSelectSample(sample) }
                                .testTag("sample_item_$index"),
                            color = ObsidianSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = sample.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${sample.category} • ${sample.author}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // High Speed Analyzing Card
        item(key = "analyzing_card") {
            AnimatedVisibility(
                visible = extractionState is ExtractionState.Analyzing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val state = extractionState as? ExtractionState.Analyzing
                if (state != null) {
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
                                text = "TikTok Stream Processor Active",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.step,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { state.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan,
                                trackColor = Color(0xFF1F2842)
                            )
                        }
                    }
                }
            }
        }

        // Extraction Error Card
        item(key = "error_card") {
            AnimatedVisibility(
                visible = extractionState is ExtractionState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val err = extractionState as? ExtractionState.Error
                if (err != null) {
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
                                    text = "Unable to Resolve TikTok Link",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ErrorRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = err.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Success: Video Metadata and Quality Download Card
        item(key = "result_card") {
            AnimatedVisibility(
                visible = extractionState is ExtractionState.Success,
                enter = fadeIn(tweenSpec()) + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                exit = fadeOut() + shrinkVertically()
            ) {
                val success = extractionState as? ExtractionState.Success
                if (success != null) {
                    ResultVideoCard(
                        metadata = success.metadata,
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
}

private fun tweenSpec() = androidx.compose.animation.core.tween<Float>(durationMillis = 280)
