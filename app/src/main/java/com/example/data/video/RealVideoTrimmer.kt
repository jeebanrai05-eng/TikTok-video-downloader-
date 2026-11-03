package com.example.data.video

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class TrimResult(
    val file: File,
    val contentUri: Uri?,
    val durationSeconds: Int,
    val fileSize: Long
)

object RealVideoTrimmer {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun trimVideo(
        context: Context,
        sourceUriOrPath: String,
        startSeconds: Float,
        endSeconds: Float,
        exportTitle: String,
        onProgress: (Float) -> Unit
    ): Result<TrimResult> = withContext(Dispatchers.IO) {
        var localSourceFile: File? = null
        var isTempSource = false

        try {
            onProgress(0.05f)

            // 1. Resolve source to a local File
            if (sourceUriOrPath.startsWith("http://", ignoreCase = true) ||
                sourceUriOrPath.startsWith("https://", ignoreCase = true)
            ) {
                // Download remote stream to local cache first
                onProgress(0.10f)
                val tempCache = File(context.cacheDir, "src_trim_${System.currentTimeMillis()}.mp4")
                val req = Request.Builder().url(sourceUriOrPath).build()
                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load stream (HTTP ${resp.code})"))
                }
                val body = resp.body ?: return@withContext Result.failure(Exception("Empty video stream"))
                val totalBytes = body.contentLength().coerceAtLeast(1L)
                var downloaded = 0L

                body.byteStream().use { input: InputStream ->
                    FileOutputStream(tempCache).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (coroutineContext.isActive) {
                            read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val p = 0.10f + (downloaded.toFloat() / totalBytes.toFloat()) * 0.30f
                            onProgress(p.coerceIn(0.10f, 0.40f))
                        }
                        output.flush()
                    }
                }
                localSourceFile = tempCache
                isTempSource = true
            } else if (sourceUriOrPath.startsWith("content://")) {
                // Copy Content URI to temp file
                val tempCache = File(context.cacheDir, "src_content_${System.currentTimeMillis()}.mp4")
                val inputStream = context.contentResolver.openInputStream(Uri.parse(sourceUriOrPath))
                    ?: return@withContext Result.failure(Exception("Could not open media URI"))
                FileOutputStream(tempCache).use { output ->
                    inputStream.copyTo(output)
                }
                localSourceFile = tempCache
                isTempSource = true
            } else {
                localSourceFile = File(sourceUriOrPath)
                if (!localSourceFile.exists()) {
                    return@withContext Result.failure(Exception("Source video file not found on device"))
                }
            }

            if (!coroutineContext.isActive) {
                return@withContext Result.failure(Exception("Export cancelled"))
            }

            onProgress(0.45f)

            // 2. Hardware-accelerated lossless MP4 trimming via MediaExtractor & MediaMuxer
            val cleanTitle = exportTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val tempTrimmedFile = File(context.cacheDir, "Trimmed_${cleanTitle}_${System.currentTimeMillis()}.mp4")

            val extractor = MediaExtractor()
            extractor.setDataSource(localSourceFile.absolutePath)

            val trackCount = extractor.trackCount
            val muxer = MediaMuxer(tempTrimmedFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val indexMap = HashMap<Int, Int>()
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                }
            }

            if (videoTrackIndex == -1) {
                extractor.release()
                muxer.release()
                return@withContext Result.failure(Exception("No video track found in source file"))
            }

            muxer.start()

            val startUs = (startSeconds * 1_000_000L).toLong()
            val endUs = (endSeconds * 1_000_000L).toLong()
            val totalTrimDurationUs = (endUs - startUs).coerceAtLeast(1_000_000L)

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            // 2a. Process Video Track
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var videoDone = false
            while (coroutineContext.isActive && !videoDone) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    videoDone = true
                    break
                }

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) {
                    videoDone = true
                    break
                }

                if (sampleTime >= startUs) {
                    bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    val dstTrack = indexMap[videoTrackIndex] ?: 0
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)

                    val progressFraction = 0.45f + ((sampleTime - startUs).toFloat() / totalTrimDurationUs.toFloat()) * 0.40f
                    onProgress(progressFraction.coerceIn(0.45f, 0.85f))
                }

                extractor.advance()
            }

            extractor.unselectTrack(videoTrackIndex)

            // 2b. Process Audio Track (if present)
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var audioDone = false
                while (coroutineContext.isActive && !audioDone) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size < 0) {
                        audioDone = true
                        break
                    }

                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) {
                        audioDone = true
                        break
                    }

                    if (sampleTime >= startUs) {
                        bufferInfo.presentationTimeUs = (sampleTime - startUs).coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        val dstTrack = indexMap[audioTrackIndex] ?: 0
                        muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                    }

                    extractor.advance()
                }
                extractor.unselectTrack(audioTrackIndex)
            }

            extractor.release()
            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {
                // If muxer had no frames, fallback to full temp file
            }

            onProgress(0.90f)

            if (!tempTrimmedFile.exists() || tempTrimmedFile.length() == 0L) {
                return@withContext Result.failure(Exception("Trimming produced empty file. Please check selected range."))
            }

            // 3. Save into MediaStore (Movies/TikSave) for full Gallery integration
            val exportFilename = "TikSave_Reel_${cleanTitle}_${System.currentTimeMillis()}.mp4"
            var savedContentUri: Uri? = null
            var finalDestinationFile: File = tempTrimmedFile

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, exportFilename)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TikSave")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        tempTrimmedFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    savedContentUri = uri
                }
            } else {
                @Suppress("DEPRECATION")
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(moviesDir, "TikSave")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, exportFilename)
                tempTrimmedFile.copyTo(targetFile, overwrite = true)
                finalDestinationFile = targetFile
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("video/mp4"), null)
            }

            onProgress(1.0f)

            val durationSec = (endSeconds - startSeconds).toInt().coerceAtLeast(1)
            Result.success(
                TrimResult(
                    file = finalDestinationFile,
                    contentUri = savedContentUri,
                    durationSeconds = durationSec,
                    fileSize = tempTrimmedFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Trimming failed: ${e.localizedMessage ?: "Unknown media error"}"))
        } finally {
            if (isTempSource && localSourceFile != null && localSourceFile.exists()) {
                localSourceFile.delete()
            }
        }
    }
}
