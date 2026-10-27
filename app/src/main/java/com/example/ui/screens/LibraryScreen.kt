package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DownloadItem
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HdrGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.util.FormatUtils

enum class LibraryFilter {
    ALL,
    HDR,
    AUDIO,
    FAVORITES
}

@Composable
fun LibraryScreen(
    downloads: List<DownloadItem>,
    onPlayItem: (pathOrUrl: String, title: String) -> Unit,
    onDeleteItem: (DownloadItem) -> Unit,
    onToggleFavorite: (id: Long) -> Unit,
    onClearAll: () -> Unit,
    onGoToDownloader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }

    val filteredList = downloads.filter { item ->
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.authorNickname.contains(searchQuery, ignoreCase = true) ||
                item.authorUsername.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.HDR -> item.quality == "HDR_ULTRA"
            LibraryFilter.AUDIO -> item.quality == "AUDIO_ONLY"
            LibraryFilter.FAVORITES -> item.isFavorite
        }

        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Saved Library",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    text = "${downloads.size} items downloaded without watermark",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (downloads.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear All Library",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text("Search by title or @creator...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = ObsidianSurface,
                unfocusedContainerColor = ObsidianSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "All (${downloads.size})",
                selected = selectedFilter == LibraryFilter.ALL,
                onClick = { selectedFilter = LibraryFilter.ALL }
            )
            FilterChipItem(
                label = "🌟 HDR Ultra",
                selected = selectedFilter == LibraryFilter.HDR,
                onClick = { selectedFilter = LibraryFilter.HDR }
            )
            FilterChipItem(
                label = "🎵 Audio",
                selected = selectedFilter == LibraryFilter.AUDIO,
                onClick = { selectedFilter = LibraryFilter.AUDIO }
            )
            FilterChipItem(
                label = "❤️ Favorites",
                selected = selectedFilter == LibraryFilter.FAVORITES,
                onClick = { selectedFilter = LibraryFilter.FAVORITES }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Downloads List or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ObsidianSurfaceVariant, CircleShape)
                            .border(1.dp, ObsidianCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching downloads found" else "No saved videos yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Paste a TikTok link to download HDR videos or extract audio without any watermark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onGoToDownloader,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_download_now_button")
                    ) {
                        Text("Download A Video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    LibraryDownloadCard(
                        item = item,
                        onPlay = { onPlayItem(item.localFilePath, item.title) },
                        onShare = {
                            FormatUtils.shareFile(
                                context = context,
                                filePath = item.localFilePath,
                                mimeType = if (item.quality == "AUDIO_ONLY") "audio/mp3" else "video/mp4"
                            )
                        },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (selected) NeonCyan else ObsidianCardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        color = if (selected) NeonCyan.copy(alpha = 0.15f) else ObsidianSurface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) NeonCyan else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LibraryDownloadCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .testTag("library_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .clickable(onClick = onPlay)
            ) {
                AsyncImage(
                    model = item.coverUrl.ifEmpty { "https://picsum.photos/seed/${item.id}/200/300" },
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center)
                        .background(NeonCyan.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.quality == "AUDIO_ONLY") Icons.Default.Audiotrack else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Duration badge
                if (item.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(item.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Quality badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeText, badgeColor) = when (item.quality) {
                        "HDR_ULTRA" -> "HDR+ ULTRA" to HdrGold
                        "AUDIO_ONLY" -> "MP3 AUDIO" to NeonMagenta
                        else -> "FULL HD" to NeonCyan
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.authorNickname,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${FormatUtils.formatBytes(item.fileSizeBytes)} • ${FormatUtils.formatDate(item.downloadedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            // Quick actions
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) NeonMagenta else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
