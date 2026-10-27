package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val videoId: String,
    val title: String,
    val authorNickname: String,
    val authorUsername: String,
    val authorAvatar: String,
    val coverUrl: String,
    val videoUrl: String,
    val localFilePath: String,
    val quality: String, // HDR_ULTRA, HD_STANDARD, AUDIO_ONLY, COVER_IMAGE
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val downloadedAtMillis: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
