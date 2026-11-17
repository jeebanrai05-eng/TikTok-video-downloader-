package com.example.data.repository

import android.content.Context
import com.example.data.api.TikTokApiService
import com.example.data.api.TikTokNetworkClient
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadDao
import com.example.data.db.DownloadItem
import com.example.data.download.DownloadManagerService
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.TikWmData
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
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

    private data class OEmbedInfo(
        val title: String,
        val authorName: String,
        val authorUniqueId: String,
        val thumbnailUrl: String
    )

    companion object {
        // Matches all TikTok domains & share paths: www, m, vt, vm, t, lite, @/video/, /photo/, /t/, with or without https://
        private val TIKTOK_URL_REGEX = Pattern.compile(
            "(?:https?://)?(?:[a-zA-Z0-9-]+\\.)*tiktok\\.com/[^\\s\"'<>()\\[\\]]+",
            Pattern.CASE_INSENSITIVE
        )

        private val UNIVERSAL_DATA_PATTERN = Pattern.compile(
            "<script[^>]*id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>(.*?)</script>",
            Pattern.DOTALL
        )

        private val SIGI_STATE_PATTERN = Pattern.compile(
            "<script[^>]*id=\"SIGI_STATE\"[^>]*>(.*?)</script>",
            Pattern.DOTALL
        )

        private val ANCHOR_HREF_PATTERN = Pattern.compile(
            "<a\\b[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        private val IMG_SRC_PATTERN = Pattern.compile(
            "<img\\b[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        private val H3_PATTERN = Pattern.compile(
            "<h3[^>]*>(.*?)</h3>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
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
            if (trimmed.isEmpty()) return null
            val matcher = TIKTOK_URL_REGEX.matcher(trimmed)
            if (matcher.find()) {
                var matched = matcher.group(0) ?: return null
                matched = matched.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '"', '\'')
                if (!matched.startsWith("http://", ignoreCase = true) &&
                    !matched.startsWith("https://", ignoreCase = true)
                ) {
                    matched = "https://$matched"
                }
                return matched
            }
            return null
        }

        fun isValidTikTokUrl(input: String): Boolean {
            return extractTikTokUrl(input) != null
        }

        private fun decodeHtmlEntities(input: String): String {
            var s = input
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
            // Decode hex numeric entities like &#x1F95A;
            val hexRegex = Regex("&#x([0-9a-fA-F]+);")
            s = hexRegex.replace(s) { m ->
                val cp = m.groupValues[1].toIntOrNull(16)
                if (cp != null) String(Character.toChars(cp)) else m.value
            }
            val decRegex = Regex("&#([0-9]+);")
            s = decRegex.replace(s) { m ->
                val cp = m.groupValues[1].toIntOrNull()
                if (cp != null) String(Character.toChars(cp)) else m.value
            }
            return s.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        }

        /**
         * Parses HTML fragments returned by ajaxSearch TikTok endpoints (TikVideo / SaveTik)
         * into clean VideoMetadata for both standard videos and Photo Carousel posts.
         */
        fun parseAjaxSearchHtml(html: String, originalUrl: String): VideoMetadata? {
            if (html.isBlank()) return null

            var hdVideoUrl = ""
            var mp4Url1 = ""
            var mp4Url2 = ""
            var audioUrl = ""
            val photoSlides = mutableListOf<String>()

            val anchorMatcher = ANCHOR_HREF_PATTERN.matcher(html)
            while (anchorMatcher.find()) {
                val rawHref = anchorMatcher.group(1)?.let { decodeHtmlEntities(it) }.orEmpty()
                val rawLabel = anchorMatcher.group(2)?.let { decodeHtmlEntities(it) }.orEmpty()
                val lowerLabel = rawLabel.lowercase()

                if (!rawHref.startsWith("http")) continue
                // Skip promotional / external profile links
                if (rawHref.contains("tiktokio.") || rawHref.contains("play.google.com")) continue

                when {
                    lowerLabel.contains("mp3") || lowerLabel.contains("audio") -> {
                        if (audioUrl.isBlank()) audioUrl = rawHref
                    }
                    lowerLabel.contains("photo") || lowerLabel.contains("image") || lowerLabel.contains("avatar") -> {
                        if (!rawHref.contains("tiktokio") && photoSlides.none { it == rawHref }) {
                            photoSlides.add(rawHref)
                        }
                    }
                    lowerLabel.contains("mp4 hd") || lowerLabel.contains("hd [") || lowerLabel.contains("without watermark (hd)") -> {
                        if (hdVideoUrl.isBlank()) hdVideoUrl = rawHref
                    }
                    lowerLabel.contains("mp4") || lowerLabel.contains("download [") || rawHref.contains("snapcdn.app") -> {
                        if (mp4Url1.isBlank()) mp4Url1 = rawHref
                        else if (mp4Url2.isBlank()) mp4Url2 = rawHref
                    }
                }
            }

            val imgs = mutableListOf<String>()
            val imgMatcher = IMG_SRC_PATTERN.matcher(html)
            while (imgMatcher.find()) {
                val src = imgMatcher.group(1)?.let { decodeHtmlEntities(it) }.orEmpty()
                if (src.startsWith("http") && !src.contains("ads")) {
                    imgs.add(src)
                }
            }

            val h3Matcher = H3_PATTERN.matcher(html)
            val title = if (h3Matcher.find()) {
                decodeHtmlEntities(h3Matcher.group(1).orEmpty()).ifBlank { "TikTok Video" }
            } else {
                "TikTok Video"
            }

            val bestVideo = hdVideoUrl.ifBlank { mp4Url1 }.ifBlank { mp4Url2 }
            val standardVideo = mp4Url1.ifBlank { mp4Url2 }.ifBlank { bestVideo }
            val fastVideo = mp4Url2.ifBlank { standardVideo }

            if (bestVideo.isBlank() && photoSlides.isEmpty()) {
                return null
            }

            val cover = imgs.firstOrNull().orEmpty().ifBlank { photoSlides.firstOrNull().orEmpty() }
            val finalSlides = if (photoSlides.isNotEmpty()) photoSlides else emptyList()

            val idMatch = Regex("([0-9]{15,22})").find(originalUrl + " " + bestVideo)
            val videoId = idMatch?.groupValues?.getOrNull(1) ?: System.currentTimeMillis().toString()

            return VideoMetadata(
                id = videoId,
                title = title,
                authorNickname = "TikTok Creator",
                authorUsername = "@tiktok",
                authorAvatar = "",
                coverUrl = cover,
                durationSeconds = 20,
                playCount = 0L,
                likeCount = 0L,
                commentCount = 0L,
                shareCount = 0L,
                ultra8kUrl = bestVideo,
                ultra8kSize = 20_400_000L,
                ultraHdrUrl = bestVideo,
                ultraHdrSize = 16_800_000L,
                standardHdUrl = standardVideo,
                standardHdSize = 10_500_000L,
                normal720pUrl = fastVideo,
                normal720pSize = 7_400_000L,
                low480pUrl = fastVideo,
                low480pSize = 4_800_000L,
                audioUrl = audioUrl.ifBlank { bestVideo },
                musicTitle = "Original Sound",
                musicAuthor = "TikTok",
                originalTikTokUrl = originalUrl,
                images = finalSlides
            )
        }

        /**
         * Parses TikTok's __UNIVERSAL_DATA_FOR_REHYDRATION__ (or SIGI_STATE) JSON directly from page HTML.
         * Extracts watermark-free playAddr/bitrateInfo streams and Photo Carousel imagePost slides.
         */
        fun parseUniversalDataFromHtml(html: String, canonicalUrl: String): VideoMetadata? {
            try {
                val universalMatcher = UNIVERSAL_DATA_PATTERN.matcher(html)
                if (universalMatcher.find()) {
                    val jsonRaw = universalMatcher.group(1)?.trim() ?: return null
                    val root = JSONObject(jsonRaw)
                    val scope = root.optJSONObject("__DEFAULT_SCOPE__") ?: return null
                    val detailObj = scope.optJSONObject("webapp.video-detail")
                        ?: scope.optJSONObject("webapp.reflow.video.detail")
                        ?: return null
                    val itemStruct = detailObj.optJSONObject("itemInfo")?.optJSONObject("itemStruct")
                        ?: return null
                    return parseItemStruct(itemStruct, canonicalUrl)
                }

                val sigiMatcher = SIGI_STATE_PATTERN.matcher(html)
                if (sigiMatcher.find()) {
                    val jsonRaw = sigiMatcher.group(1)?.trim() ?: return null
                    val root = JSONObject(jsonRaw)
                    val itemModule = root.optJSONObject("ItemModule") ?: return null
                    val keys = itemModule.keys()
                    if (keys.hasNext()) {
                        val firstKey = keys.next()
                        val itemStruct = itemModule.optJSONObject(firstKey) ?: return null
                        return parseItemStruct(itemStruct, canonicalUrl)
                    }
                }
            } catch (_: Exception) {
            }
            return null
        }

        private fun parseItemStruct(item: JSONObject, canonicalUrl: String): VideoMetadata? {
            val id = item.optString("id").ifBlank { System.currentTimeMillis().toString() }
            val authorObj = item.optJSONObject("author")
            val uniqueId = authorObj?.optString("uniqueId")?.ifBlank { "user" } ?: "user"
            val nickname = authorObj?.optString("nickname")?.ifBlank { "TikTok Creator" } ?: "TikTok Creator"
            val avatar = authorObj?.optString("avatarLarger")
                ?.ifBlank { authorObj.optString("avatarMedium") }
                ?.ifBlank { authorObj.optString("avatarThumb") }
                .orEmpty()

            val desc = item.optString("desc").ifBlank { "TikTok Video by $nickname" }
            val stats = item.optJSONObject("stats")
            val playCount = stats?.optLong("playCount", 0L) ?: 0L
            val likeCount = stats?.optLong("diggCount", 0L) ?: 0L
            val commentCount = stats?.optLong("commentCount", 0L) ?: 0L
            val shareCount = stats?.optLong("shareCount", 0L) ?: 0L

            val musicObj = item.optJSONObject("music")
            val musicTitle = musicObj?.optString("title")?.ifBlank { "Original Sound" } ?: "Original Sound"
            val musicAuthor = musicObj?.optString("authorName")?.ifBlank { nickname } ?: nickname
            val audioUrl = musicObj?.optString("playUrl").orEmpty()

            val imageList = mutableListOf<String>()
            val imagePost = item.optJSONObject("imagePost")
            val imagesArr = imagePost?.optJSONArray("images")
            if (imagesArr != null) {
                for (i in 0 until imagesArr.length()) {
                    val imgEntry = imagesArr.optJSONObject(i) ?: continue
                    val urlList = imgEntry.optJSONObject("imageURL")?.optJSONArray("urlList")
                    if (urlList != null && urlList.length() > 0) {
                        val imgUrl = urlList.optString(0)
                        if (imgUrl.startsWith("http")) {
                            imageList.add(imgUrl)
                        }
                    }
                }
            }

            val videoObj = item.optJSONObject("video")
            val duration = videoObj?.optInt("duration", 15) ?: 15
            val coverUrl = videoObj?.optString("originCover")
                ?.ifBlank { videoObj.optString("cover") }
                ?.ifBlank { imageList.firstOrNull().orEmpty() }
                .orEmpty()

            data class StreamCandidate(val bitrate: Long, val url: String, val size: Long)
            val candidates = mutableListOf<StreamCandidate>()

            val bitrateInfo = videoObj?.optJSONArray("bitrateInfo")
            if (bitrateInfo != null) {
                for (i in 0 until bitrateInfo.length()) {
                    val entry = bitrateInfo.optJSONObject(i) ?: continue
                    val bitrate = entry.optLong("Bitrate", 0L)
                    val playAddrObj = entry.optJSONObject("PlayAddr") ?: continue
                    val dataSize = playAddrObj.optString("DataSize").toLongOrNull()
                        ?: playAddrObj.optLong("DataSize", 0L)
                    val urlList = playAddrObj.optJSONArray("UrlList") ?: continue
                    var chosenUrl = ""
                    for (j in 0 until urlList.length()) {
                        val candidateUrl = urlList.optString(j)
                        if (candidateUrl.startsWith("http")) {
                            chosenUrl = candidateUrl
                            break
                        }
                    }
                    if (chosenUrl.isNotBlank()) {
                        candidates.add(StreamCandidate(bitrate, chosenUrl, dataSize))
                    }
                }
            }

            val sortedCandidates = candidates.sortedByDescending { it.bitrate }
            val fallbackPlayAddr = videoObj?.optString("playAddr").orEmpty()
                .ifBlank { videoObj?.optString("PlayAddrStruct")?.orEmpty() ?: "" }

            val bestCandidate = sortedCandidates.firstOrNull()
            val midCandidate = sortedCandidates.getOrNull(sortedCandidates.size / 2) ?: bestCandidate
            val lowCandidate = sortedCandidates.lastOrNull() ?: bestCandidate

            val bestUrl = bestCandidate?.url?.ifBlank { fallbackPlayAddr } ?: fallbackPlayAddr
            val midUrl = midCandidate?.url?.ifBlank { bestUrl } ?: bestUrl
            val lowUrl = lowCandidate?.url?.ifBlank { midUrl } ?: midUrl

            if (bestUrl.isBlank() && imageList.isEmpty()) {
                return null
            }

            val bestSize = (bestCandidate?.size ?: 0L).takeIf { it > 0L } ?: 14_500_000L
            val midSize = (midCandidate?.size ?: 0L).takeIf { it > 0L } ?: (bestSize * 0.65).toLong()
            val lowSize = (lowCandidate?.size ?: 0L).takeIf { it > 0L } ?: (bestSize * 0.40).toLong()

            return VideoMetadata(
                id = id,
                title = desc,
                authorNickname = nickname,
                authorUsername = "@$uniqueId",
                authorAvatar = avatar,
                coverUrl = coverUrl,
                durationSeconds = duration,
                playCount = playCount,
                likeCount = likeCount,
                commentCount = commentCount,
                shareCount = shareCount,
                ultra8kUrl = bestUrl,
                ultra8kSize = bestSize,
                ultraHdrUrl = bestUrl,
                ultraHdrSize = bestSize,
                standardHdUrl = midUrl,
                standardHdSize = midSize,
                normal720pUrl = midUrl,
                normal720pSize = midSize,
                low480pUrl = lowUrl,
                low480pSize = lowSize,
                audioUrl = audioUrl.ifBlank { bestUrl },
                musicTitle = musicTitle,
                musicAuthor = musicAuthor,
                originalTikTokUrl = canonicalUrl,
                images = imageList
            )
        }
    }

    fun extractTikTokUrl(input: String): String? = Companion.extractTikTokUrl(input)

    private sealed class ResolverOutcome {
        data class Success(val source: String, val priority: Int, val metadata: VideoMetadata) : ResolverOutcome()
        data class Failure(val source: String, val error: Throwable) : ResolverOutcome()
    }

    private fun extractViaAjaxSearch(endpointUrl: String, originUrl: String, targetUrl: String): VideoMetadata {
        val body = FormBody.Builder()
            .add("q", targetUrl)
            .add("lang", "en")
            .build()

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body)
            .header("User-Agent", TikTokNetworkClient.DESKTOP_USER_AGENT)
            .header("Origin", originUrl)
            .header("Referer", "$originUrl/")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("X-Requested-With", "XMLHttpRequest")
            .build()

        TikTokNetworkClient.fastApiClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Resolver HTTP ${response.code}")
            }
            val raw = response.body?.string().orEmpty()
            val json = JSONObject(raw)
            val status = json.optString("status")
            if (!status.equals("ok", ignoreCase = true)) {
                val msg = json.optString("mess").ifBlank { json.optString("msg") }
                throw IllegalStateException(msg.ifBlank { "Stream unavailable on mirror" })
            }
            val html = json.optString("data")
            return parseAjaxSearchHtml(html, targetUrl)
                ?: throw IllegalStateException("Could not parse stream links from resolver")
        }
    }

    private fun fetchOEmbedInfo(targetUrl: String): OEmbedInfo? {
        return try {
            val encoded = URLEncoder.encode(targetUrl, "UTF-8")
            val request = Request.Builder()
                .url("https://www.tiktok.com/oembed?url=$encoded")
                .header("User-Agent", TikTokNetworkClient.DESKTOP_USER_AGENT)
                .build()
            TikTokNetworkClient.fastApiClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string().orEmpty())
                val title = json.optString("title")
                val authorName = json.optString("author_name")
                val authorUrl = json.optString("author_url")
                val uniqueId = authorUrl.substringAfterLast("/@", "").substringBefore("?").ifBlank { "tiktok" }
                val thumb = json.optString("thumbnail_url")
                OEmbedInfo(
                    title = title,
                    authorName = authorName,
                    authorUniqueId = uniqueId,
                    thumbnailUrl = thumb
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDirectlyFromTikTokWeb(targetUrl: String): VideoMetadata {
        // Try Mobile UA first (lighter payload, cleaner reflow JSON on mobile links), then Desktop UA
        val userAgents = listOf(TikTokNetworkClient.MOBILE_USER_AGENT, TikTokNetworkClient.DESKTOP_USER_AGENT)
        var lastError: Exception? = null

        for (ua in userAgents) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", ua)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Referer", "https://www.tiktok.com/")
                    .build()

                TikTokNetworkClient.fastApiClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val finalUrl = response.request.url.toString()
                        val html = response.body?.string().orEmpty()
                        val parsed = parseUniversalDataFromHtml(html, finalUrl)
                        if (parsed != null) return parsed
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
            }
        }
        throw (lastError ?: IllegalStateException("Direct TikTok web payload unavailable"))
    }

    private suspend fun extractFromTikWm(targetUrl: String, hdParam: Int): VideoMetadata {
        val response = try {
            apiService.extractWithGet(targetUrl, hd = hdParam)
        } catch (getErr: Exception) {
            if (getErr is CancellationException) throw getErr
            if (getErr is SocketTimeoutException) throw getErr
            apiService.extractWithPost(targetUrl, hd = hdParam)
        }

        if (response.code == 0 && response.data != null) {
            return mapDataToMetadata(response.data, targetUrl)
        }
        val errMsg = response.msg?.takeIf { it.isNotBlank() }
            ?: "Could not extract video stream."
        throw IllegalStateException(errMsg)
    }

    suspend fun resolveTikTokVideo(
        rawInput: String,
        hdPriority: Boolean = true,
        onProgress: (String, Float) -> Unit
    ): ExtractionState = withContext(Dispatchers.IO) {
        val targetUrl = extractTikTokUrl(rawInput)
            ?: return@withContext ExtractionState.Error(
                "Please paste a valid TikTok link (e.g. tiktok.com/@user/video/..., vt.tiktok.com/..., or lite.tiktok.com/...)"
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

        val hdParam = if (hdPriority) 1 else 0
        onProgress("Searching TikTok fast CDN mirrors...", 0.30f)

        try {
            val winnerMeta: VideoMetadata = coroutineScope {
                val outcomes = Channel<ResolverOutcome>(capacity = 4)
                val oembedDeferred = async { fetchOEmbedInfo(targetUrl) }

                val tikVideoJob = async {
                    try {
                        val meta = extractViaAjaxSearch(
                            endpointUrl = "https://tikvideo.app/api/ajaxSearch",
                            originUrl = "https://tikvideo.app",
                            targetUrl = targetUrl
                        )
                        outcomes.send(ResolverOutcome.Success("tikvideo", 2, meta))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        outcomes.send(ResolverOutcome.Failure("tikvideo", e))
                    }
                }

                val saveTikJob = async {
                    try {
                        val meta = extractViaAjaxSearch(
                            endpointUrl = "https://savetik.co/api/ajaxSearch",
                            originUrl = "https://savetik.co",
                            targetUrl = targetUrl
                        )
                        outcomes.send(ResolverOutcome.Success("savetik", 2, meta))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        outcomes.send(ResolverOutcome.Failure("savetik", e))
                    }
                }

                val directJob = async {
                    try {
                        val meta = extractDirectlyFromTikTokWeb(targetUrl)
                        outcomes.send(ResolverOutcome.Success("direct", 3, meta))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        outcomes.send(ResolverOutcome.Failure("direct", e))
                    }
                }

                val tikWmJob = async {
                    try {
                        val meta = extractFromTikWm(targetUrl, hdParam)
                        outcomes.send(ResolverOutcome.Success("tikwm", 1, meta))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        outcomes.send(ResolverOutcome.Failure("tikwm", e))
                    }
                }

                var bestSuccess: ResolverOutcome.Success? = null
                val errors = mutableListOf<Throwable>()
                var completedCount = 0
                val totalEngines = 4

                while (completedCount < totalEngines) {
                    val outcome = outcomes.receive()
                    completedCount++
                    when (outcome) {
                        is ResolverOutcome.Success -> {
                            if (bestSuccess == null || outcome.priority < bestSuccess.priority) {
                                bestSuccess = outcome
                            }
                            if (outcome.priority == 1) {
                                // TikWM has full stats + hdplay: win immediately
                                break
                            } else {
                                onProgress("No-watermark stream found! Finalizing metadata...", 0.80f)
                                // Allow 250ms grace window in case TikWM/richer mirror finishes simultaneously
                                val extra = withTimeoutOrNull(250L) {
                                    if (completedCount < totalEngines) outcomes.receive() else null
                                }
                                if (extra != null) {
                                    completedCount++
                                    if (extra is ResolverOutcome.Success && extra.priority < bestSuccess.priority) {
                                        bestSuccess = extra
                                    }
                                }
                                break
                            }
                        }
                        is ResolverOutcome.Failure -> {
                            errors.add(outcome.error)
                        }
                    }
                }

                tikVideoJob.cancel()
                saveTikJob.cancel()
                directJob.cancel()
                tikWmJob.cancel()

                var finalMeta = bestSuccess?.metadata
                    ?: throw (errors.lastOrNull() ?: IllegalStateException("Extraction failed"))

                // Enrich author/cover/title from oEmbed (0.17s) if mirror omitted creator info
                if (finalMeta.authorNickname == "TikTok Creator" || finalMeta.coverUrl.isBlank()) {
                    val oembed = withTimeoutOrNull(700L) { oembedDeferred.await() }
                    if (oembed != null) {
                        finalMeta = finalMeta.copy(
                            title = if (finalMeta.title == "TikTok Video" && oembed.title.isNotBlank()) oembed.title else finalMeta.title,
                            authorNickname = oembed.authorName.ifBlank { finalMeta.authorNickname },
                            authorUsername = if (oembed.authorUniqueId.isNotBlank()) "@${oembed.authorUniqueId}" else finalMeta.authorUsername,
                            coverUrl = finalMeta.coverUrl.ifBlank { oembed.thumbnailUrl }
                        )
                    }
                } else {
                    oembedDeferred.cancel()
                }

                finalMeta
            }

            onProgress("Ready to download!", 1.0f)
            ExtractionState.Success(winnerMeta)
        } catch (e: Exception) {
            val friendly = when {
                e is UnknownHostException ->
                    "No internet connection or DNS unreachable. Please check your Wi-Fi or mobile data."
                e is SocketTimeoutException || (e.message?.contains("timeout", ignoreCase = true) == true) ->
                    "TikTok mirrors took too long to respond. Please tap Fetch Video to retry."
                else ->
                    "Could not extract this TikTok link (${e.localizedMessage ?: "video may be private or deleted"})."
            }
            ExtractionState.Error(friendly)
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
