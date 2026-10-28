package com.example.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val isUpdateAvailable: Boolean,
    val versionCode: Int = 2
)

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        const val UPDATE_CHANNEL_ID = "tiksave_updates"
        const val UPDATE_NOTIFICATION_ID = 9001
        private const val RAW_VERSION_MAIN = "https://raw.githubusercontent.com/jeebanrai05-eng/TikTok-video-downloader-/main/version.json"
        private const val RAW_VERSION_MASTER = "https://raw.githubusercontent.com/jeebanrai05-eng/TikTok-video-downloader-/master/version.json"
        private const val GITHUB_API_LATEST = "https://api.github.com/repos/jeebanrai05-eng/TikTok-video-downloader-/releases/latest"
    }

    suspend fun checkForUpdates(currentVersion: String = "1.0.2"): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Check raw version.json from repository
            val versionEndpoints = listOf(RAW_VERSION_MAIN, RAW_VERSION_MASTER)
            for (endpoint in versionEndpoints) {
                try {
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Cache-Control", "no-cache")
                        .build()
                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            val vName = json.optString("versionName", "1.0.2")
                            val vCode = json.optInt("versionCode", 2)
                            val title = json.optString("releaseTitle", "TikSave 8K 120FPS Ultra v$vName")
                            val notes = json.optString("releaseNotes", "• 8K Ultra 120FPS streaming\n• Duplicate download fixed\n• Performance improvements")
                            val apk = json.optString("apkUrl", "https://github.com/jeebanrai05-eng/TikTok-video-downloader-/releases/download/v$vName/TikSave-Release.apk")

                            val hasNewer = isNewerVersion(currentVersion, vName) || vCode > 2
                            return@withContext Result.success(
                                AppUpdateInfo(
                                    latestVersion = "v$vName",
                                    releaseTitle = title,
                                    releaseNotes = notes,
                                    apkDownloadUrl = apk,
                                    isUpdateAvailable = hasNewer,
                                    versionCode = vCode
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Try next endpoint
                }
            }

            // Step 2: Fallback to GitHub Releases API
            val request = Request.Builder()
                .url(GITHUB_API_LATEST)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "TikSave-Android-App")
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                null
            }

            if (response != null && response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "v1.0.2")
                val cleanTag = tagName.removePrefix("v").trim()
                val releaseName = json.optString("name", "TikSave 8K 120FPS Ultra v1.0.2")
                val body = json.optString("body", "• 8K 120FPS ultra smooth support\n• Faster processing engine")

                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (apkUrl.isBlank()) {
                    apkUrl = "https://github.com/jeebanrai05-eng/TikTok-video-downloader-/releases/download/$tagName/TikSave-Release.apk"
                }

                val hasNewer = isNewerVersion(currentVersion, cleanTag)
                Result.success(
                    AppUpdateInfo(
                        latestVersion = tagName,
                        releaseTitle = releaseName,
                        releaseNotes = body,
                        apkDownloadUrl = apkUrl,
                        isUpdateAvailable = hasNewer
                    )
                )
            } else {
                // If offline or repo release pending, show current up-to-date state
                Result.success(
                    AppUpdateInfo(
                        latestVersion = "v1.0.2",
                        releaseTitle = "TikSave 8K 120FPS Ultra v1.0.2",
                        releaseNotes = "• 8K Ultra 120FPS Extreme stream mode\n• Fixed double video download bug\n• Modern animated update manager\n• Direct device storage & Gallery sync",
                        apkDownloadUrl = "https://github.com/jeebanrai05-eng/TikTok-video-downloader-/releases",
                        isUpdateAvailable = false
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendUpdateNotification(updateInfo: AppUpdateInfo) {
        try {
            createNotificationChannel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("KEY_OPEN_UPDATE", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("TikSave Update: ${updateInfo.latestVersion}")
                .setContentText("Tap to upgrade to 8K 120FPS ultra speeds and bug fixes.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle("New Update Available: ${updateInfo.releaseTitle}")
                        .bigText("${updateInfo.releaseNotes}\n\nTap to install directly inside TikSave.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TikSave App Updates"
            val descriptionText = "Notifications for new TikSave releases and ultra feature updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(UPDATE_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(currParts.size, latestParts.size)) {
                val c = currParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updateFile = File(context.cacheDir, "TikSave-Update.apk")
            if (updateFile.exists()) updateFile.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "TikSave-Updater")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download update: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty update response"))
            val totalBytes = body.contentLength()
            var downloaded = 0L
            val buffer = ByteArray(64 * 1024)
            var read: Int

            body.byteStream().use { inputStream: InputStream ->
                FileOutputStream(updateFile).use { outputStream ->
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress(downloaded.toFloat() / totalBytes.toFloat())
                        }
                    }
                    outputStream.flush()
                }
            }

            // Launch system package installer
            promptInstall(updateFile)
            Result.success(updateFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstall(apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
