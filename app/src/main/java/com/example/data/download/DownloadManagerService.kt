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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
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
     * Resolves the primary public storage directory on the device (Downloads/TikSave or Movies/TikSave)
     */
    fun getPublicStorageDir(quality: DownloadQuality): File {
        val publicCategory = when (quality) {
            DownloadQuality.AUDIO_ONLY -> Environment.DIRECTORY_MUSIC
            DownloadQuality.COVER_IMAGE -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MOVIES
        }

        // 1. Try public storage (e.g., /storage/emulated/0/Movies/TikSave or Downloads/TikSave)
        val publicBase = Environment.getExternalStoragePublicDirectory(publicCategory)
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFolder = File(publicBase, "TikSave")
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        // 2. If accessible and writable, return it
        if (targetFolder.exists() && (targetFolder.canWrite() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
            return targetFolder
        }

        // Fallback to secondary public downloads
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
            val mimeType = when (quality) {
                DownloadQuality.AUDIO_ONLY -> "audio/mp3"
                DownloadQuality.COVER_IMAGE -> "image/jpeg"
                else -> "video/mp4"
            }

            // Target public device folder
            val destinationDir = getPublicStorageDir(quality)
            val outputFile = File(destinationDir, targetFilename)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "identity") // Ensures accurate content-length for smooth progress
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
                        if (elapsed >= 250) {
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

            // Sync with Android MediaStore so it immediately appears in Gallery & Files
            var mediaStoreUri: Uri? = null
            try {
                mediaStoreUri = registerInMediaStore(outputFile, targetFilename, mimeType, quality)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Trigger MediaScanner for instant Gallery recognition
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf(mimeType)
                ) { path, uri ->
                    if (mediaStoreUri == null) mediaStoreUri = uri
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

            val locationText = "Saved to Gallery & ${outputFile.parentFile?.name ?: "Downloads"}/${outputFile.name}"
            Result.success(DownloadResult(outputFile, mediaStoreUri, locationText))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserts the downloaded file into Android's native MediaStore so Gallery & Google Photos display it instantly
     */
    private fun registerInMediaStore(
        file: File,
        filename: String,
        mimeType: String,
        quality: DownloadQuality
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Uri.fromFile(file)
        }

        val resolver = context.contentResolver
        val (contentUri, relativePath) = when (quality) {
            DownloadQuality.AUDIO_ONLY -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to (Environment.DIRECTORY_MUSIC + "/TikSave")
            DownloadQuality.COVER_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to (Environment.DIRECTORY_PICTURES + "/TikSave")
            else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to (Environment.DIRECTORY_MOVIES + "/TikSave")
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(contentUri, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outStream: OutputStream ->
                FileInputStream(file).use { inStream: InputStream ->
                    val copyBuffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inStream.read(copyBuffer).also { bytesRead = it } != -1) {
                        outStream.write(copyBuffer, 0, bytesRead)
                    }
                    outStream.flush()
                }
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }
}
