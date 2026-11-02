package com.example.data.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.DownloadQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class DownloadResult(
    val file: File,
    val contentUri: Uri? = null,
    val storageLocationText: String = ""
)

class DownloadManagerService(private val context: Context) {

    // High performance HTTP client with connection pooling and resilient timeouts
    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun download(
        url: String,
        targetFilename: String,
        quality: DownloadQuality,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long, etaSeconds: Int) -> Unit
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        try {
            val isImage = quality == DownloadQuality.COVER_IMAGE ||
                    quality == DownloadQuality.PHOTO_SLIDE_ULTRA ||
                    targetFilename.endsWith(".jpg", ignoreCase = true) ||
                    targetFilename.endsWith(".jpeg", ignoreCase = true) ||
                    targetFilename.endsWith(".png", ignoreCase = true) ||
                    targetFilename.endsWith(".webp", ignoreCase = true)

            val isAudio = quality == DownloadQuality.AUDIO_ONLY ||
                    quality == DownloadQuality.AUDIO_HIGH_SOUND ||
                    quality == DownloadQuality.AUDIO_VFX_SOUND ||
                    targetFilename.endsWith(".mp3", ignoreCase = true) ||
                    targetFilename.endsWith(".m4a", ignoreCase = true) ||
                    targetFilename.endsWith(".wav", ignoreCase = true)

            val mimeType = when {
                isAudio -> "audio/mpeg"
                isImage -> "image/jpeg"
                else -> "video/mp4"
            }

            val relativeFolder = when {
                isAudio -> "Music/TikSave"
                isImage -> "Pictures/TikSave"
                else -> "Movies/TikSave"
            }

            val lowerUrl = url.lowercase()
            val isTikTok = lowerUrl.contains("tiktok.com") || lowerUrl.contains("tikwm.com") || lowerUrl.contains("byteoversea") || lowerUrl.contains("ibytedtos")
            val isYouTube = lowerUrl.contains("googlevideo.com") || lowerUrl.contains("youtube.com") || lowerUrl.contains("ytimg.com")
            val isFacebook = lowerUrl.contains("fbcdn.net") || lowerUrl.contains("facebook.com")

            fun createCall(attempt: Int): okhttp3.Response {
                val reqBuilder = Request.Builder().url(url)
                when (attempt) {
                    0 -> {
                        // Stage 1: Domain-appropriate headers
                        when {
                            isTikTok -> {
                                reqBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                                reqBuilder.header("Referer", "https://www.tiktok.com/")
                                reqBuilder.header("Origin", "https://www.tiktok.com")
                            }
                            isYouTube -> {
                                reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                                reqBuilder.header("Referer", "https://www.youtube.com/")
                            }
                            isFacebook -> {
                                reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                                reqBuilder.header("Referer", "https://www.facebook.com/")
                            }
                            else -> {
                                reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                            }
                        }
                        reqBuilder.header("Accept", "*/*")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                    1 -> {
                        // Stage 2 (Bypass 403 Forbidden): Desktop Chrome with NO Referer and NO Origin
                        reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                        reqBuilder.header("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5")
                        reqBuilder.header("Range", "bytes=0-")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                    else -> {
                        // Stage 3 (ExoPlayer stream header)
                        reqBuilder.header("User-Agent", "ExoPlayer/2.19.1 (Linux;Android 14) AndroidXMedia3/1.2.0")
                        reqBuilder.header("Accept", "*/*")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                }
                return client.newCall(reqBuilder.build()).execute()
            }

            var response = createCall(0)
            if (response.code == 403 || response.code == 401 || !response.isSuccessful) {
                response.close()
                // Retry without Referer/Origin
                response = createCall(1)
                if (response.code == 403 || response.code == 401 || !response.isSuccessful) {
                    response.close()
                    // Retry with media player UA
                    response = createCall(2)
                }
            }

            if (!response.isSuccessful) {
                val code = response.code
                val msg = response.message
                response.close()
                return@withContext Result.failure(Exception("HTTP Error $code: $msg"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            var lastSpeedCheckTime = System.currentTimeMillis()
            var bytesSinceLastCheck = 0L
            var currentSpeed = 0L

            val buffer = ByteArray(128 * 1024)
            var read = 0

            // 1. Download to safe application cache directory first.
            // This guarantees zero EPERM errors during download and prevents incomplete files in the Gallery.
            val tempCacheFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$targetFilename")
            if (tempCacheFile.exists()) tempCacheFile.delete()

            body.byteStream().use { inputStream: InputStream ->
                FileOutputStream(tempCacheFile).use { outputStream ->
                    while (coroutineContext.isActive) {
                        read = inputStream.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesSinceLastCheck += read

                        val currentTime = System.currentTimeMillis()
                        val elapsed = currentTime - lastSpeedCheckTime
                        if (elapsed >= 200) {
                            currentSpeed = ((bytesSinceLastCheck * 1000L) / elapsed.coerceAtLeast(1L))
                            lastSpeedCheckTime = currentTime
                            bytesSinceLastCheck = 0L

                            val progressFraction = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }

                            val etaSeconds = if (currentSpeed > 0 && totalBytes > downloadedBytes) {
                                ((totalBytes - downloadedBytes) / currentSpeed).toInt()
                            } else 0

                            onProgress(progressFraction, downloadedBytes, totalBytes, currentSpeed, etaSeconds)
                        }
                    }
                    outputStream.flush()
                }
            }

            if (!coroutineContext.isActive) {
                if (tempCacheFile.exists()) tempCacheFile.delete()
                return@withContext Result.failure(Exception("Download cancelled"))
            }

            // 2. Commit file to Android MediaStore (Android 10+ / Scoped Storage) or Public Directory
            var mediaUri: Uri? = null
            var finalLibraryFile: File = tempCacheFile

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Official MediaStore API for Android 10, 11, 12, 13, 14, 15, 16
                // Zero permission required, zero EPERM errors, instantly visible in phone Gallery & Music players
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFilename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = when {
                    isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    isImage -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val insertedUri = context.contentResolver.insert(collectionUri, values)
                if (insertedUri != null) {
                    context.contentResolver.openOutputStream(insertedUri)?.use { outStream ->
                        tempCacheFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                        outStream.flush()
                    }

                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(insertedUri, values, null, null)
                    mediaUri = insertedUri
                }

                // Also save persistent copy in app external files directory for local DB and built-in player
                val persistentDir = File(context.getExternalFilesDir(null), relativeFolder)
                if (!persistentDir.exists()) persistentDir.mkdirs()
                finalLibraryFile = File(persistentDir, targetFilename)
                tempCacheFile.copyTo(finalLibraryFile, overwrite = true)
                tempCacheFile.delete()
            } else {
                // Android 9 and lower: Direct public directory
                val publicType = when {
                    isAudio -> Environment.DIRECTORY_MUSIC
                    isImage -> Environment.DIRECTORY_PICTURES
                    else -> Environment.DIRECTORY_MOVIES
                }
                val publicDir = File(Environment.getExternalStoragePublicDirectory(publicType), "TikSave")
                if (!publicDir.exists()) publicDir.mkdirs()
                finalLibraryFile = File(publicDir, targetFilename)
                tempCacheFile.copyTo(finalLibraryFile, overwrite = true)
                tempCacheFile.delete()

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(finalLibraryFile.absolutePath),
                    arrayOf(mimeType)
                ) { _, uri -> mediaUri = uri }

                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(finalLibraryFile)
                }
                context.sendBroadcast(scanIntent)
            }

            onProgress(1f, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes, 0L, 0)

            val displayLocation = when {
                isAudio -> "Music/TikSave"
                isImage -> "Pictures/TikSave (Gallery)"
                else -> "Movies/TikSave (Gallery)"
            }

            Result.success(DownloadResult(finalLibraryFile, mediaUri, "Saved to $displayLocation"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
