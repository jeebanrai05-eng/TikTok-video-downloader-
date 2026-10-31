package com.example.data.download

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
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

    // Turbo HTTP client with high-speed connection pooling & keep-alive
    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Resolves the primary public storage directory on the device (Pictures for images, Music for audio, Movies for videos)
     * Exactly one folder is used so no duplicate files are created.
     */
    fun getPublicStorageDir(quality: DownloadQuality, filename: String = ""): File {
        val isImage = quality == DownloadQuality.COVER_IMAGE ||
                quality == DownloadQuality.PHOTO_SLIDE_ULTRA ||
                filename.endsWith(".jpg", ignoreCase = true) ||
                filename.endsWith(".jpeg", ignoreCase = true) ||
                filename.endsWith(".png", ignoreCase = true) ||
                filename.endsWith(".webp", ignoreCase = true)

        val isAudio = quality == DownloadQuality.AUDIO_ONLY ||
                filename.endsWith(".mp3", ignoreCase = true) ||
                filename.endsWith(".m4a", ignoreCase = true)

        val publicCategory = when {
            isAudio -> Environment.DIRECTORY_MUSIC
            isImage -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MOVIES
        }

        // 1. Try public device directory (e.g., /storage/emulated/0/Pictures/TikSave or Movies/TikSave)
        val publicBase = Environment.getExternalStoragePublicDirectory(publicCategory)
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFolder = File(publicBase, "TikSave")
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        if (targetFolder.exists() && (targetFolder.canWrite() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
            return targetFolder
        }

        // Fallback to public downloads
        val dlFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TikSave")
        if (!dlFolder.exists()) {
            dlFolder.mkdirs()
        }
        if (dlFolder.exists() && dlFolder.canWrite()) {
            return dlFolder
        }

        // Fallback to app external files dir
        val appExternal = context.getExternalFilesDir(publicCategory) ?: context.filesDir
        val appFolder = File(appExternal, "TikSave")
        if (!appFolder.exists()) appFolder.mkdirs()
        return appFolder
    }

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
                    targetFilename.endsWith(".mp3", ignoreCase = true) ||
                    targetFilename.endsWith(".m4a", ignoreCase = true)

            val mimeType = when {
                isAudio -> "audio/mpeg"
                isImage -> "image/jpeg"
                else -> "video/mp4"
            }

            // Target SINGLE public device file - no second copy!
            val destinationDir = getPublicStorageDir(quality, targetFilename)
            val outputFile = File(destinationDir, targetFilename)

            // Remove any pre-existing incomplete file with same name
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Referer", "https://www.tiktok.com/")
                .header("Origin", "https://www.tiktok.com")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "identity")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            var lastSpeedCheckTime = System.currentTimeMillis()
            var bytesSinceLastCheck = 0L
            var currentSpeed = 0L

            // 128 KB buffer for ultra high throughput
            val buffer = ByteArray(128 * 1024)
            var read = 0

            body.byteStream().use { inputStream: InputStream ->
                FileOutputStream(outputFile).use { outputStream ->
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
                if (outputFile.exists()) outputFile.delete()
                return@withContext Result.failure(Exception("Download cancelled"))
            }

            // Register the single file in Android MediaStore via MediaScannerConnection (NO DUPLICATE COPY)
            var mediaStoreUri: Uri? = null
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf(mimeType)
                ) { path, uri ->
                    mediaStoreUri = uri
                }

                // Broadcast media scan for maximum compatibility across all Android versions
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(outputFile)
                }
                context.sendBroadcast(scanIntent)
            } catch (_: Exception) {
            }

            // Final progress 100%
            onProgress(1f, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes, 0L, 0)

            val locationText = "Saved in Gallery & ${outputFile.parentFile?.name ?: "Movies"}/${outputFile.name}"
            Result.success(DownloadResult(outputFile, mediaStoreUri, locationText))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
