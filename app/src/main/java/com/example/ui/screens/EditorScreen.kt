package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.db.DownloadItem
import com.example.data.video.TimelineThumbnailExtractor
import com.example.data.video.TrimResult
import com.example.data.video.VideoFrameThumbnail
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    videoSourceUriOrPath: String?,
    videoTitle: String,
    allDownloads: List<DownloadItem>,
    onSelectVideo: (source: String, title: String) -> Unit,
    onExportClip: (startSec: Float, endSec: Float, clipTitle: String) -> Unit,
    isExporting: Boolean,
    exportProgress: Float,
    exportResult: TrimResult?,
    onCancelExport: () -> Unit,
    onDismissExportResult: () -> Unit,
    onPlayExportedClip: (pathOrUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Photo/Video Picker launcher for importing local videos
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectVideo(uri.toString(), "Imported Device Video")
        }
    }

    var showSourcePickerModal by remember { mutableStateOf(false) }

    if (videoSourceUriOrPath.isNullOrBlank()) {
        // Empty State: Show Video Selector & Importer
        EmptyEditorState(
            recentDownloads = allDownloads,
            onPickFromGallery = {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onSelectDownloadItem = { item ->
                onSelectVideo(item.localFilePath.ifEmpty { item.videoUrl }, item.title)
            },
            onUseSampleVideo = {
                onSelectVideo(
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "Sample Nature 1080p Clip"
                )
            },
            modifier = modifier
        )
    } else {
        // Active Editor Interface
        ActiveEditorWorkspace(
            sourceUriOrPath = videoSourceUriOrPath,
            videoTitle = videoTitle,
            onChangeVideoClick = { showSourcePickerModal = true },
            onPickFromGallery = {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            },
            onExportClip = onExportClip,
            isExporting = isExporting,
            exportProgress = exportProgress,
            exportResult = exportResult,
            onCancelExport = onCancelExport,
            onDismissExportResult = onDismissExportResult,
            onPlayExportedClip = onPlayExportedClip,
            modifier = modifier
        )
    }

    if (showSourcePickerModal) {
        Dialog(
            onDismissRequest = { showSourcePickerModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = ObsidianBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Video to Edit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        IconButton(onClick = { showSourcePickerModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showSourcePickerModal = false
                            pickMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Device Gallery", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "OR CHOOSE FROM TIK_SAVE LIBRARY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (allDownloads.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No downloaded videos yet in Library.\nDownload a video or import from device gallery.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allDownloads) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showSourcePickerModal = false
                                            onSelectVideo(item.localFilePath.ifEmpty { item.videoUrl }, item.title)
                                        },
                                    color = ObsidianSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${FormatUtils.formatDuration(item.durationSeconds)} • ${item.authorNickname}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = NeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorState(
    recentDownloads: List<DownloadItem>,
    onPickFromGallery: () -> Unit,
    onSelectDownloadItem: (DownloadItem) -> Unit,
    onUseSampleVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonMagenta.copy(alpha = 0.2f))),
                        CircleShape
                    )
                    .border(1.5.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Professional Video Cutter & Editor",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trim any section with frame-by-frame precision timeline,\nlossless MP4 export, and 9:16 Reels formatting.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Choose from Gallery
            Button(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_pick_gallery_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Select Video from Device Gallery", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Demo Video
            OutlinedButton(
                onClick = onUseSampleVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_use_sample_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = NeonMagenta)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try with Sample 1080p Video", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT DOWNLOADS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (recentDownloads.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No downloaded videos in TikSave yet", color = Color.White.copy(alpha = 0.8f))
                        Text("Paste a link in Downloader or pick from Gallery", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(recentDownloads) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectDownloadItem(item) }
                        .testTag("editor_item_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${FormatUtils.formatDuration(item.durationSeconds)} • ${item.authorNickname}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.ContentCut, contentDescription = "Cut", tint = NeonCyan)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveEditorWorkspace(
    sourceUriOrPath: String,
    videoTitle: String,
    onChangeVideoClick: () -> Unit,
    onPickFromGallery: () -> Unit,
    onExportClip: (startSec: Float, endSec: Float, clipTitle: String) -> Unit,
    isExporting: Boolean,
    exportProgress: Float,
    exportResult: TrimResult?,
    onCancelExport: () -> Unit,
    onDismissExportResult: () -> Unit,
    onPlayExportedClip: (pathOrUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(10000) }
    var isFullScreenPreview by remember { mutableStateOf(false) }

    // Start & End in seconds
    var startSeconds by remember(sourceUriOrPath) { mutableFloatStateOf(0f) }
    var endSeconds by remember(sourceUriOrPath) { mutableFloatStateOf(15f) }

    // Timeline Zoom (1x, 2x, 4x)
    var zoomLevel by remember { mutableIntStateOf(1) }

    // Thumbnails extraction
    var thumbnails by remember(sourceUriOrPath) { mutableStateOf<List<VideoFrameThumbnail>>(emptyList()) }
    var isExtractingThumbnails by remember(sourceUriOrPath) { mutableStateOf(true) }

    LaunchedEffect(sourceUriOrPath) {
        isExtractingThumbnails = true
        thumbnails = TimelineThumbnailExtractor.extractThumbnails(
            context = context,
            sourceUriOrPath = sourceUriOrPath,
            frameCount = 12
        )
        isExtractingThumbnails = false
    }

    // Playback position tracking ticker
    LaunchedEffect(videoViewRef, isPlaying) {
        while (isActive) {
            val vv = videoViewRef
            if (vv != null && isPlaying) {
                currentPlaybackMs = vv.currentPosition
                val endMs = (endSeconds * 1000f).toInt()
                if (currentPlaybackMs >= endMs) {
                    // Loop back to start
                    val startMs = (startSeconds * 1000f).toInt()
                    vv.seekTo(startMs)
                }
            }
            delay(100)
        }
    }

    val maxDurationSec = (totalDurationMs / 1000f).coerceAtLeast(1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onChangeVideoClick) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "Change Video", tint = NeonCyan)
                }
                IconButton(onClick = { isFullScreenPreview = true }) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 1. TOP: VIDEO PREVIEW PLAYER
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        when {
                            sourceUriOrPath.startsWith("content://") -> setVideoURI(Uri.parse(sourceUriOrPath))
                            sourceUriOrPath.startsWith("http://") || sourceUriOrPath.startsWith("https://") -> setVideoURI(Uri.parse(sourceUriOrPath))
                            else -> setVideoPath(sourceUriOrPath)
                        }
                        setOnPreparedListener { mp ->
                            totalDurationMs = mp.duration.coerceAtLeast(1000)
                            endSeconds = minOf(15f, totalDurationMs / 1000f)
                            mp.isLooping = false
                        }
                        setOnCompletionListener {
                            seekTo((startSeconds * 1000f).toInt())
                            start()
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.matchParentSize()
            )

            // Play/Pause Floating Overlay
            IconButton(
                onClick = {
                    val vv = videoViewRef ?: return@IconButton
                    if (isPlaying) {
                        vv.pause()
                        isPlaying = false
                    } else {
                        if (vv.currentPosition < (startSeconds * 1000f).toInt() || vv.currentPosition >= (endSeconds * 1000f).toInt()) {
                            vv.seekTo((startSeconds * 1000f).toInt())
                        }
                        vv.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .border(1.dp, NeonCyan, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Current Time / Duration Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${formatTimeWithMillis(currentPlaybackMs / 1000f)} / ${formatTimeWithMillis(maxDurationSec)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 2. MIDDLE: HORIZONTAL VISUAL TIMELINE
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header with Controls: Zoom and Play Selected
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISUAL FRAME TIMELINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan,
                        fontSize = 11.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Play Selected Section button
                        OutlinedButton(
                            onClick = {
                                val vv = videoViewRef ?: return@OutlinedButton
                                vv.seekTo((startSeconds * 1000f).toInt())
                                vv.start()
                                isPlaying = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Clip", fontSize = 11.sp, color = NeonCyan)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Zoom button
                        IconButton(
                            onClick = {
                                zoomLevel = when (zoomLevel) {
                                    1 -> 2
                                    2 -> 4
                                    else -> 1
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("${zoomLevel}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Frame-based timeline strip with actual thumbnails
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    if (isExtractingThumbnails) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading frame previews...", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    } else if (thumbnails.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            thumbnails.forEach { frame ->
                                Image(
                                    bitmap = frame.bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(72.dp)
                                        .aspectRatio(frame.bitmap.width.toFloat() / frame.bitmap.height.toFloat().coerceAtLeast(1f)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Full video stream preview active", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    // Dual Range Slider over Timeline for Start and End Handles
                    RangeSlider(
                        value = startSeconds..endSeconds,
                        onValueChange = { range ->
                            startSeconds = range.start.coerceIn(0f, maxDurationSec)
                            endSeconds = range.endInclusive.coerceIn(startSeconds + 0.5f, maxDurationSec)
                            // Seek player to start as user drags
                            videoViewRef?.seekTo((startSeconds * 1000f).toInt())
                        },
                        valueRange = 0f..maxDurationSec,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 6.dp)
                            .testTag("timeline_range_slider")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Frame-by-frame precision step buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StepButton(label = "-1s") {
                            startSeconds = (startSeconds - 1f).coerceAtLeast(0f)
                            videoViewRef?.seekTo((startSeconds * 1000f).toInt())
                        }
                        StepButton(label = "-0.1s") {
                            startSeconds = (startSeconds - 0.1f).coerceAtLeast(0f)
                            videoViewRef?.seekTo((startSeconds * 1000f).toInt())
                        }
                        StepButton(label = "+0.1s") {
                            startSeconds = (startSeconds + 0.1f).coerceAtMost(endSeconds - 0.5f)
                            videoViewRef?.seekTo((startSeconds * 1000f).toInt())
                        }
                        StepButton(label = "+1s") {
                            startSeconds = (startSeconds + 1f).coerceAtMost(endSeconds - 0.5f)
                            videoViewRef?.seekTo((startSeconds * 1000f).toInt())
                        }
                    }

                    Text("Nudge Start", color = Color.Gray, fontSize = 10.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StepButton(label = "-1s") {
                            endSeconds = (endSeconds - 1f).coerceAtLeast(startSeconds + 0.5f)
                            videoViewRef?.seekTo((endSeconds * 1000f).toInt())
                        }
                        StepButton(label = "-0.1s") {
                            endSeconds = (endSeconds - 0.1f).coerceAtLeast(startSeconds + 0.5f)
                            videoViewRef?.seekTo((endSeconds * 1000f).toInt())
                        }
                        StepButton(label = "+0.1s") {
                            endSeconds = (endSeconds + 0.1f).coerceAtMost(maxDurationSec)
                            videoViewRef?.seekTo((endSeconds * 1000f).toInt())
                        }
                        StepButton(label = "+1s") {
                            endSeconds = (endSeconds + 1f).coerceAtMost(maxDurationSec)
                            videoViewRef?.seekTo((endSeconds * 1000f).toInt())
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 3. BOTTOM: TIMESTAMPS & DURATION CHIPS
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Timestamps Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimestampBox(label = "START TIME", timeStr = formatTimeWithMillis(startSeconds), color = NeonCyan)
                    TimestampBox(label = "CLIP DURATION", timeStr = formatTimeWithMillis(endSeconds - startSeconds), color = HdrGold)
                    TimestampBox(label = "END TIME", timeStr = formatTimeWithMillis(endSeconds), color = NeonMagenta)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Duration Presets (15s, 30s, 60s, Full)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(label = "15s Reel") {
                        endSeconds = minOf(startSeconds + 15f, maxDurationSec)
                    }
                    PresetChip(label = "30s TikTok") {
                        endSeconds = minOf(startSeconds + 30f, maxDurationSec)
                    }
                    PresetChip(label = "60s Story") {
                        endSeconds = minOf(startSeconds + 60f, maxDurationSec)
                    }
                    PresetChip(label = "Full Video") {
                        startSeconds = 0f
                        endSeconds = maxDurationSec
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Action Button
                Button(
                    onClick = {
                        val title = "Clip_${videoTitle.replace(" ", "_").take(15)}"
                        onExportClip(startSeconds, endSeconds, title)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_clip_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✂️ Export Lossless MP4 Clip (${formatTimeWithMillis(endSeconds - startSeconds)})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Fullscreen Player Dialog
    if (isFullScreenPreview) {
        Dialog(
            onDismissRequest = { isFullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            when {
                                sourceUriOrPath.startsWith("content://") -> setVideoURI(Uri.parse(sourceUriOrPath))
                                sourceUriOrPath.startsWith("http://") || sourceUriOrPath.startsWith("https://") -> setVideoURI(Uri.parse(sourceUriOrPath))
                                else -> setVideoPath(sourceUriOrPath)
                            }
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                seekTo((startSeconds * 1000f).toInt())
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { isFullScreenPreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Exporting Progress Dialog
    if (isExporting) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { exportProgress },
                        color = NeonCyan,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(68.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Trimming Lossless MP4 Clip...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${(exportProgress * 100).toInt()}% • Hardware Muxing & Audio Sync",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan,
                        trackColor = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedButton(
                        onClick = onCancelExport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel Export")
                    }
                }
            }
        }
    }

    // Export Success Dialog
    if (exportResult != null) {
        Dialog(onDismissRequest = onDismissExportResult) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SuccessGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Clip Exported Successfully!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Saved to Movies/TikSave and Library\nDuration: ${FormatUtils.formatDuration(exportResult.durationSeconds)} • Size: ${FormatUtils.formatBytes(exportResult.fileSize)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismissExportResult()
                                onPlayExportedClip(exportResult.file.absolutePath, "Exported Reel")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Clip", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, exportResult.contentUri ?: Uri.fromFile(exportResult.file))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Trimmed Clip"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismissExportResult,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimestampBox(label: String, timeStr: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeStr,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(6.dp)),
        color = ObsidianBg,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        color = ObsidianBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun formatTimeWithMillis(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val ms = ((seconds - totalSec) * 10).roundToInt().coerceIn(0, 9)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d.%d", min, sec, ms)
}
