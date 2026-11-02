package com.example.data.ai

data class CaptionVariation(
    val id: String,
    val styleTitle: String, // e.g. "⚡ Viral Hook (High CTR)", "😂 Comedy & Relatable", "📖 Story & Intrigue", "✨ Aesthetic & Poetic"
    val hook: String,
    val caption: String,
    val hashtags: List<String>,
    val facebookReelsPost: String,
    val tiktokPost: String,
    val youtubeShortsPost: String
)

data class AiViralResult(
    val title: String,
    val viralScore: Int, // e.g. 97
    val generationId: Int, // Incremented on every random regeneration
    val toneLabel: String,
    val hooks: List<String>,
    val caption: String,
    val hashtags: List<String>,
    val facebookReadyPost: String,
    val tiktokReadyPost: String,
    val youtubeShortsReadyPost: String = "",
    val bestPostingTime: String,
    val soundtrackRecommendation: String,
    val variations: List<CaptionVariation> = emptyList(),
    val audienceInsight: String = ""
)

enum class AiContentTone(val label: String, val badge: String, val targetCategory: String) {
    VIRAL_TREND("🔥 Viral Trend", "Max FYP Reach", "Broad Entertainment"),
    YOUTUBE_TO_REELS("▶️ YouTube to FB Reels", "High CTR Facebook", "Reels & Engagement"),
    FACEBOOK_TO_SHORTS("👥 FB to TikTok / Shorts", "Algorithm Boost", "Shorts & TikTok FYP"),
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
