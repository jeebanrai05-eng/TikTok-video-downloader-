package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "msg") val msg: String? = null,
    @param:Json(name = "data") val data: TikWmData? = null
)

@JsonClass(generateAdapter = true)
data class TikWmData(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "cover") val cover: String? = null,
    @param:Json(name = "play") val play: String? = null,
    @param:Json(name = "hdplay") val hdplay: String? = null,
    @param:Json(name = "wmplay") val wmplay: String? = null,
    @param:Json(name = "size") val size: Long? = null,
    @param:Json(name = "hd_size") val hdSize: Long? = null,
    @param:Json(name = "music") val music: String? = null,
    @param:Json(name = "music_info") val musicInfo: TikWmMusicInfo? = null,
    @param:Json(name = "play_count") val playCount: Long? = null,
    @param:Json(name = "digg_count") val diggCount: Long? = null,
    @param:Json(name = "comment_count") val commentCount: Long? = null,
    @param:Json(name = "share_count") val shareCount: Long? = null,
    @param:Json(name = "duration") val duration: Int? = null,
    @param:Json(name = "author") val author: TikWmAuthor? = null,
    @param:Json(name = "images") val images: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "unique_id") val uniqueId: String? = null,
    @param:Json(name = "nickname") val nickname: String? = null,
    @param:Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class TikWmMusicInfo(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "author") val author: String? = null,
    @param:Json(name = "play") val play: String? = null
)

enum class DownloadQuality(val label: String, val badge: String, val description: String) {
    ULTRA_8K_120FPS("8K Ultra 120FPS", "8K / 120FPS", "Extreme bitrate watermark-free TikTok master stream"),
    HDR_ULTRA("4K HDR+ 60FPS", "4K / HDR+", "Original high-bitrate stream with crisp HDR clarity"),
    HD_STANDARD("1080p Full HD", "1080p FHD", "Clean watermark-free TikTok HD stream"),
    NORMAL_720P("720p Fast HD", "720p HD", "Balanced quality & super-fast CDN download"),
    LOW_480P("480p Compact", "480p SD", "Lightweight file size for quick chat sharing"),
    AUDIO_ONLY("Original Sound", "320 kbps MP3", "Extract clean original TikTok soundtrack"),
    AUDIO_HIGH_SOUND("High Sound Boost", "Boosted MP3", "Enhanced dynamic range & bass clarity"),
    AUDIO_VFX_SOUND("VFX & Dialogue", "Clean Vocal", "Voiceover and sound-effect ready soundtrack"),
    COVER_IMAGE("HD Cover", "JPG Poster", "Save original full-resolution TikTok cover poster"),
    PHOTO_SLIDE_ULTRA("Photo Slide HDR+", "4K / 8K Photo", "Download TikTok photo carousel in full resolution")
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
    val ultra8kUrl: String = "",
    val ultra8kSize: Long = 0L,
    val ultraHdrUrl: String = "",
    val ultraHdrSize: Long = 0L,
    val standardHdUrl: String = "",
    val standardHdSize: Long = 0L,
    val normal720pUrl: String = "",
    val normal720pSize: Long = 0L,
    val low480pUrl: String = "",
    val low480pSize: Long = 0L,
    val audioUrl: String = "",
    val musicTitle: String = "",
    val musicAuthor: String = "",
    val originalTikTokUrl: String,
    val images: List<String> = emptyList()
) {
    val isPhotoSlide: Boolean get() = images.isNotEmpty()

    val availableQualities: List<DownloadQuality>
        get() {
            if (isPhotoSlide && standardHdUrl.isBlank() && ultraHdrUrl.isBlank()) {
                return buildList {
                    add(DownloadQuality.PHOTO_SLIDE_ULTRA)
                    if (audioUrl.isNotBlank()) add(DownloadQuality.AUDIO_ONLY)
                }
            }
            val list = mutableListOf<DownloadQuality>()
            if (ultra8kUrl.isNotBlank()) list.add(DownloadQuality.ULTRA_8K_120FPS)
            if (ultraHdrUrl.isNotBlank()) list.add(DownloadQuality.HDR_ULTRA)
            if (standardHdUrl.isNotBlank()) list.add(DownloadQuality.HD_STANDARD)
            if (normal720pUrl.isNotBlank()) list.add(DownloadQuality.NORMAL_720P)
            if (low480pUrl.isNotBlank()) list.add(DownloadQuality.LOW_480P)
            if (isPhotoSlide) list.add(0, DownloadQuality.PHOTO_SLIDE_ULTRA)
            if (list.isEmpty() && coverUrl.isNotBlank()) list.add(DownloadQuality.COVER_IMAGE)
            return list
        }
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
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Int,
    val localFilePath: String? = null,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)
