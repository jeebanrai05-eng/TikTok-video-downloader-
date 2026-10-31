package com.example.data.ai

data class AiViralResult(
    val title: String,
    val viralScore: Int, // e.g. 96
    val hooks: List<String>,
    val caption: String,
    val hashtags: List<String>,
    val facebookReadyPost: String,
    val tiktokReadyPost: String,
    val bestPostingTime: String,
    val soundtrackRecommendation: String
)

enum class AiContentTone(val label: String, val badge: String) {
    VIRAL_TREND("🔥 Viral Trend", "Max FYP Reach"),
    NEPAL_POLITICS("🇳🇵 News & Politics", "High Discussion"),
    COMEDY_FUNNY("😂 Comedy & Humor", "High Shares"),
    AESTHETIC_VIBE("✨ Aesthetic & Chill", "High Saves"),
    MOTIVATION("💪 Motivation & Life", "High Engagement")
}

sealed interface AiGenerationState {
    object Idle : AiGenerationState
    data class Generating(val step: String, val progressFraction: Float) : AiGenerationState
    data class Success(val result: AiViralResult) : AiGenerationState
    data class Error(val message: String) : AiGenerationState
}
