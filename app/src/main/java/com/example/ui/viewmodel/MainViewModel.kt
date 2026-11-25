package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ai.AiContentTone
import com.example.data.ai.AiGenerationState
import com.example.data.ai.AiService
import com.example.data.db.DownloadItem
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
import com.example.data.model.VideoMetadata
import com.example.data.repository.DownloadRepository
import com.example.data.update.AppUpdateInfo
import com.example.data.update.UpdateManager
import com.example.ui.util.FormatUtils
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

enum class AppTab {
    DOWNLOADER,
    AI_STUDIO,
    LIBRARY,
    SETTINGS
}

data class BatchDownloadState(
    val isRunning: Boolean = false,
    val totalCount: Int = 0,
    val currentIndex: Int = 0,
    val currentStatus: String = "",
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val isFinished: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else ((completedCount + failedCount).toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("tiksave_prefs", Context.MODE_PRIVATE)
    private val repository = DownloadRepository(application)
    val updateManager = UpdateManager(application)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

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

    private val _previewingMedia = MutableStateFlow<Pair<String, String>?>(null)
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

    // Persisted Preferences
    private val _ultra8k120FpsEnabled = MutableStateFlow(prefs.getBoolean("ultra_8k", true))
    val ultra8k120FpsEnabled: StateFlow<Boolean> = _ultra8k120FpsEnabled.asStateFlow()

    private val _hdrBoostEnabled = MutableStateFlow(prefs.getBoolean("hdr_boost", true))
    val hdrBoostEnabled: StateFlow<Boolean> = _hdrBoostEnabled.asStateFlow()

    private val _turboSpeedEnabled = MutableStateFlow(prefs.getBoolean("turbo_speed", true))
    val turboSpeedEnabled: StateFlow<Boolean> = _turboSpeedEnabled.asStateFlow()

    private val _autoClipboardEnabled = MutableStateFlow(prefs.getBoolean("auto_clipboard", true))
    val autoClipboardEnabled: StateFlow<Boolean> = _autoClipboardEnabled.asStateFlow()

    private val _wifiOnlyEnabled = MutableStateFlow(prefs.getBoolean("wifi_only", false))
    val wifiOnlyEnabled: StateFlow<Boolean> = _wifiOnlyEnabled.asStateFlow()

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
    private var batchDownloadJob: Job? = null

    // v1.1.9 New Feature: Multi-Link Batch Queue Saver + 48h (1-2 Day) Highlight Window
    private val _batchInputText = MutableStateFlow("")
    val batchInputText: StateFlow<String> = _batchInputText.asStateFlow()

    private val _batchSelectedQuality = MutableStateFlow(DownloadQuality.ULTRA_8K_120FPS)
    val batchSelectedQuality: StateFlow<DownloadQuality> = _batchSelectedQuality.asStateFlow()

    private val _batchState = MutableStateFlow<BatchDownloadState?>(null)
    val batchState: StateFlow<BatchDownloadState?> = _batchState.asStateFlow()

    private val _isBatchFeatureHighlighted = MutableStateFlow(true)
    val isBatchFeatureHighlighted: StateFlow<Boolean> = _isBatchFeatureHighlighted.asStateFlow()

    private val _highlightRemainingLabel = MutableStateFlow("NEW ✨ • 48H SPOTLIGHT")
    val highlightRemainingLabel: StateFlow<String> = _highlightRemainingLabel.asStateFlow()

    init {
        initFeatureHighlightWindow()
        checkClipboardForTikTokLink()
        checkForAppUpdates(silent = true)
    }

    private fun initFeatureHighlightWindow() {
        val key = "v119_batch_highlight_start_ms"
        val now = System.currentTimeMillis()
        var firstSeen = prefs.getLong(key, 0L)
        if (firstSeen == 0L) {
            firstSeen = now
            prefs.edit().putLong(key, firstSeen).apply()
        }
        val highlightDurationMs = 48L * 60L * 60L * 1000L // 48 hours (1.5 - 2 days spotlight)
        val remainingMs = (firstSeen + highlightDurationMs) - now
        if (remainingMs > 0L) {
            val hoursLeft = ((remainingMs / (60L * 60L * 1000L)) + 1L).coerceAtMost(48L)
            _isBatchFeatureHighlighted.value = true
            _highlightRemainingLabel.value = "NEW ✨ • ${hoursLeft}H SPOTLIGHT"
        } else {
            _isBatchFeatureHighlighted.value = false
            _highlightRemainingLabel.value = "v1.1.9 FEATURE"
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setUltra8k120FpsEnabled(enabled: Boolean) {
        _ultra8k120FpsEnabled.value = enabled
        prefs.edit().putBoolean("ultra_8k", enabled).apply()
    }

    fun setHdrBoostEnabled(enabled: Boolean) {
        _hdrBoostEnabled.value = enabled
        prefs.edit().putBoolean("hdr_boost", enabled).apply()
    }

    fun setTurboSpeedEnabled(enabled: Boolean) {
        _turboSpeedEnabled.value = enabled
        prefs.edit().putBoolean("turbo_speed", enabled).apply()
    }

    fun setAutoClipboardEnabled(enabled: Boolean) {
        _autoClipboardEnabled.value = enabled
        prefs.edit().putBoolean("auto_clipboard", enabled).apply()
        if (!enabled) _clipboardDetectedUrl.value = null
    }

    fun setWifiOnlyEnabled(enabled: Boolean) {
        _wifiOnlyEnabled.value = enabled
        prefs.edit().putBoolean("wifi_only", enabled).apply()
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        if (_extractionState.value is ExtractionState.Error) {
            _extractionState.value = ExtractionState.Idle
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvents.emit(message)
        }
    }

    fun clearInput() {
        _urlInput.value = ""
        _extractionState.value = ExtractionState.Idle
    }

    fun checkClipboardForTikTokLink() {
        if (!_autoClipboardEnabled.value) return
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString() ?: ""
                val extracted = DownloadRepository.extractTikTokUrl(text)
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
                val extracted = DownloadRepository.extractTikTokUrl(text) ?: text.trim()
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
        _urlInput.value = sample.url
        startExtraction()
    }

    fun setSelectedQuality(quality: DownloadQuality) {
        _selectedQuality.value = quality
    }

    fun startExtraction() {
        val input = _urlInput.value.trim()
        if (input.isBlank()) {
            viewModelScope.launch { _toastEvents.emit("Please paste a TikTok video or photo link first") }
            return
        }

        viewModelScope.launch {
            _extractionState.value = ExtractionState.Analyzing("Connecting to TikTok 8K CDN...", 0.15f)
            val result = repository.resolveTikTokVideo(
                rawInput = input,
                hdPriority = _hdrBoostEnabled.value
            ) { step, progress ->
                _extractionState.value = ExtractionState.Analyzing(step, progress)
            }
            _extractionState.value = result
            if (result is ExtractionState.Success) {
                val meta = result.metadata
                if (meta.isPhotoSlide) {
                    _selectedQuality.value = DownloadQuality.PHOTO_SLIDE_ULTRA
                    _toastEvents.emit("📸 Photo Carousel detected (${meta.images.size} photos in 8K HDR+)!")
                } else {
                    _selectedQuality.value = if (_ultra8k120FpsEnabled.value) {
                        DownloadQuality.ULTRA_8K_120FPS
                    } else {
                        DownloadQuality.HDR_ULTRA
                    }
                    _toastEvents.emit("🎵 TikTok video extracted without watermark!")
                }
            }
        }
    }

    fun startDownload(metadata: VideoMetadata, quality: DownloadQuality) {
        if (metadata.isPhotoSlide && quality == DownloadQuality.PHOTO_SLIDE_ULTRA && metadata.images.size > 1) {
            downloadAllPhotoSlides(metadata)
            return
        }

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
            DownloadQuality.PHOTO_SLIDE_ULTRA -> metadata.images.firstOrNull() ?: metadata.ultraHdrUrl.ifBlank { metadata.coverUrl }
        }

        if (downloadUrl.isBlank()) {
            _isDownloading.value = false
            viewModelScope.launch { _toastEvents.emit("Selected stream is unavailable for this post") }
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

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _toastEvents.emit("Download library cleared")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedBytes = withContext(Dispatchers.IO) {
                var total = 0L
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name != "TikSave-Update.apk") {
                        total += dirSize(file)
                        file.deleteRecursively()
                    }
                }
                total
            }
            val msg = if (freedBytes > 0) {
                "Cleaned ${FormatUtils.formatBytes(freedBytes)} of temporary cache"
            } else {
                "Stream cache is already clean"
            }
            _toastEvents.emit(msg)
        }
    }

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
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
                AiContentTone.NEPAL_POLITICS -> "Nepal Politics & Current Affairs"
                AiContentTone.COMEDY_FUNNY -> "Funny Nepali Comedy Moments"
                AiContentTone.AESTHETIC_VIBE -> "Aesthetic Calm Nature & Travel"
                AiContentTone.MOTIVATION -> "Daily Nepali Success & Motivation"
                AiContentTone.VIRAL_TREND -> "Viral TikTok FYP Content"
            }
        }
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Analyzing TikTok content context & tone...", 0.2f)
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
            ?: _aiInputText.value.trim().ifEmpty { "Viral TikTok Content" }
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Randomizing fresh TikTok captions & hashtags...", 0.25f)
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
        _aiSelectedTone.value = AiContentTone.VIRAL_TREND
        _currentTab.value = AppTab.AI_STUDIO
        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Generating("Analyzing TikTok video & crafting viral hooks...", 0.25f)
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
                _toastEvents.emit("✅ Saved all $successfulCount photos to Gallery (Pictures/TikSave)!")
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

    fun renameDownloadItem(id: Long, newTitle: String) {
        viewModelScope.launch {
            try {
                repository.renameDownload(id, newTitle)
                _toastEvents.emit("Item renamed successfully")
            } catch (e: Exception) {
                _toastEvents.emit("Failed to rename: ${e.localizedMessage}")
            }
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
                        _toastEvents.emit("TikSave is up to date! (v${BuildConfig.VERSION_NAME})")
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

    // ==================== v1.1.9 Multi-Link Batch Queue Saver ====================

    fun onBatchInputChange(text: String) {
        _batchInputText.value = text
    }

    fun setBatchQuality(quality: DownloadQuality) {
        _batchSelectedQuality.value = quality
    }

    fun pasteBatchFromClipboard() {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString() ?: ""
                val links = DownloadRepository.extractAllTikTokUrls(text)
                if (links.isNotEmpty()) {
                    val existing = _batchInputText.value.trim()
                    val merged = if (existing.isEmpty()) {
                        links.joinToString("\n")
                    } else {
                        (DownloadRepository.extractAllTikTokUrls(existing) + links).distinct().joinToString("\n")
                    }
                    _batchInputText.value = merged
                    showToast("Added ${links.size} TikTok link(s) to Batch Queue")
                } else {
                    showToast("No valid TikTok links found in clipboard")
                }
            } else {
                showToast("Clipboard is empty")
            }
        } catch (_: Exception) {
            showToast("Could not access clipboard")
        }
    }

    fun startBatchDownload() {
        val urls = DownloadRepository.extractAllTikTokUrls(_batchInputText.value)
        if (urls.isEmpty()) {
            showToast("Paste at least 1 TikTok link in the Batch Queue first")
            return
        }
        if (_batchState.value?.isRunning == true) {
            showToast("Batch Queue is already running")
            return
        }

        val targetQuality = _batchSelectedQuality.value
        batchDownloadJob?.cancel()
        batchDownloadJob = viewModelScope.launch {
            var completed = 0
            var failed = 0
            _batchState.value = BatchDownloadState(
                isRunning = true,
                totalCount = urls.size,
                currentIndex = 1,
                currentStatus = "Resolving link 1 of ${urls.size}...",
                completedCount = 0,
                failedCount = 0
            )

            try {
                urls.forEachIndexed { index, rawUrl ->
                    val itemNumber = index + 1
                    _batchState.value = _batchState.value?.copy(
                        currentIndex = itemNumber,
                        currentStatus = "Resolving #$itemNumber/${urls.size}..."
                    )

                    val extraction = repository.resolveTikTokVideo(
                        rawInput = rawUrl,
                        hdPriority = _hdrBoostEnabled.value
                    ) { _, _ -> }

                    if (extraction is ExtractionState.Success) {
                        val meta = extraction.metadata
                        val isAudio = targetQuality == DownloadQuality.AUDIO_ONLY ||
                                targetQuality == DownloadQuality.AUDIO_HIGH_SOUND ||
                                targetQuality == DownloadQuality.AUDIO_VFX_SOUND

                        val downloadUrl = when {
                            meta.isPhotoSlide && !isAudio -> meta.images.firstOrNull() ?: meta.coverUrl
                            targetQuality == DownloadQuality.ULTRA_8K_120FPS -> meta.ultra8kUrl.ifBlank { meta.ultraHdrUrl }
                            targetQuality == DownloadQuality.HDR_ULTRA -> meta.ultraHdrUrl.ifBlank { meta.standardHdUrl }
                            targetQuality == DownloadQuality.HD_STANDARD -> meta.standardHdUrl
                            isAudio -> meta.audioUrl.ifBlank { meta.standardHdUrl }
                            else -> meta.ultraHdrUrl.ifBlank { meta.standardHdUrl }
                        }

                        if (downloadUrl.isNotBlank()) {
                            val ext = when {
                                isAudio -> "mp3"
                                meta.isPhotoSlide -> "jpg"
                                else -> "mp4"
                            }
                            val cleanTitle = meta.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20)
                            val filename = "TikSave_Batch_${itemNumber}_${cleanTitle}_${System.currentTimeMillis()}.$ext"

                            _batchState.value = _batchState.value?.copy(
                                currentStatus = "Downloading #$itemNumber: ${meta.title.take(28)}..."
                            )

                            val dlRes = repository.downloadManager.download(
                                url = downloadUrl,
                                targetFilename = filename,
                                quality = if (meta.isPhotoSlide && !isAudio) DownloadQuality.PHOTO_SLIDE_ULTRA else targetQuality
                            ) { _, _, _, _, _ -> }

                            if (dlRes.isSuccess) {
                                val file = dlRes.getOrNull()?.file
                                if (file != null) {
                                    repository.saveDownloadRecord(
                                        meta,
                                        if (meta.isPhotoSlide && !isAudio) DownloadQuality.PHOTO_SLIDE_ULTRA else targetQuality,
                                        file
                                    )
                                }
                                completed++
                            } else {
                                failed++
                            }
                        } else {
                            failed++
                        }
                    } else {
                        failed++
                    }

                    _batchState.value = _batchState.value?.copy(
                        completedCount = completed,
                        failedCount = failed
                    )
                }

                _batchState.value = BatchDownloadState(
                    isRunning = false,
                    totalCount = urls.size,
                    currentIndex = urls.size,
                    currentStatus = "Completed: $completed saved" + if (failed > 0) " ($failed failed)" else "!",
                    completedCount = completed,
                    failedCount = failed,
                    isFinished = true
                )
                showToast("✅ Batch Queue finished: $completed of ${urls.size} saved to Library!")
            } catch (_: kotlinx.coroutines.CancellationException) {
                _batchState.value = null
            }
        }
    }

    fun cancelBatchDownload() {
        batchDownloadJob?.cancel()
        _batchState.value = null
        showToast("Batch Queue cancelled")
    }

    fun dismissBatchState() {
        _batchState.value = null
    }
}
