package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    fun formatDuration(seconds: Int): String {
        val safeSec = seconds.coerceAtLeast(0)
        val m = safeSec / 60
        val s = safeSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun detectMimeType(filePath: String, qualityName: String = ""): String {
        val lower = filePath.lowercase(Locale.US)
        return when {
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") ||
                    qualityName.startsWith("AUDIO_") -> "audio/mpeg"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                    lower.endsWith(".webp") || qualityName == "COVER_IMAGE" || qualityName == "PHOTO_SLIDE_ULTRA" -> "image/jpeg"
            else -> "video/mp4"
        }
    }

    fun shareFile(context: Context, filePath: String, mimeType: String? = null) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val resolvedMime = mimeType ?: detectMimeType(filePath)
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share TikSave Media"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openFileExternally(context: Context, filePath: String, mimeType: String = "video/*") {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open in Gallery / Player"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDeviceGallery(context: Context, filePath: String) {
        val lower = filePath.lowercase(Locale.US)
        val mimeType = when {
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") -> "audio/*"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") -> "image/*"
            else -> "video/*"
        }
        openFileExternally(context, filePath, mimeType)
    }
}
