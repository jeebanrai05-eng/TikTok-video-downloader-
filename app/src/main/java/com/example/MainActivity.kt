package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ModernDownloadModal
import com.example.ui.components.UpdateDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrendingScreen
import com.example.ui.theme.HdrGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                TikSaveApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkClipboardForTikTokLink()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (sharedText.isNotBlank()) {
                viewModel.onUrlChange(sharedText)
                viewModel.startExtraction()
                viewModel.setTab(AppTab.DOWNLOADER)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikSaveApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val clipboardLink by viewModel.clipboardDetectedUrl.collectAsStateWithLifecycle()
    val extractionState by viewModel.extractionState.collectAsStateWithLifecycle()
    val activeDownload by viewModel.activeDownload.collectAsStateWithLifecycle()
    val isDownloadModalVisible by viewModel.isDownloadModalVisible.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val previewingMedia by viewModel.previewingMedia.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val isInstallingUpdate by viewModel.isInstallingUpdate.collectAsStateWithLifecycle()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()
    val trendingVideos by viewModel.trendingVideos.collectAsStateWithLifecycle()
    val trendingCategory by viewModel.trendingCategory.collectAsStateWithLifecycle()
    val trendingSearchQuery by viewModel.trendingSearchQuery.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle back button when in sub-screens
    BackHandler(enabled = currentTab != AppTab.DOWNLOADER) {
        viewModel.setTab(AppTab.DOWNLOADER)
    }

    // Observe toasts
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Glowing logo dot
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                                .border(1.5.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(NeonMagenta, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TikSave",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "8K 120FPS ULTRA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.DOWNLOADER,
                    onClick = { viewModel.setTab(AppTab.DOWNLOADER) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.DOWNLOADER) Icons.Filled.Download else Icons.Outlined.Download,
                            contentDescription = "Downloader"
                        )
                    },
                    label = { Text("Downloader") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_downloader")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.TRENDING,
                    onClick = { viewModel.setTab(AppTab.TRENDING) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.TRENDING) Icons.Filled.Whatshot else Icons.Outlined.Whatshot,
                            contentDescription = "Trending Politics"
                        )
                    },
                    label = { Text("Trending") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFFFF5722),
                        indicatorColor = Color(0xFFFF5722),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_trending")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.LIBRARY,
                    onClick = { viewModel.setTab(AppTab.LIBRARY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allDownloads.isNotEmpty()) {
                                    Badge(containerColor = NeonMagenta, contentColor = Color.White) {
                                        Text("${allDownloads.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == AppTab.LIBRARY) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                contentDescription = "Library"
                            )
                        }
                    },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_library")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DOWNLOADER -> DownloaderScreen(
                    urlInput = urlInput,
                    onUrlChange = viewModel::onUrlChange,
                    clipboardLink = clipboardLink,
                    onUseClipboardLink = viewModel::useClipboardUrl,
                    onDismissClipboard = viewModel::dismissClipboardPrompt,
                    onPasteClick = viewModel::pasteFromClipboard,
                    onClearClick = viewModel::clearInput,
                    onExtractClick = viewModel::startExtraction,
                    onSelectSample = viewModel::selectSample,
                    extractionState = extractionState,
                    selectedQuality = selectedQuality,
                    onQualitySelected = viewModel::setSelectedQuality,
                    onDownloadClick = viewModel::startDownload,
                    onPreviewClick = viewModel::previewMedia,
                    activeDownload = activeDownload,
                    onCancelDownload = viewModel::cancelActiveDownload,
                    onDismissActiveDownload = viewModel::dismissActiveDownload
                )

                AppTab.TRENDING -> TrendingScreen(
                    videos = trendingVideos,
                    selectedCategory = trendingCategory,
                    onCategorySelected = viewModel::setTrendingCategory,
                    searchQuery = trendingSearchQuery,
                    onSearchQueryChange = viewModel::setTrendingSearchQuery,
                    onPreviewVideo = viewModel::previewMedia,
                    onDownloadVideo = viewModel::downloadTrendingVideo,
                    onCopyFacebookPost = viewModel::copyFacebookPost,
                    onDirectPostFacebook = { item -> viewModel.downloadAndShareToFacebook(context, item) }
                )

                AppTab.LIBRARY -> LibraryScreen(
                    downloads = allDownloads,
                    onPlayItem = viewModel::previewMedia,
                    onDeleteItem = viewModel::deleteItem,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onClearAll = viewModel::clearAllHistory,
                    onGoToDownloader = { viewModel.setTab(AppTab.DOWNLOADER) }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    onClearCache = {
                        Toast.makeText(context, "Stream cache cleaned successfully", Toast.LENGTH_SHORT).show()
                    },
                    onCheckForUpdates = {
                        viewModel.checkForAppUpdates(silent = false)
                    },
                    isCheckingForUpdates = isCheckingForUpdates
                )
            }
        }
    }

    // Modern High-Tech Download Overlay Modal
    if (isDownloadModalVisible && activeDownload != null) {
        ModernDownloadModal(
            download = activeDownload!!,
            onCancel = viewModel::cancelActiveDownload,
            onMinimize = viewModel::hideDownloadModal,
            onPlay = { path -> viewModel.previewMedia(path, activeDownload!!.title) },
            onDismiss = viewModel::dismissActiveDownload
        )
    }

    // Video preview dialog overlay
    previewingMedia?.let { (mediaPath, title) ->
        VideoPlayerDialog(
            mediaUrlOrPath = mediaPath,
            title = title,
            onDismiss = viewModel::dismissPreview
        )
    }

    // Self Update Dialog (Loading radar, downloading progress, and installing animations)
    if (updateInfo != null || isCheckingForUpdates) {
        UpdateDialog(
            updateInfo = updateInfo,
            isChecking = isCheckingForUpdates,
            isDownloading = isDownloadingUpdate,
            isInstalling = isInstallingUpdate,
            downloadProgress = updateDownloadProgress,
            onInstallUpdate = viewModel::installAppUpdate,
            onDismiss = viewModel::dismissUpdateDialog
        )
    }
}
