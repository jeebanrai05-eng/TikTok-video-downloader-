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

enum class DownloadQuality(val label: String, val badge: String, val description: String) {
    ULTRA_8K_120FPS("8K Ultra 120FPS", "8K / 120FPS", "Extreme bitrate 8K 120FPS ultra-smooth stream with zero watermark"),
    HDR_ULTRA("HDR+ Ultra", "4K / 1080p Ultra", "Original bitrate stream with enhanced HDR fidelity & zero watermark"),
    HD_STANDARD("HD Fast", "1080p / 720p", "High-speed clean video without TikTok watermark"),
    AUDIO_ONLY("Audio MP3", "320 kbps", "Extract studio quality background audio soundtrack"),
    COVER_IMAGE("HD Cover", "JPG Poster", "Download original full-resolution video cover")
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
    val audioUrl: String,
    val musicTitle: String,
    val musicAuthor: String,
    val originalTikTokUrl: String
)

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
