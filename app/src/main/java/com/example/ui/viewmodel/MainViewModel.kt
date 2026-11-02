package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiContentTone
import com.example.data.ai.AiGenerationState
import com.example.data.ai.AiService
import com.example.data.ai.AiViralResult
import com.example.data.db.DownloadItem
import com.example.data.model.ActiveDownload
import com.example.data.model.ClipPreset
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.MediaPlatform
import com.example.data.model.VideoMetadata
import com.example.data.repository.DownloadRepository
import com.example.data.update.AppUpdateInfo
import com.example.data.update.UpdateManager
import com.example.ui.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

enum class AppTab {
    DOWNLOADER,
    AI_STUDIO,
    LIBRARY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)
    val updateManager = UpdateManager(application)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(MediaPlatform.TIKTOK)
    val selectedPlatform: StateFlow<MediaPlatform> = _selectedPlatform.asStateFlow()

    private val _selectedClipPreset = MutableStateFlow(ClipPreset.SEC_30)
    val selectedClipPreset: StateFlow<ClipPreset> = _selectedClipPreset.asStateFlow()

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    private val _activeDownload = MutableStateFlow<ActiveDownload?>(null)
    val activeDownload: StateFlow<ActiveDownload?> = _activeDownload.asStateFlow()

    private val _isDownloadModalVisible = MutableStateFlow(false)
    val isDownloadModalVisible: StateFlow<Boolean> = _isDownloadModalVisible.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _selectedQuality = MutableStateFlow(DownloadQuality.ULTRA_8K_120FPS)
    val selectedQuality: StateFlow<DownloadQuality> = _selectedQuality.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.DOWNLOADER)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _previewingMedia = MutableStateFlow<Pair<String, String>?>(null) // (urlOrPath, title)
    val previewingMedia: StateFlow<Pair<String, String>?> = _previewingMedia.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _isInstallingUpdate = MutableStateFlow(false)
    val isInstallingUpdate: StateFlow<Boolean> = _isInstallingUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow(0f)
    val updateDownloadProgress: StateFlow<Float> = _updateDownloadProgress.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    // Download history & favorites
    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDownloads: StateFlow<List<DownloadItem>> = repository.favoriteDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Creator Studio state
    private val aiService = AiService()

    private val _aiInputText = MutableStateFlow("")
    val aiInputText: StateFlow<String> = _aiInputText.asStateFlow()

    private val _aiSelectedTone = MutableStateFlow(AiContentTone.VIRAL_TREND)
    val aiSelectedTone: StateFlow<AiContentTone> = _aiSelectedTone.asStateFlow()

    private val _aiGenerationState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiGenerationState: StateFlow<AiGenerationState> = _aiGenerationState.asStateFlow()

    private var currentDownloadJob: Job? = null

    init {
        checkClipboardForTikTokLink()
        // Check for updates silently on startup
        checkForAppUpdates(silent = true)
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSelectedPlatform(platform: MediaPlatform) {
        _selectedPlatform.value = platform
    }

    fun setSelectedClipPreset(preset: ClipPreset) {
        _selectedClipPreset.value = preset
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        // Auto-detect platform if input contains specific domains
        val detected = DownloadRepository.detectPlatform(newUrl)
        _selectedPlatform.value = detected

        if (_extractionState.value is ExtractionState.Error) {
            _extractionState.value = ExtractionState.Idle
        }
    }

    fun clearInput() {
        _urlInput.value = ""
        _extractionState.value = ExtractionState.Idle
    }

    fun checkClipboardForTikTokLink() {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString() ?: ""
                val extracted = DownloadRepository.extractAnyUrl(text)
                if (extracted != null && extracted != _urlInput.value) {
                    _clipboardDetectedUrl.value = extracted
                }
            }
        } catch (_: Exception) {
        }
    }

    fun useClipboardUrl() {
        _clipboardDetectedUrl.value?.let { link ->
            _urlInput.value = link
            _clipboardDetectedUrl.value = null
            startExtraction()
        }
    }

    fun dismissClipboardPrompt() {
        _clipboardDetectedUrl.value = null
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString() ?: ""
                val extracted = DownloadRepository.extractAnyUrl(text) ?: text.trim()
                _urlInput.value = extracted
                startExtraction()
            } else {
                viewModelScope.launch { _toastEvents.emit("Clipboard is empty") }
            }
        } catch (_: Exception) {
            viewModelScope.launch { _toastEvents.emit("Could not access clipboard") }
        }
    }

    fun selectSample(sample: DownloadRepository.SampleItem) {
        _selectedPlatform.value = sample.platform
        _urlInput.value = sample.url
        startExtraction()
    }

    fun setSelectedQuality(quality: DownloadQuality) {
        _selectedQuality.value = quality
    }

    fun startExtraction() {
        val input = _urlInput.value.trim()
        if (input.isBlank()) {
            viewModelScope.launch { _toastEvents.emit("Please paste a link first (TikTok, YouTube, or Facebook)") }
            return
        }

        val platform = DownloadRepository.detectPlatform(input)
        _selectedPlatform.value = platform

        viewModelScope.launch {
            _extractionState.value = ExtractionState.Analyzing("Connecting to ${platform.badge} stream engine...", 0.15f)
            val result = repository.resolveMediaLink(input, platform) { step, progress ->
                _extractionState.value = ExtractionState.Analyzing(step, progress)
            }
            _extractionState.value = result
            if (result is ExtractionState.Success) {
                val meta = result.metadata
                if (meta.isPhotoSlide) {
                    _selectedQuality.value = DownloadQuality.PHOTO_SLIDE_ULTRA
                    _toastEvents.emit("📸 Photo Carousel detected (${meta.images.size} photos in 8K HDR+ Ultra)!")
                } else {
                    _selectedQuality.value = DownloadQuality.ULTRA_8K_120FPS
                    when (meta.platform) {
                        MediaPlatform.YOUTUBE_TO_REELS -> _toastEvents.emit("▶️ YouTube video extracted! Ready to clip for Facebook Reels.")
                        MediaPlatform.FACEBOOK_TO_SHORTS -> _toastEvents.emit("👥 Facebook video extracted! Ready to clip for TikTok & Shorts.")
                        MediaPlatform.TIKTOK -> _toastEvents.emit("🎵 TikTok video extracted without watermark!")
                    }
                }
            }
        }
    }

    fun startDownload(metadata: VideoMetadata, quality: DownloadQuality) {
        // If it's a photo slide and quality is PHOTO_SLIDE_ULTRA with multiple images, download all photos
        if (metadata.isPhotoSlide && quality == DownloadQuality.PHOTO_SLIDE_ULTRA && metadata.images.size > 1) {
            downloadAllPhotoSlides(metadata)
            return
        }

        // Prevent concurrent duplicate download clicks
        if (_isDownloading.value) {
            viewModelScope.launch { _toastEvents.emit("Download is already running in background") }
            _isDownloadModalVisible.value = true
            return
        }

        currentDownloadJob?.cancel()
        _isDownloading.value = true

        val isAudio = quality == DownloadQuality.AUDIO_ONLY ||
                quality == DownloadQuality.AUDIO_HIGH_SOUND ||
                quality == DownloadQuality.AUDIO_VFX_SOUND

        val downloadUrl = when (quality) {
            DownloadQuality.ULTRA_8K_120FPS -> if (metadata.isPhotoSlide && metadata.ultra8kUrl.isBlank()) (metadata.images.firstOrNull() ?: metadata.coverUrl) else metadata.ultra8kUrl
            DownloadQuality.HDR_ULTRA -> if (metadata.isPhotoSlide && metadata.ultraHdrUrl.isBlank()) (metadata.images.firstOrNull() ?: metadata.coverUrl) else metadata.ultraHdrUrl
            DownloadQuality.HD_STANDARD -> if (metadata.isPhotoSlide && metadata.standardHdUrl.isBlank()) (metadata.images.firstOrNull() ?: metadata.coverUrl) else metadata.standardHdUrl
            DownloadQuality.NORMAL_720P -> metadata.normal720pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.LOW_480P -> metadata.low480pUrl.ifEmpty { metadata.standardHdUrl }
            DownloadQuality.AUDIO_ONLY, DownloadQuality.AUDIO_HIGH_SOUND, DownloadQuality.AUDIO_VFX_SOUND -> metadata.audioUrl
            DownloadQuality.COVER_IMAGE -> metadata.coverUrl
            DownloadQuality.PHOTO_SLIDE_ULTRA -> metadata.ultraHdrUrl.ifBlank { metadata.images.firstOrNull() ?: metadata.coverUrl }
        }

        if (downloadUrl.isBlank()) {
            _isDownloading.value = false
            viewModelScope.launch { _toastEvents.emit("Selected stream is unavailable for this video") }
            return
        }

        val ext = when {
            isAudio -> "mp3"
            quality == DownloadQuality.COVER_IMAGE || quality == DownloadQuality.PHOTO_SLIDE_ULTRA -> "jpg"
            else -> "mp4"
        }

        val audioProfileSuffix = when (quality) {
            DownloadQuality.AUDIO_HIGH_SOUND -> "_HighSound"
            DownloadQuality.AUDIO_VFX_SOUND -> "_VFXSound"
            DownloadQuality.AUDIO_ONLY -> "_Audio"
            else -> ""
        }

        val cleanTitle = metadata.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val filename = "TikSave_${cleanTitle}_${quality.name}${audioProfileSuffix}_${System.currentTimeMillis()}.$ext"

        _activeDownload.value = ActiveDownload(
            videoId = metadata.id,
            title = metadata.title,
            quality = quality,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = when (quality) {
                DownloadQuality.ULTRA_8K_120FPS -> metadata.ultra8kSize
                DownloadQuality.HDR_ULTRA -> metadata.ultraHdrSize
                DownloadQuality.HD_STANDARD -> metadata.standardHdSize
                DownloadQuality.NORMAL_720P -> metadata.normal720pSize
                DownloadQuality.LOW_480P -> metadata.low480pSize
                DownloadQuality.AUDIO_ONLY, DownloadQuality.AUDIO_HIGH_SOUND, DownloadQuality.AUDIO_VFX_SOUND -> 4_500_000L
                DownloadQuality.COVER_IMAGE, DownloadQuality.PHOTO_SLIDE_ULTRA -> 2_500_000L
            },
            speedBytesPerSec = 0L,
            etaSeconds = 0
        )
        _isDownloadModalVisible.value = true

        currentDownloadJob = viewModelScope.launch {
            try {
                val result = repository.downloadManager.download(
                    url = downloadUrl,
                    targetFilename = filename,
                    quality = quality
                ) { prog, downloaded, total, speed, eta ->
                    _activeDownload.value = _activeDownload.value?.copy(
                        progress = prog,
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        speedBytesPerSec = speed,
                        etaSeconds = eta
                    )
                }

                result.onSuccess { downloadResult ->
                    _activeDownload.value = _activeDownload.value?.copy(
                        progress = 1.0f,
                        isCompleted = true,
                        localFilePath = downloadResult.file.absolutePath,
                        speedBytesPerSec = 0L
                    )

                    repository.saveDownloadRecord(metadata, quality, downloadResult.file)

                    val destinationLabel = when {
                        isAudio -> "Music/TikSave (Music Player ready)"
                        quality == DownloadQuality.COVER_IMAGE || quality == DownloadQuality.PHOTO_SLIDE_ULTRA -> "Pictures/TikSave (Gallery ready)"
                        else -> "Movies/TikSave (Gallery ready)"
                    }
                    _toastEvents.emit("✅ Download complete! Saved to $destinationLabel")
                }.onFailure { err ->
                    _activeDownload.value = _activeDownload.value?.copy(
                        errorMessage = err.localizedMessage ?: "Download failed"
                    )
                    _toastEvents.emit("Download failed: ${err.localizedMessage}")
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun cancelActiveDownload() {
        currentDownloadJob?.cancel()
        _isDownloading.value = false
        _activeDownload.value = null
        _isDownloadModalVisible.value = false
        viewModelScope.launch {
            _toastEvents.emit("Download cancelled")
        }
    }

    fun hideDownloadModal() {
        _isDownloadModalVisible.value = false
    }

    fun dismissActiveDownload() {
        _activeDownload.value = null
        _isDownloadModalVisible.value = false
    }

    fun previewMedia(urlOrPath: String, title: String) {
        _previewingMedia.value = Pair(urlOrPath, title)
    }

    fun dismissPreview() {
        _previewingMedia.value = null
    }

    fun deleteItem(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item.id, item.localFilePath)
            _toastEvents.emit("Deleted from library")
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun toggleFavorite(item: DownloadItem) {
        toggleFavorite(item.id)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _toastEvents.emit("Download library cleared")
        }
    }

    // AI Studio methods
    fun setAiInputText(text: String) {
        _aiInputText.value = text
    }

    fun setAiSelectedTone(tone: AiContentTone) {
        _aiSelectedTone.value = tone
    }

    fun generateAiViralPack() {
        val input = _aiInputText.value.trim().ifEmpty {
            when (_aiSelectedTone.value) {
                AiContentTone.YOUTUBE_TO_REELS -> "YouTube Viral Podcast Clip to Facebook Reels"
                AiContentTone.FACEBOOK_TO_SHORTS -> "Facebook Trending Video to TikTok & Shorts"
                AiContentTone.NEPAL_POLITICS -> "Nepal Politics & Current Affairs Debate"
                AiContentTone.COMEDY_FUNNY -> "Funny Nepali Comedy Moments"
                AiContentTone.AESTHETIC_VIBE -> "Aesthetic Calm Nature & Travel"
                AiContentTone.MOTIVATION -> "Daily Nepali Success & Motivation"
                else -> "Viral TikTok Content"
            }
        }
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Analyzing content context & tone...", 0.2f)
            try {
                val result = aiService.generateViralPack(
                    input = input,
                    tone = _aiSelectedTone.value,
                    forceRandomSeed = Random.nextInt(1000, 999999),
                    onProgress = { step, prog ->
                        _aiGenerationState.value = AiGenerationState.Generating(step, prog)
                    }
                )
                _aiGenerationState.value = AiGenerationState.Success(result)
            } catch (e: Exception) {
                _aiGenerationState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to generate AI package")
            }
        }
    }

    fun regenerateAiViralPack() {
        val currentTitle = (_aiGenerationState.value as? AiGenerationState.Success)?.result?.title
            ?: _aiInputText.value.trim().ifEmpty { "Viral Social Media Content" }
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Randomizing unlimited fresh captions & hashtags...", 0.25f)
            try {
                val newSeed = Random.nextInt(1000, 999999)
                val result = aiService.generateViralPack(
                    input = currentTitle,
                    tone = _aiSelectedTone.value,
                    forceRandomSeed = newSeed,
                    onProgress = { step, prog ->
                        _aiGenerationState.value = AiGenerationState.Generating(step, prog)
                    }
                )
                _aiGenerationState.value = AiGenerationState.Success(result)
                _toastEvents.emit("🎲 Fresh Variation #${result.generationId} Generated!")
            } catch (e: Exception) {
                _aiGenerationState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to regenerate pack")
            }
        }
    }

    fun launchAiWithVideo(metadata: VideoMetadata) {
        _aiInputText.value = metadata.title
        _aiSelectedTone.value = when (metadata.platform) {
            MediaPlatform.YOUTUBE_TO_REELS -> AiContentTone.YOUTUBE_TO_REELS
            MediaPlatform.FACEBOOK_TO_SHORTS -> AiContentTone.FACEBOOK_TO_SHORTS
            MediaPlatform.TIKTOK -> AiContentTone.VIRAL_TREND
        }
        _currentTab.value = AppTab.AI_STUDIO
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Analyzing video & generating viral pack...", 0.25f)
            try {
                val result = aiService.generateViralPack(
                    input = metadata.title,
                    tone = _aiSelectedTone.value,
                    author = metadata.authorNickname,
                    isPhotoSlide = metadata.isPhotoSlide,
                    slideCount = metadata.images.size,
                    forceRandomSeed = Random.nextInt(1000, 999999),
                    onProgress = { step, prog ->
                        _aiGenerationState.value = AiGenerationState.Generating(step, prog)
                    }
                )
                _aiGenerationState.value = AiGenerationState.Success(result)
            } catch (e: Exception) {
                _aiGenerationState.value = AiGenerationState.Error(e.localizedMessage ?: "Failed to generate AI pack")
            }
        }
    }

    fun downloadAllPhotoSlides(metadata: VideoMetadata) {
        if (metadata.images.isEmpty()) {
            startDownload(metadata, DownloadQuality.PHOTO_SLIDE_ULTRA)
            return
        }

        if (_isDownloading.value) {
            viewModelScope.launch { _toastEvents.emit("Download is already running in background") }
            _isDownloadModalVisible.value = true
            return
        }

        currentDownloadJob?.cancel()
        _isDownloading.value = true
        _isDownloadModalVisible.value = true

        val totalImages = metadata.images.size
        _activeDownload.value = ActiveDownload(
            videoId = metadata.id,
            title = "${metadata.title} (All $totalImages Photos)",
            quality = DownloadQuality.PHOTO_SLIDE_ULTRA,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = (totalImages * 2_500_000L),
            speedBytesPerSec = 0L,
            etaSeconds = 0
        )

        currentDownloadJob = viewModelScope.launch {
            try {
                var successfulCount = 0
                val cleanTitle = metadata.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)

                for ((index, imgUrl) in metadata.images.withIndex()) {
                    val slideNumber = index + 1
                    val filename = "TikSave_${cleanTitle}_slide_${slideNumber}_${System.currentTimeMillis()}.jpg"

                    val slideMeta = metadata.copy(
                        id = "${metadata.id}_slide_$slideNumber",
                        title = "${metadata.title} - Slide #$slideNumber",
                        ultraHdrUrl = imgUrl,
                        standardHdUrl = imgUrl,
                        images = listOf(imgUrl)
                    )

                    val result = repository.downloadManager.download(
                        url = imgUrl,
                        targetFilename = filename,
                        quality = DownloadQuality.PHOTO_SLIDE_ULTRA
                    ) { prog, downloaded, _, speed, eta ->
                        val overallProgress = (index.toFloat() + prog) / totalImages.toFloat()
                        _activeDownload.value = _activeDownload.value?.copy(
                            progress = overallProgress.coerceIn(0f, 1f),
                            bytesDownloaded = (index * 2_500_000L) + downloaded,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    }

                    result.onSuccess { res ->
                        successfulCount++
                        repository.saveDownloadRecord(slideMeta, DownloadQuality.PHOTO_SLIDE_ULTRA, res.file)
                    }
                }

                _activeDownload.value = _activeDownload.value?.copy(
                    progress = 1.0f,
                    isCompleted = true,
                    speedBytesPerSec = 0L
                )
                _toastEvents.emit("✅ Successfully saved all $successfulCount photos to Gallery (Pictures/TikSave)!")
            } catch (e: Exception) {
                _toastEvents.emit("Download error: ${e.localizedMessage}")
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun downloadPhotoSlide(metadata: VideoMetadata, slideUrl: String, slideNumber: Int) {
        val slideMeta = metadata.copy(
            id = "${metadata.id}_slide_$slideNumber",
            title = "${metadata.title} - Slide #$slideNumber",
            ultraHdrUrl = slideUrl,
            standardHdUrl = slideUrl,
            images = listOf(slideUrl)
        )
        startDownload(slideMeta, DownloadQuality.PHOTO_SLIDE_ULTRA)
        viewModelScope.launch {
            _toastEvents.emit("📸 Saving Slide #$slideNumber in 8K HDR+ Ultra...")
        }
    }

    // App Self Update Logic
    fun checkForAppUpdates(silent: Boolean = false) {
        if (_isCheckingForUpdates.value || _isDownloadingUpdate.value) return

        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            try {
                val result = updateManager.checkForUpdates()
                result.onSuccess { update ->
                    if (update.isUpdateAvailable) {
                        _updateInfo.value = update
                    } else if (!silent) {
                        _toastEvents.emit("TikSave is up to date! (v1.1.0)")
                    }
                }.onFailure { e ->
                    if (!silent) {
                        _toastEvents.emit("Could not check for updates: ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _toastEvents.emit("Could not check for updates: ${e.localizedMessage}")
                }
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    fun installAppUpdate() {
        val apkUrl = _updateInfo.value?.apkDownloadUrl ?: return
        if (_isDownloadingUpdate.value) return

        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _updateDownloadProgress.value = 0f
            try {
                val result = updateManager.downloadAndInstallApk(apkUrl) { progress ->
                    _updateDownloadProgress.value = progress
                }
                _isDownloadingUpdate.value = false
                result.onSuccess {
                    _isInstallingUpdate.value = true
                    _toastEvents.emit("Launching package installer...")
                }.onFailure { err ->
                    _isInstallingUpdate.value = false
                    _toastEvents.emit("Update install failed: ${err.localizedMessage}")
                }
            } catch (e: Exception) {
                _isDownloadingUpdate.value = false
                _isInstallingUpdate.value = false
                _toastEvents.emit("Update install failed: ${e.localizedMessage}")
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
        _isCheckingForUpdates.value = false
        _isDownloadingUpdate.value = false
        _isInstallingUpdate.value = false
    }
}
