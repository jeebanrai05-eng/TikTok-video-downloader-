package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: TikWmData? = null
)

@JsonClass(generateAdapter = true)
data class TikWmData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "play") val play: String? = null,
    @Json(name = "hdplay") val hdplay: String? = null,
    @Json(name = "wmplay") val wmplay: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "hd_size") val hdSize: Long? = null,
    @Json(name = "music") val music: String? = null,
    @Json(name = "music_info") val musicInfo: TikWmMusicInfo? = null,
    @Json(name = "play_count") val playCount: Long? = null,
    @Json(name = "digg_count") val diggCount: Long? = null,
    @Json(name = "comment_count") val commentCount: Long? = null,
    @Json(name = "share_count") val shareCount: Long? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "author") val author: TikWmAuthor? = null,
    @Json(name = "images") val images: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    @Json(name = "id") val id: String? = null,
    @Json(name = "unique_id") val uniqueId: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class TikWmMusicInfo(
    @Json(name = "title") val title: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "play") val play: String? = null
)

enum class MediaPlatform(val label: String, val badge: String, val destination: String) {
    TIKTOK("TikTok Downloader", "🎵 TikTok", "No Watermark HD"),
    YOUTUBE_TO_REELS("YouTube to Facebook Reels", "▶️ YouTube", "Ready for Facebook Reels"),
    FACEBOOK_TO_SHORTS("Facebook to TikTok / Shorts", "👥 Facebook", "Ready for TikTok & Shorts")
}

enum class ClipPreset(val label: String, val seconds: Int, val description: String) {
    SEC_15("15s Viral Reel", 15, "Fast-paced high-retention reel"),
    SEC_30("30s Short Clip", 30, "Optimal TikTok & FB Reel duration"),
    SEC_60("60s Full Reel", 60, "Complete storytelling clip"),
    SEC_90("90s Max Reel", 90, "Extended Facebook Reel format"),
    FULL("Full Length", 0, "Original full length video stream")
}

enum class DownloadQuality(val label: String, val badge: String, val description: String) {
    ULTRA_8K_120FPS("8K Ultra 120FPS", "8K / 120FPS", "Extreme bitrate 8K 120FPS ultra-smooth stream with zero watermark"),
    HDR_ULTRA("4K HDR+ 60FPS", "4K / HDR+", "Original high-bitrate stream with enhanced HDR fidelity & vibrant colors"),
    HD_STANDARD("1080p Full HD", "1080p FHD", "Crisp high-definition clean video ready to post"),
    NORMAL_720P("720p Normal", "720p HD", "Balanced quality and fast download for immediate sharing"),
    LOW_480P("480p Low Fast", "480p SD", "Compact lightweight clip for fast messaging & low data"),
    AUDIO_ONLY("Original Sound", "320 kbps MP3", "Extract studio quality original audio soundtrack"),
    AUDIO_HIGH_SOUND("High Sound Boost", "Boosted MP3", "High volume dynamic audio with enhanced clarity & bass (+6dB)"),
    AUDIO_VFX_SOUND("VFX & Dialogue", "Clean Vocal", "Isolate clear dialogue, voiceovers, and sound effects"),
    COVER_IMAGE("HD Cover", "JPG Poster", "Download original full-resolution video cover poster"),
    PHOTO_SLIDE_ULTRA("Photo Slide HDR+", "4K / 8K Photo", "Download photo slides in pristine HDR+ ultra high resolution")
}

data class VideoMetadata(
    val id: String,
    val title: String,
    val authorNickname: String,
    val authorUsername: String,
    val authorAvatar: String,
    val coverUrl: String,
    val durationSeconds: Int,
    val playCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long,
    val ultra8kUrl: String,
    val ultra8kSize: Long,
    val ultraHdrUrl: String,
    val ultraHdrSize: Long,
    val standardHdUrl: String,
    val standardHdSize: Long,
    val normal720pUrl: String = "",
    val normal720pSize: Long = 0L,
    val low480pUrl: String = "",
    val low480pSize: Long = 0L,
    val audioUrl: String,
    val musicTitle: String,
    val musicAuthor: String,
    val originalTikTokUrl: String,
    val images: List<String> = emptyList(),
    val platform: MediaPlatform = MediaPlatform.TIKTOK,
    val clipPreset: ClipPreset = ClipPreset.FULL,
    val facebookReelsReadyCaption: String = "",
    val tiktokShortsReadyCaption: String = ""
) {
    val isPhotoSlide: Boolean get() = images.isNotEmpty()
}

sealed interface ExtractionState {
    object Idle : ExtractionState
    data class Analyzing(val step: String, val progressFraction: Float) : ExtractionState
    data class Success(val metadata: VideoMetadata) : ExtractionState
    data class Error(val message: String) : ExtractionState
}

data class ActiveDownload(
    val videoId: String,
    val title: String,
    val quality: DownloadQuality,
    val progress: Float, // 0.0 to 1.0
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Int,
    val localFilePath: String? = null,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)
