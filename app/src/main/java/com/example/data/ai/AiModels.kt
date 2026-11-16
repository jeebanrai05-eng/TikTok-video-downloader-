package com.example.data.ai

data class CaptionVariation(
    val id: String,
    val styleTitle: String,
    val hook: String,
    val caption: String,
    val hashtags: List<String>,
    val tiktokPost: String
)

data class AiViralResult(
    val title: String,
    val viralScore: Int,
    val generationId: Int,
    val toneLabel: String,
    val hooks: List<String>,
    val caption: String,
    val hashtags: List<String>,
    val tiktokReadyPost: String,
    val bestPostingTime: String,
    val soundtrackRecommendation: String,
    val variations: List<CaptionVariation> = emptyList(),
    val audienceInsight: String = ""
)

enum class AiContentTone(val label: String, val badge: String, val targetCategory: String) {
    VIRAL_TREND("🔥 Viral FYP", "Max TikTok Reach", "Broad Entertainment"),
    NEPAL_POLITICS("🇳🇵 News & Nepal Issues", "High Discussion", "Public Interest"),
    COMEDY_FUNNY("😂 Comedy & Humor", "High Shares", "Fun & Memes"),
    AESTHETIC_VIBE("✨ Aesthetic & Chill", "High Saves", "Visuals & Music"),
    MOTIVATION("💪 Motivation & Life", "High Engagement", "Self Growth")
}

sealed interface AiGenerationState {
    object Idle : AiGenerationState
    data class Generating(val step: String, val progressFraction: Float) : AiGenerationState
    data class Success(val result: AiViralResult) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}
