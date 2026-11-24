package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import com.example.ui.theme.HdrGold
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExtractionState
import com.example.ui.components.ModernDownloadModal
import com.example.ui.components.UpdateDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.UltraPurple
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
    val aiInputText by viewModel.aiInputText.collectAsStateWithLifecycle()
    val aiSelectedTone by viewModel.aiSelectedTone.collectAsStateWithLifecycle()
    val aiGenerationState by viewModel.aiGenerationState.collectAsStateWithLifecycle()

    val ultra8kEnabled by viewModel.ultra8k120FpsEnabled.collectAsStateWithLifecycle()
    val hdrBoostEnabled by viewModel.hdrBoostEnabled.collectAsStateWithLifecycle()
    val turboSpeedEnabled by viewModel.turboSpeedEnabled.collectAsStateWithLifecycle()
    val autoClipboardEnabled by viewModel.autoClipboardEnabled.collectAsStateWithLifecycle()
    val wifiOnlyEnabled by viewModel.wifiOnlyEnabled.collectAsStateWithLifecycle()

    val logoTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by logoTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BackHandler(enabled = currentTab != AppTab.DOWNLOADER) {
        viewModel.setTab(AppTab.DOWNLOADER)
    }

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
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .scale(logoScale)
                                .background(
                                    Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.28f), Color.Transparent)),
                                    CircleShape
                                )
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(listOf(NeonCyan, NeonMagenta)),
                                    CircleShape
                                ),
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(NeonCyan.copy(alpha = 0.16f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} ULTRA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = NeonCyan,
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.showToast("TikSave PRO ULTRA Active ⭐") },
                        modifier = Modifier.testTag("btn_pro_crown")
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "PRO ULTRA",
                            tint = HdrGold
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.setTab(AppTab.SETTINGS) },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeonCyan
                        )
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
                    selected = currentTab == AppTab.AI_STUDIO,
                    onClick = { viewModel.setTab(AppTab.AI_STUDIO) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.AI_STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "AI Studio"
                        )
                    },
                    label = { Text("AI Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = UltraPurple,
                        indicatorColor = ElectricIndigo,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_tab_ai_studio")
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
        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(240)) { it / 10 * direction })
                    .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { -it / 10 * direction })
            },
            label = "main_tab_transition"
        ) { tab ->
            when (tab) {
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
                    onGenerateAiClick = viewModel::launchAiWithVideo,
                    onDownloadPhotoSlide = { url, idx ->
                        val currentExt = extractionState
                        if (currentExt is ExtractionState.Success) {
                            viewModel.downloadPhotoSlide(currentExt.metadata, url, idx)
                        }
                    },
                    activeDownload = activeDownload,
                    onCancelDownload = viewModel::cancelActiveDownload,
                    onDismissActiveDownload = viewModel::dismissActiveDownload
                )

                AppTab.AI_STUDIO -> AiStudioScreen(
                    inputText = aiInputText,
                    onInputChange = viewModel::setAiInputText,
                    selectedTone = aiSelectedTone,
                    onToneSelected = viewModel::setAiSelectedTone,
                    generationState = aiGenerationState,
                    onGenerateClick = viewModel::generateAiViralPack,
                    onRegenerateClick = viewModel::regenerateAiViralPack
                )

                AppTab.LIBRARY -> LibraryScreen(
                    downloads = allDownloads,
                    onPlayItem = viewModel::previewMedia,
                    onDeleteItem = viewModel::deleteItem,
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onClearAll = viewModel::clearAllHistory,
                    onGoToDownloader = { viewModel.setTab(AppTab.DOWNLOADER) },
                    onRenameItem = { item, newTitle -> viewModel.renameDownloadItem(item.id, newTitle) }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    ultra8k120FpsEnabled = ultra8kEnabled,
                    onUltra8k120FpsChange = viewModel::setUltra8k120FpsEnabled,
                    hdrBoostEnabled = hdrBoostEnabled,
                    onHdrBoostChange = viewModel::setHdrBoostEnabled,
                    turboSpeedEnabled = turboSpeedEnabled,
                    onTurboSpeedChange = viewModel::setTurboSpeedEnabled,
                    autoClipboardEnabled = autoClipboardEnabled,
                    onAutoClipboardChange = viewModel::setAutoClipboardEnabled,
                    wifiOnlyEnabled = wifiOnlyEnabled,
                    onWifiOnlyChange = viewModel::setWifiOnlyEnabled,
                    onClearCache = viewModel::clearCache,
                    onCheckForUpdates = { viewModel.checkForAppUpdates(silent = false) },
                    isCheckingForUpdates = isCheckingForUpdates
                )
            }
        }
    }

    if (isDownloadModalVisible && activeDownload != null) {
        ModernDownloadModal(
            download = activeDownload!!,
            onCancel = viewModel::cancelActiveDownload,
            onMinimize = viewModel::hideDownloadModal,
            onPlay = { path -> viewModel.previewMedia(path, activeDownload!!.title) },
            onDismiss = viewModel::dismissActiveDownload
        )
    }

    previewingMedia?.let { (mediaPath, title) ->
        VideoPlayerDialog(
            mediaUrlOrPath = mediaPath,
            title = title,
            onDismiss = viewModel::dismissPreview
        )
    }

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
