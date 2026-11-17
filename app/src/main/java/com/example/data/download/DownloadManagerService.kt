package com.example.data.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.api.TikTokNetworkClient
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

    private val client = TikTokNetworkClient.downloadClient

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
            val isTikTokCdn = lowerUrl.contains("tiktok.com") ||
                    lowerUrl.contains("tikwm.com") ||
                    lowerUrl.contains("tiktokcdn") ||
                    lowerUrl.contains("byteoversea") ||
                    lowerUrl.contains("ibytedtos")

            fun createCall(attempt: Int): okhttp3.Response {
                val reqBuilder = Request.Builder().url(url)
                when (attempt) {
                    0 -> {
                        if (isTikTokCdn) {
                            reqBuilder.header("User-Agent", com.example.data.api.TikTokNetworkClient.DESKTOP_USER_AGENT)
                            reqBuilder.header("Referer", "https://www.tiktok.com/")
                            reqBuilder.header("Origin", "https://www.tiktok.com")
                        } else {
                            reqBuilder.header("User-Agent", com.example.data.api.TikTokNetworkClient.DESKTOP_USER_AGENT)
                        }
                        reqBuilder.header("Accept", "*/*")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                    1 -> {
                        // Stage 2: Mobile Chrome + Range for strict CDN hotlink checks
                        reqBuilder.header("User-Agent", com.example.data.api.TikTokNetworkClient.MOBILE_USER_AGENT)
                        if (isTikTokCdn) {
                            reqBuilder.header("Referer", "https://www.tiktok.com/")
                        }
                        reqBuilder.header("Accept", "*/*")
                        reqBuilder.header("Range", "bytes=0-")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                    else -> {
                        // Stage 3: Media player UA without Referer
                        reqBuilder.header("User-Agent", "ExoPlayer/2.19.1 (Linux;Android 14) AndroidXMedia3/1.2.0")
                        reqBuilder.header("Accept", "*/*")
                        reqBuilder.header("Connection", "keep-alive")
                    }
                }
                return client.newCall(reqBuilder.build()).execute()
            }

            var response = createCall(0)
            if (!response.isSuccessful) {
                response.close()
                response = createCall(1)
                if (!response.isSuccessful) {
                    response.close()
                    response = createCall(2)
                }
            }

            if (!response.isSuccessful) {
                val code = response.code
                val msg = response.message.ifBlank { "Unable to fetch stream" }
                response.close()
                return@withContext Result.failure(Exception("Stream error ($code): $msg. Please re-fetch the TikTok link."))
            }

            val tempCacheFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}_$targetFilename")
            if (tempCacheFile.exists()) tempCacheFile.delete()

            var downloadedBytes = 0L
            var totalBytes = -1L

            response.use { okResp ->
                val body = okResp.body ?: return@withContext Result.failure(Exception("Empty response body from TikTok CDN"))
                totalBytes = body.contentLength()
                var lastSpeedCheckTime = System.currentTimeMillis()
                var bytesSinceLastCheck = 0L
                val buffer = ByteArray(128 * 1024)

                body.byteStream().use { inputStream: InputStream ->
                    FileOutputStream(tempCacheFile).use { outputStream ->
                        var read: Int
                        while (coroutineContext.isActive) {
                            read = inputStream.read(buffer)
                            if (read == -1) break
                            outputStream.write(buffer, 0, read)
                            downloadedBytes += read
                            bytesSinceLastCheck += read

                            val currentTime = System.currentTimeMillis()
                            val elapsed = currentTime - lastSpeedCheckTime
                            if (elapsed >= 160) {
                                val currentSpeed = (bytesSinceLastCheck * 1000L) / elapsed.coerceAtLeast(1L)
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
            }

            if (!coroutineContext.isActive) {
                if (tempCacheFile.exists()) tempCacheFile.delete()
                return@withContext Result.failure(Exception("Download cancelled"))
            }

            if (downloadedBytes <= 0L) {
                if (tempCacheFile.exists()) tempCacheFile.delete()
                return@withContext Result.failure(Exception("Downloaded file was empty. Please try fetching again."))
            }

            var mediaUri: Uri? = null
            var finalLibraryFile: File

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

                val persistentDir = File(context.getExternalFilesDir(null), relativeFolder)
                if (!persistentDir.exists()) persistentDir.mkdirs()
                finalLibraryFile = File(persistentDir, targetFilename)
                tempCacheFile.copyTo(finalLibraryFile, overwrite = true)
                tempCacheFile.delete()
            } else {
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

                @Suppress("DEPRECATION")
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
