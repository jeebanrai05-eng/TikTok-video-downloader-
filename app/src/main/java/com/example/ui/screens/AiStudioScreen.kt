package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.UltraPurple

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
    var selectedVariationIndex by remember { mutableIntStateOf(0) }

    fun copyText(text: String, label: String, key: String) {
        clipboardManager.setText(AnnotatedString(text))
        copiedItemIndex = key
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareToTikTok(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share TikTok Caption"))
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
        item(key = "ai_hero") {
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
                                    ElectricIndigo.copy(alpha = 0.34f),
                                    NeonMagenta.copy(alpha = 0.22f),
                                    ObsidianSurface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(UltraPurple.copy(alpha = 0.25f), CircleShape)
                                    .border(1.dp, UltraPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFC4B5FD),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "TIKTOK AI VIRAL STUDIO",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Unlimited TikTok FYP hook, caption & hashtag generator. Creates fresh high-retention angles on every tap!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Input Box
        item(key = "ai_input") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TIKTOK TOPIC, CAPTION OR NICHE",
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
                                "Enter any TikTok topic (e.g., Nepali comedy, travel vlog, dance, motivation, tech)...",
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

                    Text(
                        text = "TIKTOK CONTENT TONE & STYLE",
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
                            val chipBorder by animateColorAsState(
                                targetValue = if (isSelected) NeonMagenta else ObsidianCardBorder,
                                animationSpec = tween(180),
                                label = "tone_border"
                            )
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) NeonMagenta.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                                animationSpec = tween(180),
                                label = "tone_bg"
                            )

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(1.dp, chipBorder, RoundedCornerShape(999.dp))
                                    .clickable { onToneSelected(tone) }
                                    .testTag("ai_tone_${tone.name}"),
                                color = chipBg
                            ) {
                                Text(
                                    text = tone.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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
                                containerColor = ElectricIndigo,
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
                                Text("Generate Pack", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

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
                            Text("Randomize", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = generationState is AiGenerationState.Generating,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val gen = generationState as? AiGenerationState.Generating
                        if (gen != null) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = gen.step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { gen.progressFraction },
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
            }
        }

        // Generated Results
        if (generationState is AiGenerationState.Success) {
            val result = generationState.result

            item(key = "ai_score_banner") {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TIKTOK FYP POTENTIAL • ANGLE #${result.generationId}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HdrGold,
                                letterSpacing = 0.8.sp
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

            if (result.variations.isNotEmpty()) {
                item(key = "ai_variations") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CHOOSE TIKTOK CAPTION STYLE",
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

            val currentVariation = result.variations.getOrNull(selectedVariationIndex)

            // Ready-to-Post TikTok Caption Box
            item(key = "ai_tiktok_post") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, NeonMagenta.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎵 READY FOR TIKTOK POST",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeonMagenta,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val formattedText = currentVariation?.tiktokPost ?: result.tiktokReadyPost

                        AnimatedContent(
                            targetState = formattedText,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "caption_preview"
                        ) { previewText ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = ObsidianSurfaceVariant
                            ) {
                                Text(
                                    text = previewText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.padding(14.dp),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { copyText(formattedText, "TikTok Post", "post_copy") },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, ObsidianCardBorder)
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
                                onClick = { shareToTikTok(formattedText) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share to TikTok", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Viral Hooks List
            item(key = "ai_hooks") {
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
                                text = "TOP TIKTOK HOOKS (FIRST 3 SECONDS)",
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
                                        modifier = Modifier.size(36.dp)
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
            item(key = "ai_hashtags") {
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
                                text = "HIGH-REACH FYP HASHTAGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonMagenta,
                                letterSpacing = 1.sp
                            )

                            OutlinedButton(
                                onClick = { copyText(result.hashtags.joinToString(" "), "All hashtags", "tags_all") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, ObsidianCardBorder)
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
            item(key = "ai_insights") {
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
                            Text(result.soundtrackRecommendation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
