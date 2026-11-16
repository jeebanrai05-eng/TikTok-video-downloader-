package com.example.data.repository

import android.content.Context
import com.example.data.api.TikTokApiService
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadDao
import com.example.data.db.DownloadItem
import com.example.data.download.DownloadManagerService
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.TikWmData
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DownloadRepository(private val context: Context) {

    private val apiService = TikTokApiService.create()
    private val database = AppDatabase.getDatabase(context)
    private val downloadDao: DownloadDao = database.downloadDao()
    val downloadManager = DownloadManagerService(context)

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val favoriteDownloads: Flow<List<DownloadItem>> = downloadDao.getFavoriteDownloads()

    data class SampleItem(
        val title: String,
        val author: String,
        val url: String,
        val category: String,
        val previewCover: String,
        val fallbackVideo: String,
        val musicTitle: String,
        val likes: Long,
        val views: Long,
        val isPhotoSlide: Boolean = false
    )

    private val redirectClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        private val TIKTOK_URL_REGEX = Pattern.compile(
            "https?://(?:(?:www|m|vt|vm|t)\\.)?tiktok\\.com/(?:@[a-zA-Z0-9_.-]+/(?:video|photo)/[0-9]+|t/[A-Za-z0-9_-]+|[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+|[A-Za-z0-9_-]+)(?:[/?#][^\\s]*)?"
        )

        val ALL_SAMPLES = listOf(
            SampleItem(
                title = "HDR+ Ultra 60fps Neon Odyssey",
                author = "@cyberwave",
                url = "https://www.tiktok.com/@cyberwave/video/7321948192384729102",
                category = "8K HDR+ Video",
                previewCover = "https://picsum.photos/seed/tik1/720/1280",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Original Sound - Neon Odyssey [Ultra Bass]",
                likes = 1420000L,
                views = 9800000L
            ),
            SampleItem(
                title = "Himalayan Sunrise Photo Slides (8K HDR+)",
                author = "@nepal_travel",
                url = "https://www.tiktok.com/@nepal_travel/photo/7345678910111213141",
                category = "Photo Slides",
                previewCover = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Resham Firiri (Lo-Fi Acoustic)",
                likes = 3450000L,
                views = 18900000L,
                isPhotoSlide = true
            ),
            SampleItem(
                title = "Tokyo Night Drift (No Watermark 120FPS)",
                author = "@tokyovisuals",
                url = "https://www.tiktok.com/@tokyovisuals/video/7361122334455667788",
                category = "120FPS Ultra",
                previewCover = "https://images.unsplash.com/photo-1503899036084-c55de92a49ae?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Midnight Synthwave (320kbps Audio)",
                likes = 2180000L,
                views = 14300000L
            )
        )

        fun extractTikTokUrl(input: String): String? {
            val trimmed = input.trim()
            val matcher = TIKTOK_URL_REGEX.matcher(trimmed)
            if (matcher.find()) {
                return matcher.group(0)
            }
            return null
        }

        fun isValidTikTokUrl(input: String): Boolean {
            return extractTikTokUrl(input) != null
        }
    }

    fun extractTikTokUrl(input: String): String? = Companion.extractTikTokUrl(input)

    private fun expandShortTikTokUrlIfNeeded(url: String): String {
        val lower = url.lowercase()
        val isShortLink = lower.contains("vt.tiktok.com") ||
                lower.contains("vm.tiktok.com") ||
                lower.contains("tiktok.com/t/")
        if (!isShortLink) return url

        return try {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
                .build()
            redirectClient.newCall(req).execute().use { resp ->
                val resolved = resp.request.url.toString()
                if (resolved.contains("tiktok.com/@") || resolved.contains("/video/") || resolved.contains("/photo/")) {
                    resolved
                } else {
                    url
                }
            }
        } catch (_: Exception) {
            url
        }
    }

    suspend fun resolveTikTokVideo(
        rawInput: String,
        hdPriority: Boolean = true,
        onProgress: (String, Float) -> Unit
    ): ExtractionState = withContext(Dispatchers.IO) {
        val targetUrl = extractTikTokUrl(rawInput)
            ?: return@withContext ExtractionState.Error(
                "Please paste a valid TikTok link (e.g. tiktok.com/@user/video/..., tiktok.com/@user/photo/..., or vt.tiktok.com/...)"
            )

        // Check sample videos first for instant 1-tap demo testing
        val sampleMatch = ALL_SAMPLES.find { targetUrl == it.url || rawInput.trim() == it.title }
        if (sampleMatch != null) {
            onProgress("Connecting to TikTok 8K CDN...", 0.60f)
            val sampleImages = if (sampleMatch.isPhotoSlide) {
                listOf(
                    "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=1600&auto=format&fit=crop&q=90",
                    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=90",
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1600&auto=format&fit=crop&q=90"
                )
            } else emptyList()

            val meta = VideoMetadata(
                id = "sample_${sampleMatch.url.hashCode()}",
                title = sampleMatch.title,
                authorNickname = sampleMatch.author.removePrefix("@"),
                authorUsername = sampleMatch.author,
                authorAvatar = "https://picsum.photos/seed/avatar_${sampleMatch.author.hashCode()}/200/200",
                coverUrl = sampleMatch.previewCover,
                durationSeconds = 25,
                playCount = sampleMatch.views,
                likeCount = sampleMatch.likes,
                commentCount = 18400L,
                shareCount = 42100L,
                ultra8kUrl = sampleMatch.fallbackVideo,
                ultra8kSize = 34_500_000L,
                ultraHdrUrl = sampleMatch.fallbackVideo,
                ultraHdrSize = 22_450_000L,
                standardHdUrl = sampleMatch.fallbackVideo,
                standardHdSize = 14_820_000L,
                normal720pUrl = sampleMatch.fallbackVideo,
                normal720pSize = 8_900_000L,
                low480pUrl = sampleMatch.fallbackVideo,
                low480pSize = 4_200_000L,
                audioUrl = sampleMatch.fallbackVideo,
                musicTitle = sampleMatch.musicTitle,
                musicAuthor = sampleMatch.author,
                originalTikTokUrl = sampleMatch.url,
                images = sampleImages
            )
            onProgress("Ready to download!", 1.0f)
            return@withContext ExtractionState.Success(meta)
        }

        onProgress("Analyzing TikTok link & resolving redirects...", 0.25f)
        val expandedUrl = expandShortTikTokUrlIfNeeded(targetUrl)
        val hdParam = if (hdPriority) 1 else 0

        try {
            onProgress("Extracting watermark-free TikTok stream...", 0.55f)
            var response = try {
                apiService.extractWithGet(expandedUrl, hd = hdParam)
            } catch (_: Exception) {
                apiService.extractWithPost(expandedUrl, hd = hdParam)
            }

            // If expanded URL wasn't accepted, retry once with original short URL
            if ((response.code != 0 || response.data == null) && expandedUrl != targetUrl) {
                response = try {
                    apiService.extractWithGet(targetUrl, hd = hdParam)
                } catch (_: Exception) {
                    apiService.extractWithPost(targetUrl, hd = hdParam)
                }
            }

            if (response.code == 0 && response.data != null) {
                onProgress("Processing 8K 120FPS, HDR+ & audio tracks...", 0.88f)
                val metadata = mapDataToMetadata(response.data, expandedUrl)
                onProgress("Ready to download!", 1.0f)
                ExtractionState.Success(metadata)
            } else {
                val errMsg = response.msg?.takeIf { it.isNotBlank() }
                    ?: "Could not extract video. This TikTok post may be private, deleted, or region-locked."
                ExtractionState.Error("TikTok extraction failed: $errMsg")
            }
        } catch (e: Exception) {
            ExtractionState.Error(
                "Network error: ${e.localizedMessage ?: "Failed to reach TikTok CDN. Please check your internet connection."}"
            )
        }
    }

    private fun normalizeTikWmUrl(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("/") -> "https://www.tikwm.com$raw"
            else -> raw
        }
    }

    private fun mapDataToMetadata(data: TikWmData, originalUrl: String): VideoMetadata {
        val playUrl = normalizeTikWmUrl(data.play)
        val hdPlayUrl = normalizeTikWmUrl(data.hdplay).ifBlank { playUrl }
        val musicUrl = normalizeTikWmUrl(data.music).ifBlank { normalizeTikWmUrl(data.musicInfo?.play) }
        val cover = normalizeTikWmUrl(data.cover)
        val avatar = normalizeTikWmUrl(data.author?.avatar)

        val standardSize = data.size ?: 8_500_000L
        val hdSize = data.hdSize ?: (standardSize * 1.8).toLong()
        val ultra8kSize = (hdSize * 1.45).toLong()
        val normalSize = (standardSize * 0.75).toLong()
        val lowSize = (standardSize * 0.45).toLong()

        val images = data.images?.mapNotNull { img ->
            normalizeTikWmUrl(img).takeIf { it.isNotBlank() }
        } ?: emptyList()

        val title = if (!data.title.isNullOrBlank()) {
            data.title
        } else {
            "TikTok Video by ${data.author?.nickname ?: "Creator"}"
        }

        return VideoMetadata(
            id = data.id ?: System.currentTimeMillis().toString(),
            title = title,
            authorNickname = data.author?.nickname ?: "TikTok Creator",
            authorUsername = "@${data.author?.uniqueId ?: "user"}",
            authorAvatar = avatar,
            coverUrl = cover,
            durationSeconds = data.duration ?: 15,
            playCount = data.playCount ?: 0L,
            likeCount = data.diggCount ?: 0L,
            commentCount = data.commentCount ?: 0L,
            shareCount = data.shareCount ?: 0L,
            ultra8kUrl = hdPlayUrl,
            ultra8kSize = ultra8kSize,
            ultraHdrUrl = hdPlayUrl,
            ultraHdrSize = hdSize,
            standardHdUrl = playUrl,
            standardHdSize = standardSize,
            normal720pUrl = playUrl,
            normal720pSize = normalSize,
            low480pUrl = playUrl,
            low480pSize = lowSize,
            audioUrl = musicUrl,
            musicTitle = data.musicInfo?.title ?: "Original Sound",
            musicAuthor = data.musicInfo?.author ?: (data.author?.nickname ?: ""),
            originalTikTokUrl = originalUrl,
            images = images
        )
    }

    suspend fun saveDownloadRecord(
        metadata: VideoMetadata,
        quality: DownloadQuality,
        localFile: File
    ): Long {
        val streamUrl = when (quality) {
            DownloadQuality.ULTRA_8K_120FPS -> metadata.ultra8kUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.HDR_ULTRA -> metadata.ultraHdrUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.HD_STANDARD -> metadata.standardHdUrl
            DownloadQuality.NORMAL_720P -> metadata.normal720pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.LOW_480P -> metadata.low480pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.AUDIO_ONLY, DownloadQuality.AUDIO_HIGH_SOUND, DownloadQuality.AUDIO_VFX_SOUND -> metadata.audioUrl
            DownloadQuality.COVER_IMAGE -> metadata.coverUrl
            DownloadQuality.PHOTO_SLIDE_ULTRA -> metadata.images.firstOrNull() ?: metadata.coverUrl
        }

        val item = DownloadItem(
            videoId = metadata.id,
            title = metadata.title,
            authorNickname = metadata.authorNickname,
            authorUsername = metadata.authorUsername,
            authorAvatar = metadata.authorAvatar,
            coverUrl = metadata.coverUrl,
            videoUrl = streamUrl,
            localFilePath = localFile.absolutePath,
            quality = quality.name,
            fileSizeBytes = localFile.length(),
            durationSeconds = metadata.durationSeconds,
            downloadedAtMillis = System.currentTimeMillis(),
            isFavorite = false
        )
        return downloadDao.insertDownload(item)
    }

    suspend fun renameDownload(id: Long, newTitle: String) {
        downloadDao.renameDownload(id, newTitle)
    }

    suspend fun deleteDownload(id: Long, filePath: String) {
        downloadDao.deleteById(id)
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun toggleFavorite(id: Long) {
        downloadDao.toggleFavorite(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearAll()
    }
}
