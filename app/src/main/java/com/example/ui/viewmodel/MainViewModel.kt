package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.DownloadItem
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadQuality
import com.example.data.model.ExtractionState
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

import android.content.Intent
import android.net.Uri
import com.example.data.model.TrendingPoliticsItem
import com.example.data.repository.TrendingPoliticsRepository
import kotlinx.coroutines.flow.combine

enum class AppTab {
    DOWNLOADER,
    TRENDING,
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

    // Trending Nepal Politics state
    private val _trendingCategory = MutableStateFlow("सबै (All)")
    val trendingCategory: StateFlow<String> = _trendingCategory.asStateFlow()

    private val _trendingSearchQuery = MutableStateFlow("")
    val trendingSearchQuery: StateFlow<String> = _trendingSearchQuery.asStateFlow()

    val trendingVideos: StateFlow<List<TrendingPoliticsItem>> = combine(
        _trendingCategory,
        _trendingSearchQuery
    ) { cat, query ->
        TrendingPoliticsRepository.getItemsForCategory(cat, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrendingPoliticsRepository.TRENDING_POLITICS_VIDEOS)

    private var currentDownloadJob: Job? = null

    init {
        checkClipboardForTikTokLink()
        // Check for updates silently on startup
        checkForAppUpdates(silent = true)
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
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
                val extracted = repository.extractTikTokUrl(text)
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
                val extracted = repository.extractTikTokUrl(text) ?: text.trim()
                _urlInput.value = extracted
                startExtraction()
            } else {
                viewModelScope.launch { _toastEvents.emit("Clipboard is empty") }
            }
        } catch (e: Exception) {
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
            viewModelScope.launch { _toastEvents.emit("Please paste a TikTok link first") }
            return
        }

        viewModelScope.launch {
            _extractionState.value = ExtractionState.Analyzing("Connecting to TikTok high-speed CDN...", 0.1f)
            val result = repository.resolveTikTokVideo(input) { step, progress ->
                _extractionState.value = ExtractionState.Analyzing(step, progress)
            }
            _extractionState.value = result
            if (result is ExtractionState.Success) {
                _toastEvents.emit("Video extracted without watermark! Choose HDR or HD.")
            }
        }
    }

    fun startDownload(metadata: VideoMetadata, quality: DownloadQuality) {
        // Prevent concurrent duplicate download clicks
        if (_isDownloading.value) {
            viewModelScope.launch { _toastEvents.emit("Download is already running in background") }
            _isDownloadModalVisible.value = true
            return
        }

        currentDownloadJob?.cancel()
        _isDownloading.value = true

        val downloadUrl = when (quality) {
            DownloadQuality.ULTRA_8K_120FPS -> metadata.ultra8kUrl
            DownloadQuality.HDR_ULTRA -> metadata.ultraHdrUrl
            DownloadQuality.HD_STANDARD -> metadata.standardHdUrl
            DownloadQuality.AUDIO_ONLY -> metadata.audioUrl
            DownloadQuality.COVER_IMAGE -> metadata.coverUrl
        }

        if (downloadUrl.isBlank()) {
            _isDownloading.value = false
            viewModelScope.launch { _toastEvents.emit("Selected stream is unavailable for this video") }
            return
        }

        val ext = when (quality) {
            DownloadQuality.AUDIO_ONLY -> "mp3"
            DownloadQuality.COVER_IMAGE -> "jpg"
            else -> "mp4"
        }

        val cleanTitle = metadata.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val filename = "TikSave_${cleanTitle}_${System.currentTimeMillis()}.$ext"

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
                else -> 0L
            },
            speedBytesPerSec = 0L,
            etaSeconds = 0
        )

        // Show modern high-tech download modal
        _isDownloadModalVisible.value = true

        currentDownloadJob = viewModelScope.launch {
            try {
                val result = repository.downloadManager.download(
                    url = downloadUrl,
                    targetFilename = filename,
                    quality = quality
                ) { progress, downloaded, total, speed, eta ->
                    _activeDownload.value = _activeDownload.value?.copy(
                        progress = progress,
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        speedBytesPerSec = speed,
                        etaSeconds = eta
                    )
                }

                result.onSuccess { downloadResult ->
                    val localFile = downloadResult.file
                    _activeDownload.value = _activeDownload.value?.copy(
                        progress = 1.0f,
                        localFilePath = localFile.absolutePath,
                        isCompleted = true,
                        speedBytesPerSec = 0L
                    )
                    repository.saveDownloadRecord(metadata, quality, localFile)
                    _toastEvents.emit("✅ Saved single file to Gallery & Device Storage (${localFile.name})")
                }.onFailure { error ->
                    _activeDownload.value = _activeDownload.value?.copy(
                        errorMessage = error.localizedMessage ?: "Download failed"
                    )
                    _toastEvents.emit("Download error: ${error.localizedMessage}")
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun showDownloadModal() {
        if (_activeDownload.value != null) {
            _isDownloadModalVisible.value = true
        }
    }

    fun hideDownloadModal() {
        _isDownloadModalVisible.value = false
    }

    fun cancelActiveDownload() {
        currentDownloadJob?.cancel()
        _isDownloading.value = false
        _activeDownload.value = null
        _isDownloadModalVisible.value = false
        viewModelScope.launch { _toastEvents.emit("Download cancelled") }
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
            _toastEvents.emit("Removed from library")
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

    // Self-Update methods
    fun checkForAppUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _isCheckingForUpdates.value = true
            }
            val result = updateManager.checkForUpdates(currentVersion = "1.0.2")
            if (!silent) {
                _isCheckingForUpdates.value = false
            }
            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    _updateInfo.value = info
                    // Trigger push notification to alert the user
                    updateManager.sendUpdateNotification(info)
                } else if (!silent) {
                    _toastEvents.emit("TikSave is up to date (${info.latestVersion})")
                }
            }.onFailure {
                if (!silent) {
                    _toastEvents.emit("Could not connect to update server")
                }
            }
        }
    }

    fun installAppUpdate() {
        val info = _updateInfo.value ?: return
        if (info.apkDownloadUrl.isBlank()) {
            FormatUtils.openFileExternally(getApplication<Application>(), "https://github.com/jeebanrai05-eng/TikTok-video-downloader-/releases", "text/html")
            _updateInfo.value = null
            return
        }

        _isDownloadingUpdate.value = true
        _updateDownloadProgress.value = 0f
        _isInstallingUpdate.value = false

        viewModelScope.launch {
            val result = updateManager.downloadAndInstallApk(info.apkDownloadUrl) { progress ->
                _updateDownloadProgress.value = progress
            }
            _isDownloadingUpdate.value = false
            result.onSuccess {
                _isInstallingUpdate.value = true
                _toastEvents.emit("Starting package installer...")
            }.onFailure { err ->
                _isInstallingUpdate.value = false
                _toastEvents.emit("Update error: ${err.localizedMessage}")
            }
        }
    }

    fun dismissUpdateDialog() {
        if (!_isDownloadingUpdate.value) {
            _updateInfo.value = null
            _isCheckingForUpdates.value = false
            _isInstallingUpdate.value = false
        }
    }

    // Trending Nepal Politics actions
    fun setTrendingCategory(category: String) {
        _trendingCategory.value = category
    }

    fun setTrendingSearchQuery(query: String) {
        _trendingSearchQuery.value = query
    }

    fun copyFacebookPost(item: TrendingPoliticsItem) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Facebook Post", item.fullFacebookPostContent)
        clipboard?.setPrimaryClip(clip)
        viewModelScope.launch {
            _toastEvents.emit("📋 Facebook Title & Hashtags copied to clipboard!")
        }
    }

    fun downloadTrendingVideo(item: TrendingPoliticsItem) {
        val metadata = VideoMetadata(
            id = item.id,
            title = item.title,
            authorNickname = item.authorName,
            authorUsername = item.authorHandle,
            authorAvatar = item.coverUrl,
            coverUrl = item.coverUrl,
            durationSeconds = 60,
            playCount = 1000000L,
            likeCount = 200000L,
            commentCount = 15000L,
            shareCount = 25000L,
            ultra8kUrl = item.videoUrl,
            ultra8kSize = 25000000L,
            ultraHdrUrl = item.videoUrl,
            ultraHdrSize = 18000000L,
            standardHdUrl = item.videoUrl,
            standardHdSize = 12000000L,
            audioUrl = item.videoUrl,
            musicTitle = "Original Political Audio - Nepal",
            musicAuthor = item.authorName,
            originalTikTokUrl = "https://www.tiktok.com/${item.authorHandle}"
        )
        startDownload(metadata, DownloadQuality.HDR_ULTRA)
    }

    fun downloadAndShareToFacebook(context: Context, item: TrendingPoliticsItem) {
        copyFacebookPost(item)
        downloadTrendingVideo(item)

        try {
            val fbIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.fullFacebookPostContent)
                setPackage("com.facebook.katana")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fbIntent)
        } catch (_: Exception) {
            try {
                val shareIntent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.fullFacebookPostContent)
                    },
                    "Post to Facebook"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        }
    }
}
