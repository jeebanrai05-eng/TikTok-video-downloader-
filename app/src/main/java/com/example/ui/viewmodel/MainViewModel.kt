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

enum class AppTab {
    DOWNLOADER,
    LIBRARY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    private val _activeDownload = MutableStateFlow<ActiveDownload?>(null)
    val activeDownload: StateFlow<ActiveDownload?> = _activeDownload.asStateFlow()

    private val _selectedQuality = MutableStateFlow(DownloadQuality.HDR_ULTRA)
    val selectedQuality: StateFlow<DownloadQuality> = _selectedQuality.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.DOWNLOADER)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _previewingMedia = MutableStateFlow<Pair<String, String>?>(null) // (urlOrPath, title)
    val previewingMedia: StateFlow<Pair<String, String>?> = _previewingMedia.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    // Download history & favorites
    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDownloads: StateFlow<List<DownloadItem>> = repository.favoriteDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentDownloadJob: Job? = null

    init {
        checkClipboardForTikTokLink()
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
            // Ignore clipboard security exceptions if restricted
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
        currentDownloadJob?.cancel()

        val downloadUrl = when (quality) {
            DownloadQuality.HDR_ULTRA -> metadata.ultraHdrUrl
            DownloadQuality.HD_STANDARD -> metadata.standardHdUrl
            DownloadQuality.AUDIO_ONLY -> metadata.audioUrl
            DownloadQuality.COVER_IMAGE -> metadata.coverUrl
        }

        if (downloadUrl.isBlank()) {
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
                DownloadQuality.HDR_ULTRA -> metadata.ultraHdrSize
                DownloadQuality.HD_STANDARD -> metadata.standardHdSize
                else -> 0L
            },
            speedBytesPerSec = 0L,
            etaSeconds = 0
        )

        currentDownloadJob = viewModelScope.launch {
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
                _toastEvents.emit("✅ Saved to Phone Gallery & Device Storage (${localFile.name})")
            }.onFailure { error ->
                _activeDownload.value = _activeDownload.value?.copy(
                    errorMessage = error.localizedMessage ?: "Download failed"
                )
                _toastEvents.emit("Download error: ${error.localizedMessage}")
            }
        }
    }

    fun cancelActiveDownload() {
        currentDownloadJob?.cancel()
        _activeDownload.value = null
        viewModelScope.launch { _toastEvents.emit("Download cancelled") }
    }

    fun dismissActiveDownload() {
        _activeDownload.value = null
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
}
