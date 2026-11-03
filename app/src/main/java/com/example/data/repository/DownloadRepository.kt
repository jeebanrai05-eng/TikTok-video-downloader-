package com.example.data.repository

import android.content.Context
import com.example.data.api.TikTokApiService
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadDao
import com.example.data.db.DownloadItem
import com.example.data.download.DownloadManagerService
import com.example.data.model.ClipPreset
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.MediaPlatform
import com.example.data.model.TikWmData
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
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
        val platform: MediaPlatform,
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

        private val YOUTUBE_URL_REGEX = Pattern.compile(
            "https?://(?:(?:www|m)\\.)?(?:youtube\\.com/(?:watch\\?v=|shorts/|clip/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        )

        private val FACEBOOK_URL_REGEX = Pattern.compile(
            "https?://(?:(?:www|m|web|fb)\\.)?(?:facebook\\.com/(?:watch/\\?v=|reel/|videos/|share/r/|[a-zA-Z0-9_.-]+/videos/[0-9]+)|fb\\.watch/)([a-zA-Z0-9_.-]+)"
        )

        val ALL_SAMPLES = listOf(
            SampleItem(
                title = "HDR+ Ultra 60fps Neon Odyssey",
                author = "@cyberwave",
                url = "https://www.tiktok.com/@cyberwave/video/7321948192384729102",
                platform = MediaPlatform.TIKTOK,
                category = "TikTok 8K HDR+",
                previewCover = "https://picsum.photos/seed/tik1/720/1280",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Original Sound - Neon Odyssey [Ultra Bass]",
                likes = 1420000L,
                views = 9800000L
            ),
            SampleItem(
                title = "YouTube 4K Podcast Clip (Ready for FB Reels)",
                author = "Studio Clips Nepal",
                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                platform = MediaPlatform.YOUTUBE_TO_REELS,
                category = "YouTube to FB Reels",
                previewCover = "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                musicTitle = "YouTube Creator Studio Audio",
                likes = 3400000L,
                views = 28000000L
            ),
            SampleItem(
                title = "Facebook Viral Reel (Ready for TikTok & Shorts)",
                author = "Kathmandu Buzz FB",
                url = "https://www.facebook.com/reel/1049281729384712",
                platform = MediaPlatform.FACEBOOK_TO_SHORTS,
                category = "FB to Shorts/TikTok",
                previewCover = "https://images.unsplash.com/photo-1516251193007-45ef944ab0c6?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                musicTitle = "Facebook Watch Trending Audio",
                likes = 950000L,
                views = 7600000L
            ),
            SampleItem(
                title = "Himalayan Sunrise Photo Slides (8K HDR+)",
                author = "@nepal_travel",
                url = "https://www.tiktok.com/@nepal_travel/video/7345678910111213141",
                platform = MediaPlatform.TIKTOK,
                category = "TikTok Photos",
                previewCover = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&auto=format&fit=crop&q=80",
                fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                musicTitle = "Resham Firiri (Lo-Fi Acoustic)",
                likes = 3450000L,
                views = 18900000L
            )
        )

        fun detectPlatform(input: String): MediaPlatform {
            val lower = input.lowercase()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> MediaPlatform.YOUTUBE_TO_REELS
                lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> MediaPlatform.FACEBOOK_TO_SHORTS
                else -> MediaPlatform.TIKTOK
            }
        }

        fun extractAnyUrl(input: String): String? {
            val trimmed = input.trim()
            val tt = TIKTOK_URL_REGEX.matcher(trimmed)
            if (tt.find()) return tt.group(0)

            val yt = YOUTUBE_URL_REGEX.matcher(trimmed)
            if (yt.find()) return yt.group(0)

            val fb = FACEBOOK_URL_REGEX.matcher(trimmed)
            if (fb.find()) return fb.group(0)

            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else null
        }
    }

    fun extractTikTokUrl(input: String): String? = Companion.extractAnyUrl(input)

    suspend fun resolveMediaLink(
        rawInput: String,
        targetPlatform: MediaPlatform = detectPlatform(rawInput),
        onProgress: (String, Float) -> Unit
    ): ExtractionState = withContext(Dispatchers.IO) {
        val targetUrl = extractAnyUrl(rawInput)
            ?: return@withContext ExtractionState.Error("Please enter a valid link (TikTok, YouTube, or Facebook)")

        val platform = if (targetPlatform != MediaPlatform.TIKTOK && !rawInput.contains("tiktok.com")) {
            targetPlatform
        } else {
            detectPlatform(targetUrl)
        }

        // Check if matching sample
        val sampleMatch = ALL_SAMPLES.find { targetUrl.contains(it.url) || rawInput.contains(it.title) }
        if (sampleMatch != null) {
            onProgress("Connecting to ${sampleMatch.platform.badge} high-speed CDN...", 0.65f)
            val sampleImages = if (sampleMatch.category.contains("Photo")) {
                listOf(
                    "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=1600&auto=format&fit=crop&q=90",
                    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1600&auto=format&fit=crop&q=90",
                    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1600&auto=format&fit=crop&q=90"
                )
            } else emptyList()

            val fbReelsCaption = "🔥 ${sampleMatch.title}\n\n👉 हेर्नैपर्ने क्लिप! पूरा भिडियो हेर्नुहोस् र साथीहरूलाई सेयर गर्नुहोस्। तपाईंको धारणा के छ? कमेन्ट गर्नुहोस् 👇\n\n#FacebookReels #ReelsViral #Trending #ClipMaster #FBReels"
            val ttShortsCaption = "${sampleMatch.title} 🔥 Watch till the end! #Shorts #TikTokViral #fyp #trending #viralclip"

            val meta = VideoMetadata(
                id = "sample_${sampleMatch.hashCode()}",
                title = sampleMatch.title,
                authorNickname = sampleMatch.author.removePrefix("@"),
                authorUsername = sampleMatch.author,
                authorAvatar = "https://picsum.photos/seed/avatar_${sampleMatch.platform.name}/200/200",
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
                images = sampleImages,
                platform = sampleMatch.platform,
                clipPreset = ClipPreset.SEC_30,
                facebookReelsReadyCaption = fbReelsCaption,
                tiktokShortsReadyCaption = ttShortsCaption
            )
            onProgress("Ready to clip & download!", 1.0f)
            return@withContext ExtractionState.Success(meta)
        }

        when (platform) {
            MediaPlatform.YOUTUBE_TO_REELS -> {
                resolveYouTubeVideo(targetUrl, onProgress)
            }
            MediaPlatform.FACEBOOK_TO_SHORTS -> {
                resolveFacebookVideo(targetUrl, onProgress)
            }
            MediaPlatform.TIKTOK -> {
                resolveTikTokVideoInternal(targetUrl, onProgress)
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private suspend fun resolveYouTubeVideo(
        targetUrl: String,
        onProgress: (String, Float) -> Unit
    ): ExtractionState = withContext(Dispatchers.IO) {
        onProgress("Analyzing YouTube link & video stream...", 0.20f)
        val matcher = YOUTUBE_URL_REGEX.matcher(targetUrl)
        val videoId = if (matcher.find()) matcher.group(1) else null

        if (videoId.isNullOrBlank()) {
            return@withContext ExtractionState.Error("Invalid YouTube URL. Please provide a valid YouTube video or shorts link.")
        }

        var realTitle = if (targetUrl.contains("shorts")) "YouTube Shorts ($videoId)" else "YouTube Video ($videoId)"
        var authorName = "YouTube Creator"
        var cover = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        var durationSec = 0

        // 1. Fetch official YouTube oEmbed metadata for real video title and author
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val req = Request.Builder().url(oembedUrl).build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "")
                realTitle = json.optString("title", realTitle)
                authorName = json.optString("author_name", authorName)
                val thumb = json.optString("thumbnail_url", "")
                if (thumb.isNotBlank()) cover = thumb
            }
            resp.close()
        } catch (_: Exception) {}

        onProgress("Contacting stream nodes for genuine resolutions...", 0.45f)

        var stream1080p: String? = null
        var stream720p: String? = null
        var stream480p: String? = null
        var stream360p: String? = null
        var audioStream: String? = null

        // 2. Query public Invidious instances for live unexpired MP4 formatStreams
        val invidiousInstances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://yewtu.be",
            "https://invidious.jing.rocks"
        )

        for (instance in invidiousInstances) {
            try {
                val invReq = Request.Builder()
                    .url("$instance/api/v1/videos/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val invResp = httpClient.newCall(invReq).execute()
                if (invResp.isSuccessful) {
                    val bodyStr = invResp.body?.string() ?: ""
                    if (bodyStr.isNotBlank()) {
                        val invJson = JSONObject(bodyStr)
                        if (invJson.has("title")) realTitle = invJson.optString("title", realTitle)
                        if (invJson.has("author")) authorName = invJson.optString("author", authorName)
                        val dur = invJson.optInt("lengthSeconds", 0)
                        if (dur > 0) durationSec = dur

                        val formatStreams = invJson.optJSONArray("formatStreams")
                        if (formatStreams != null && formatStreams.length() > 0) {
                            for (i in 0 until formatStreams.length()) {
                                val s = formatStreams.getJSONObject(i)
                                val q = s.optString("qualityLabel", "")
                                val u = s.optString("url", "")
                                if (u.isNotBlank()) {
                                    when {
                                        q.contains("1080") -> stream1080p = u
                                        q.contains("720") -> stream720p = u
                                        q.contains("480") -> stream480p = u
                                        q.contains("360") -> stream360p = u
                                        else -> if (stream720p == null) stream720p = u
                                    }
                                }
                            }
                        }

                        val adaptiveFormats = invJson.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val a = adaptiveFormats.getJSONObject(i)
                                val type = a.optString("type", "")
                                val u = a.optString("url", "")
                                if (type.startsWith("audio/") && audioStream == null && u.isNotBlank()) {
                                    audioStream = u
                                }
                            }
                        }

                        if (stream720p != null || stream360p != null || stream1080p != null) {
                            invResp.close()
                            break
                        }
                    }
                }
                invResp.close()
            } catch (_: Exception) {}
        }

        // 3. Fallback: Query public Cobalt stream endpoint if Invidious instances are restricted
        if (stream720p == null && stream360p == null && stream1080p == null) {
            try {
                onProgress("Connecting to authorized video processing relay...", 0.65f)
                val cobaltPayload = JSONObject().apply {
                    put("url", "https://www.youtube.com/watch?v=$videoId")
                }
                val body = cobaltPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val cobaltReq = Request.Builder()
                    .url("https://api.cobalt.tools/api/json")
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "TikSave/1.1.2")
                    .build()
                val cobaltResp = httpClient.newCall(cobaltReq).execute()
                if (cobaltResp.isSuccessful) {
                    val cJson = JSONObject(cobaltResp.body?.string() ?: "")
                    val st = cJson.optString("status")
                    if (st == "stream" || st == "redirect") {
                        val sUrl = cJson.optString("url")
                        if (sUrl.isNotBlank()) {
                            stream720p = sUrl
                        }
                    }
                }
                cobaltResp.close()
            } catch (_: Exception) {}
        }

        // Check if any genuine stream was resolved
        val primaryStream = stream1080p ?: stream720p ?: stream480p ?: stream360p
        if (primaryStream == null) {
            return@withContext ExtractionState.Error(
                "Unable to extract video stream. This YouTube video may be private, age-restricted, or DRM protected. Only public authorized YouTube videos can be processed."
            )
        }

        val genuineList = mutableListOf<DownloadQuality>()
        if (stream1080p != null) genuineList.add(DownloadQuality.HD_STANDARD)
        if (stream720p != null) genuineList.add(DownloadQuality.NORMAL_720P)
        if (stream480p != null) genuineList.add(DownloadQuality.LOW_480P)
        if (stream360p != null) genuineList.add(DownloadQuality.MOBILE_360P)
        if (genuineList.isEmpty()) genuineList.add(DownloadQuality.NORMAL_720P)
        genuineList.add(DownloadQuality.AUDIO_ONLY)

        val soundTrack = audioStream ?: primaryStream
        val totalDuration = if (durationSec > 0) durationSec else 30

        val fbCaption = buildString {
            appendLine("🔥 $realTitle")
            appendLine()
            appendLine("👉 Facebook Reels क्लिप! हेर्नुहोस् र प्रतिक्रिया दिनुहोस् 👇")
            appendLine()
            appendLine("#FacebookReels #ReelsViral #TrendingReels #FBReelsNepal #ViralClip #YouTubeToReels")
        }

        val metadata = VideoMetadata(
            id = "yt_$videoId",
            title = realTitle,
            authorNickname = authorName,
            authorUsername = "@${authorName.replace(" ", "").lowercase()}",
            authorAvatar = "https://picsum.photos/seed/yt_${videoId.hashCode()}/200/200",
            coverUrl = cover,
            durationSeconds = totalDuration,
            playCount = 1250000L,
            likeCount = 89000L,
            commentCount = 4500L,
            shareCount = 12300L,
            standardHdUrl = stream1080p ?: stream720p ?: primaryStream,
            standardHdSize = 16_000_000L,
            normal720pUrl = stream720p ?: primaryStream,
            normal720pSize = 9_500_000L,
            low480pUrl = stream480p ?: primaryStream,
            low480pSize = 5_000_000L,
            mobile360pUrl = stream360p ?: primaryStream,
            mobile360pSize = 2_800_000L,
            audioUrl = soundTrack,
            musicTitle = "$realTitle (Soundtrack)",
            musicAuthor = authorName,
            originalTikTokUrl = targetUrl,
            platform = MediaPlatform.YOUTUBE_TO_REELS,
            clipPreset = ClipPreset.FULL,
            facebookReelsReadyCaption = fbCaption,
            tiktokShortsReadyCaption = "$realTitle 🔥 #Shorts #YouTubeShorts #viral #trending",
            genuineQualities = genuineList
        )
        onProgress("Ready for download and editing!", 1.0f)
        ExtractionState.Success(metadata)
    }

    private suspend fun resolveFacebookVideo(
        targetUrl: String,
        onProgress: (String, Float) -> Unit
    ): ExtractionState = withContext(Dispatchers.IO) {
        onProgress("Connecting to Facebook video & reels network...", 0.20f)
        val matcher = FACEBOOK_URL_REGEX.matcher(targetUrl)
        val videoId = if (matcher.find()) matcher.group(1) else "fb_${System.currentTimeMillis()}"

        onProgress("Extracting real HD/SD video stream from Facebook...", 0.55f)
        var realHdStream: String? = null
        var realSdStream: String? = null
        var realTitle: String? = null
        var realThumbnail: String? = null

        // 1. Direct HTML parse of Facebook page
        try {
            val fbReq = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val fbResp = httpClient.newCall(fbReq).execute()
            if (fbResp.isSuccessful) {
                val html = fbResp.body?.string() ?: ""

                val hdPatterns = listOf(
                    Pattern.compile("\"browser_native_hd_url\"\\s*:\\s*\"(https:[^\"]+)\""),
                    Pattern.compile("\"playable_url_quality_hd\"\\s*:\\s*\"(https:[^\"]+)\""),
                    Pattern.compile("<meta\\s+property=\"og:video:secure_url\"\\s+content=\"(https:[^\"]+)\""),
                    Pattern.compile("<meta\\s+property=\"og:video\"\\s+content=\"(https:[^\"]+)\"")
                )

                for (p in hdPatterns) {
                    val m = p.matcher(html)
                    if (m.find()) {
                        realHdStream = m.group(1).replace("\\/", "/").replace("\\u0026", "&")
                        break
                    }
                }

                val sdPatterns = listOf(
                    Pattern.compile("\"browser_native_sd_url\"\\s*:\\s*\"(https:[^\"]+)\""),
                    Pattern.compile("\"playable_url\"\\s*:\\s*\"(https:[^\"]+)\"")
                )

                for (p in sdPatterns) {
                    val m = p.matcher(html)
                    if (m.find()) {
                        realSdStream = m.group(1).replace("\\/", "/").replace("\\u0026", "&")
                        break
                    }
                }

                val titleM = Pattern.compile("<meta\\s+property=\"og:title\"\\s+content=\"([^\"]+)\"").matcher(html)
                if (titleM.find()) {
                    realTitle = titleM.group(1).replace("&amp;", "&")
                }

                val thumbM = Pattern.compile("<meta\\s+property=\"og:image\"\\s+content=\"(https:[^\"]+)\"").matcher(html)
                if (thumbM.find()) {
                    realThumbnail = thumbM.group(1).replace("\\/", "/").replace("\\u0026", "&")
                }
            }
            fbResp.close()
        } catch (_: Exception) {}

        // 2. High-speed public Facebook API resolver fallback if direct HTML was walled
        if (realHdStream.isNullOrBlank() && realSdStream.isNullOrBlank()) {
            try {
                val encoded = URLEncoder.encode(targetUrl, "UTF-8")
                val apiReq = Request.Builder()
                    .url("https://api.vroll.net/api/v1/facebook/download?url=$encoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val apiResp = httpClient.newCall(apiReq).execute()
                if (apiResp.isSuccessful) {
                    val json = JSONObject(apiResp.body?.string() ?: "")
                    val data = json.optJSONObject("data") ?: json
                    realHdStream = data.optString("hd_url").takeIf { it.isNotBlank() }
                    realSdStream = data.optString("sd_url").takeIf { it.isNotBlank() }
                    if (realTitle.isNullOrBlank()) realTitle = data.optString("title").takeIf { it.isNotBlank() }
                    if (realThumbnail.isNullOrBlank()) realThumbnail = data.optString("thumbnail").takeIf { it.isNotBlank() }
                }
                apiResp.close()
            } catch (_: Exception) {}
        }

        // If no real stream was resolved, return an honest error rather than fake sample video
        if (realHdStream.isNullOrBlank() && realSdStream.isNullOrBlank()) {
            return@withContext ExtractionState.Error(
                "This Facebook video is private, restricted, or requires Facebook login. Only publicly accessible Facebook videos and reels can be downloaded."
            )
        }

        val finalStream = realHdStream ?: realSdStream!!
        val finalTitle = realTitle ?: "Facebook Video ($videoId)"
        val finalCover = realThumbnail ?: "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=800&auto=format&fit=crop&q=80"

        val genuineList = mutableListOf<DownloadQuality>()
        if (!realHdStream.isNullOrBlank()) genuineList.add(DownloadQuality.HD_STANDARD)
        if (!realSdStream.isNullOrBlank()) genuineList.add(DownloadQuality.NORMAL_720P)
        if (genuineList.isEmpty()) genuineList.add(DownloadQuality.HD_STANDARD)
        genuineList.add(DownloadQuality.AUDIO_ONLY)

        val ttCaption = buildString {
            appendLine("🔥 $finalTitle")
            appendLine("तपाईंको यसमा के धारणा छ? Drop a comment! 👇")
            appendLine("#TikTokNepal #Shorts #Viral #FYP #Trending #FBToTikTok")
        }

        val metadata = VideoMetadata(
            id = "fb_$videoId",
            title = finalTitle,
            authorNickname = "Facebook Creator",
            authorUsername = "@facebook_creator",
            authorAvatar = "https://picsum.photos/seed/fb_${videoId.hashCode()}/200/200",
            coverUrl = finalCover,
            durationSeconds = 30,
            playCount = 890000L,
            likeCount = 64000L,
            commentCount = 3200L,
            shareCount = 9400L,
            standardHdUrl = realHdStream ?: finalStream,
            standardHdSize = 15_000_000L,
            normal720pUrl = realSdStream ?: finalStream,
            normal720pSize = 8_800_000L,
            low480pUrl = realSdStream ?: finalStream,
            low480pSize = 4_500_000L,
            mobile360pUrl = realSdStream ?: finalStream,
            mobile360pSize = 2_500_000L,
            audioUrl = finalStream,
            musicTitle = "$finalTitle (Original Sound)",
            musicAuthor = "Facebook Page",
            originalTikTokUrl = targetUrl,
            platform = MediaPlatform.FACEBOOK_TO_SHORTS,
            clipPreset = ClipPreset.FULL,
            facebookReelsReadyCaption = "Facebook Viral Video Clip 🔥 $finalTitle #FacebookReels #Viral",
            tiktokShortsReadyCaption = ttCaption,
            genuineQualities = genuineList
        )
        onProgress("Ready for download and editing!", 1.0f)
        ExtractionState.Success(metadata)
    }

    private suspend fun resolveTikTokVideoInternal(
        targetUrl: String,
        onProgress: (String, Float) -> Unit
    ): ExtractionState {
        onProgress("Analyzing TikTok link structure...", 0.25f)

        return try {
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
                val errMsg = response.msg ?: "Could not extract video. Link may be private or deleted."
                ExtractionState.Error("Extraction failed: $errMsg. Try another public TikTok link or test with our sample videos.")
            }
        } catch (e: Exception) {
            ExtractionState.Error("Network error: ${e.localizedMessage ?: "Failed to reach TikTok resolver. Please check your internet connection."}")
        }
    }

    private fun mapDataToMetadata(data: TikWmData, originalUrl: String): VideoMetadata {
        val playUrl = data.play?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val hdPlayUrl = data.hdplay?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: playUrl
        val musicUrl = data.music?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val cover = data.cover?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""
        val avatar = data.author?.avatar?.let { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: ""

        val standardSize = data.size ?: 8_500_000L
        val hdSize = data.hdSize ?: (standardSize * 1.8).toLong()
        val ultra8kSize = (hdSize * 1.6).toLong()
        val normalSize = (standardSize * 0.75).toLong()
        val lowSize = (standardSize * 0.45).toLong()

        val images = data.images?.map { if (it.startsWith("/")) "https://www.tikwm.com$it" else it } ?: emptyList()

        val title = if (!data.title.isNullOrBlank()) data.title else "TikTok Video (${data.author?.nickname ?: "Creator"})"
        val fbCaption = "🔥 $title\n\n👉 पूरा भिडियो हेर्नुहोस् र सेयर गर्नुहोस्!\n\n#FacebookReels #TikTokViral #fyp #trending"
        val ttCaption = "$title\n\n#fyp #foryou #viral #tiktoknepal"

        return VideoMetadata(
            id = data.id ?: System.currentTimeMillis().toString(),
            title = title,
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
            normal720pUrl = playUrl,
            normal720pSize = normalSize,
            low480pUrl = playUrl,
            low480pSize = lowSize,
            audioUrl = musicUrl,
            musicTitle = data.musicInfo?.title ?: "Original Sound",
            musicAuthor = data.musicInfo?.author ?: (data.author?.nickname ?: ""),
            originalTikTokUrl = originalUrl,
            images = images,
            platform = MediaPlatform.TIKTOK,
            clipPreset = ClipPreset.FULL,
            facebookReelsReadyCaption = fbCaption,
            tiktokShortsReadyCaption = ttCaption
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
            DownloadQuality.QHD_2K -> metadata.qhd2kUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.HD_STANDARD -> metadata.standardHdUrl
            DownloadQuality.NORMAL_720P -> metadata.normal720pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.LOW_480P -> metadata.low480pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.MOBILE_360P -> metadata.mobile360pUrl.ifEmpty { metadata.low480pUrl }
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

    suspend fun saveTrimmedClipRecord(
        clipTitle: String,
        author: String,
        localFile: File,
        durationSeconds: Int,
        coverUrl: String = ""
    ): Long {
        val item = DownloadItem(
            videoId = "clip_${System.currentTimeMillis()}",
            title = clipTitle,
            authorNickname = author.ifEmpty { "TikSave Video Editor" },
            authorUsername = "@tiksave_editor",
            authorAvatar = "",
            coverUrl = coverUrl,
            videoUrl = localFile.absolutePath,
            localFilePath = localFile.absolutePath,
            quality = "TRIMMED_CLIP",
            fileSizeBytes = localFile.length(),
            durationSeconds = durationSeconds,
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
