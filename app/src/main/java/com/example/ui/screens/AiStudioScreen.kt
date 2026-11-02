package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiContentTone
import com.example.data.ai.AiGenerationState
import com.example.data.ai.AiViralResult
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioScreen(
    inputText: String,
    onInputChange: (String) -> Unit,
    selectedTone: AiContentTone,
    onToneSelected: (AiContentTone) -> Unit,
    generationState: AiGenerationState,
    onGenerateClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedItemIndex by remember { mutableStateOf<String?>(null) }
    var selectedPlatformTab by remember { mutableIntStateOf(0) } // 0: Facebook, 1: TikTok, 2: YouTube Shorts
    var selectedVariationIndex by remember { mutableIntStateOf(0) }

    fun copyText(text: String, label: String, key: String) {
        clipboardManager.setText(AnnotatedString(text))
        copiedItemIndex = key
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareToSocial(text: String, platformTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share $platformTitle via"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("ai_studio_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1).copy(alpha = 0.3f),
                                    Color(0xFFFF007F).copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.25f), CircleShape)
                                    .border(1.dp, Color(0xFF818CF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "AI VIRAL CREATOR STUDIO",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Unlimited, dynamic caption & hashtag generator for TikTok, Facebook Reels, and YouTube Shorts. Generates fresh random angles every time!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Input Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOPIC, VIDEO TITLE, OR NICHE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_topic_input"),
                        placeholder = {
                            Text(
                                "Enter any topic or paste video title (e.g., Balen Shah, Nepali comedy, travel, fitness, tech, podcast)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { onInputChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            onInputChange(clip)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = NeonCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = ObsidianSurfaceVariant,
                            unfocusedContainerColor = ObsidianSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tone selector
                    Text(
                        text = "TARGET PLATFORM & CONTENT TONE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiContentTone.values().forEach { tone ->
                            val isSelected = selectedTone == tone
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonMagenta else ObsidianCardBorder,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { onToneSelected(tone) }
                                    .testTag("ai_tone_${tone.name}"),
                                color = if (isSelected) NeonMagenta.copy(alpha = 0.2f) else ObsidianSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tone.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Buttons (Generate & Unlimited Randomize)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onGenerateClick,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("btn_generate_ai_pack"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            enabled = generationState !is AiGenerationState.Generating
                        ) {
                            if (generationState is AiGenerationState.Generating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking...", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Viral Pack", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Unlimited Randomize Button
                        Button(
                            onClick = onRegenerateClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_randomize_ai_pack"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonMagenta,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            enabled = generationState !is AiGenerationState.Generating
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Randomize 🎲", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (generationState is AiGenerationState.Generating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = generationState.step,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { generationState.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonCyan,
                            trackColor = ObsidianSurfaceVariant
                        )
                    }
                }
            }
        }

        // Generated Results
        if (generationState is AiGenerationState.Success) {
            val result = generationState.result

            // Score Banner & Dynamic Iteration Indicator
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, HdrGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "VIRAL POTENTIAL SCORE • ANGLE #${result.generationId}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HdrGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${result.viralScore}% High Algorithm Reach",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onRegenerateClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HdrGold, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Angle", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // ==========================================
            // MULTI-VARIATION SELECTOR (4 Distinct Flavors)
            // ==========================================
            if (result.variations.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CHOOSE CAPTION STYLE VARIATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            result.variations.forEachIndexed { index, variation ->
                                val isSelected = selectedVariationIndex == index
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonCyan else ObsidianCardBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedVariationIndex = index }
                                        .testTag("variation_chip_$index"),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else ObsidianSurfaceVariant
                                ) {
                                    Text(
                                        text = variation.styleTitle,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Variation Content
            val currentVariation = result.variations.getOrNull(selectedVariationIndex)

            // Platform Ready to Post Selector (Facebook Reels, TikTok, YouTube Shorts)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Platform Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tabs = listOf("📘 Facebook Reels", "🎵 TikTok", "▶️ YouTube Shorts")
                            tabs.forEachIndexed { idx, tabTitle ->
                                val isSelected = selectedPlatformTab == idx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonCyan else ObsidianCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedPlatformTab = idx },
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else ObsidianSurfaceVariant
                                ) {
                                    Text(
                                        text = tabTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val formattedText = when (selectedPlatformTab) {
                            0 -> currentVariation?.facebookReelsPost ?: result.facebookReadyPost
                            1 -> currentVariation?.tiktokPost ?: result.tiktokReadyPost
                            else -> currentVariation?.youtubeShortsPost ?: result.youtubeShortsReadyPost
                        }

                        val platformLabel = when (selectedPlatformTab) {
                            0 -> "Facebook Reels"
                            1 -> "TikTok"
                            else -> "YouTube Shorts"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = ObsidianSurfaceVariant
                        ) {
                            Text(
                                text = formattedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(14.dp),
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyText(formattedText, "$platformLabel Post", "post_copy") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = if (copiedItemIndex == "post_copy") Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (copiedItemIndex == "post_copy") NeonCyan else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Post", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { shareToSocial(formattedText, platformLabel) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (selectedPlatformTab) {
                                        0 -> Color(0xFF1877F2)
                                        1 -> NeonMagenta
                                        else -> Color(0xFFFF0000)
                                    }
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Viral Hooks List
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "TOP VIRAL HOOKS (FIRST 3 SECONDS)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        result.hooks.forEachIndexed { idx, hook ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp)),
                                color = ObsidianSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. $hook",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyText(hook, "Hook #${idx + 1}", "hook_$idx") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedItemIndex == "hook_$idx") Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy hook",
                                            tint = if (copiedItemIndex == "hook_$idx") NeonCyan else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trending Hashtags
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HIGH-REACH HASHTAGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonMagenta,
                                letterSpacing = 1.sp
                            )

                            OutlinedButton(
                                onClick = { copyText(result.hashtags.joinToString(" "), "All hashtags", "tags_all") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = if (copiedItemIndex == "tags_all") Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (copiedItemIndex == "tags_all") NeonCyan else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy All", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            result.hashtags.forEach { tag ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, NeonMagenta.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .clickable { copyText(tag, tag, tag) },
                                    color = NeonMagenta.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = NeonMagenta,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Audience Insight & Best Posting Times
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BEST TIME TO POST", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeonCyan)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(result.bestPostingTime, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HdrGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SOUNDTRACK VIBE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = HdrGold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(result.soundtrackRecommendation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
