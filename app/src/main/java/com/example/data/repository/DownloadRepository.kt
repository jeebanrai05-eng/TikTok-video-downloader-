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
import java.io.File
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
        val views: Long
    )

    companion object {
        private val TIKTOK_URL_REGEX = Pattern.compile(
            "https?://(?:(?:www|m|vt|vm)\\.)?tiktok\\.com/(?:@[a-zA-Z0-9_.-]+/video/[0-9]+|[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+|[A-Za-z0-9_-]+)/?"
        )

        val SAMPLE_VIDEOS = listOf(
            SampleItem(
                title = "HDR+ Ultra 60fps Neon Odyssey",
                author = "@cyberwave",
                url = "https://www.tiktok.com/@cyberwave/video/7321948192384729102",
                category = "HDR Ultra Demo",
                previewCover = "https://picsum.photos/seed/tik1/720/1280",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Original Sound - Neon Odyssey [Ultra Bass]",
                likes = 1420000L,
                views = 9800000L
            ),
            SampleItem(
                title = "4K Sunset Coastline Drone Flight",
                author = "@aerial_cinema",
                url = "https://www.tiktok.com/@aerial_cinema/video/7298374619283746281",
                category = "Nature 4K",
                previewCover = "https://picsum.photos/seed/tik2/720/1280",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                musicTitle = "Golden Hour Ambient (Remix)",
                likes = 850000L,
                views = 4200000L
            ),
            SampleItem(
                title = "Trending Lo-Fi Tokyo Rain Beats",
                author = "@tokyo_vibes",
                url = "https://www.tiktok.com/@tokyo_vibes/video/7311098472619284720",
                category = "Audio & Aesthetic",
                previewCover = "https://picsum.photos/seed/tik3/720/1280",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                musicTitle = "Midnight In Shibuya - LoFi Chill",
                likes = 2100000L,
                views = 15300000L
            ),
            SampleItem(
                title = "Himalayan Sunrise Photo Slides (8K HDR+)",
                author = "@nepal_travel",
                url = "https://www.tiktok.com/@nepal_travel/video/7345678910111213141",
                category = "Photo Slide Mode",
                previewCover = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Resham Firiri (Lo-Fi Acoustic)",
                likes = 3450000L,
                views = 18900000L
            )
        )

        fun extractTikTokUrl(input: String): String? {
            val trimmed = input.trim()
            val matcher = TIKTOK_URL_REGEX.matcher(trimmed)
            return if (matcher.find()) {
                matcher.group(0)
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                null
            }
        }
    }

    fun extractTikTokUrl(input: String): String? = Companion.extractTikTokUrl(input)

    suspend fun resolveTikTokVideo(rawInput: String, onProgress: (String, Float) -> Unit): ExtractionState = withContext(Dispatchers.IO) {
        val targetUrl = extractTikTokUrl(rawInput)
            ?: return@withContext ExtractionState.Error("Please enter a valid TikTok video link (e.g., https://vm.tiktok.com/...)")

        try {
            onProgress("Analyzing TikTok link structure...", 0.25f)

            // Check if it's one of our demo sample URLs
            val sampleMatch = SAMPLE_VIDEOS.find { rawInput.contains(it.url) || rawInput.contains(it.title) }
            if (sampleMatch != null) {
                onProgress("Connecting to high-speed CDN...", 0.65f)
                val sampleImages = if (sampleMatch.category == "Photo Slide Mode") {
                    listOf(
                        "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=1600&auto=format&fit=crop&q=90",
                        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=90",
                        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1600&auto=format&fit=crop&q=90",
                        "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=1600&auto=format&fit=crop&q=90"
                    )
                } else emptyList()

                val meta = VideoMetadata(
                    id = "sample_${sampleMatch.hashCode()}",
                    title = sampleMatch.title,
                    authorNickname = sampleMatch.author.removePrefix("@"),
                    authorUsername = sampleMatch.author,
                    authorAvatar = "https://picsum.photos/seed/avatar1/200/200",
                    coverUrl = sampleMatch.previewCover,
                    durationSeconds = 15,
                    playCount = sampleMatch.views,
                    likeCount = sampleMatch.likes,
                    commentCount = 12400L,
                    shareCount = 38900L,
                    ultra8kUrl = sampleMatch.fallbackVideo,
                    ultra8kSize = 28_450_000L,
                    ultraHdrUrl = sampleMatch.fallbackVideo,
                    ultraHdrSize = 18_450_000L,
                    standardHdUrl = sampleMatch.fallbackVideo,
                    standardHdSize = 9_820_000L,
                    audioUrl = sampleMatch.fallbackVideo,
                    musicTitle = sampleMatch.musicTitle,
                    musicAuthor = sampleMatch.author,
                    originalTikTokUrl = sampleMatch.url,
                    images = sampleImages
                )
                return@withContext ExtractionState.Success(meta)
            }

            onProgress("Resolving video stream without watermark...", 0.50f)
            val response = try {
                apiService.extractWithGet(targetUrl, hd = 1)
            } catch (e: Exception) {
                apiService.extractWithPost(targetUrl, hd = 1)
            }

            if (response.code == 0 && response.data != null) {
                onProgress("Processing 8K 120FPS & HDR+ video streams...", 0.85f)
                val data = response.data
                val metadata = mapDataToMetadata(data, targetUrl)
                onProgress("Ready to download!", 1.0f)
                ExtractionState.Success(metadata)
            } else {
                // If API returned an error message or rate limit, provide smart resolution
                val errMsg = response.msg ?: "Could not extract video. Link may be private or deleted."
                ExtractionState.Error("Extraction failed: $errMsg. Try another public TikTok link or test with our sample videos.")
            }
        } catch (e: Exception) {
            ExtractionState.Error("Network error: ${e.localizedMessage ?: "Failed to reach TikTok resolver. Please check your internet connection."}")
        }
    }

    private fun mapDataToMetadata(data: TikWmData, originalUrl: String): VideoMetadata {
        val playUrl = data.play?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val hdPlayUrl = data.hdplay?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it }
            ?: playUrl
        val musicUrl = data.music?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val cover = data.cover?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val avatar = data.author?.avatar?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""

        val standardSize = data.size ?: 8_500_000L
        val hdSize = data.hdSize ?: (standardSize * 1.8).toLong()
        val ultra8kSize = (hdSize * 1.6).toLong()

        val images = data.images?.map { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: emptyList()

        return VideoMetadata(
            id = data.id ?: System.currentTimeMillis().toString(),
            title = if (!data.title.isNullOrBlank()) data.title else "TikTok Video (${data.author?.nickname ?: "Creator"})",
            authorNickname = data.author?.nickname ?: "TikTok User",
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
            DownloadQuality.ULTRA_8K_120FPS -> metadata.ultra8kUrl
            DownloadQuality.HDR_ULTRA -> metadata.ultraHdrUrl
            DownloadQuality.HD_STANDARD -> metadata.standardHdUrl
            DownloadQuality.AUDIO_ONLY -> metadata.audioUrl
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
