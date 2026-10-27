package com.example.data.download

import android.content.Context
import android.os.Environment
import com.example.data.model.DownloadQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class DownloadManagerService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getDownloadDir(quality: DownloadQuality): File {
        val subDir = when (quality) {
            DownloadQuality.AUDIO_ONLY -> Environment.DIRECTORY_MUSIC
            DownloadQuality.COVER_IMAGE -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MOVIES
        }
        val baseDir = context.getExternalFilesDir(subDir) ?: context.filesDir
        val appFolder = File(baseDir, "TikSave")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    suspend fun download(
        url: String,
        targetFilename: String,
        quality: DownloadQuality,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long, etaSeconds: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destinationDir = getDownloadDir(quality)
            val outputFile = File(destinationDir, targetFilename)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
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

            val buffer = ByteArray(64 * 1024) // 64 KB buffer for high speed
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
                        if (elapsed >= 300) {
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

            // Final progress 100%
            onProgress(1f, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes, 0L, 0)
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
