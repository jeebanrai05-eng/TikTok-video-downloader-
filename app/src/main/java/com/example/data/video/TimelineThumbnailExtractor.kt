package com.example.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class VideoFrameThumbnail(
    val timeMs: Long,
    val bitmap: Bitmap
)

object TimelineThumbnailExtractor {

    suspend fun extractThumbnails(
        context: Context,
        sourceUriOrPath: String,
        frameCount: Int = 10,
        targetHeightPx: Int = 120
    ): List<VideoFrameThumbnail> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<VideoFrameThumbnail>()

        try {
            when {
                sourceUriOrPath.startsWith("content://") -> {
                    retriever.setDataSource(context, Uri.parse(sourceUriOrPath))
                }
                sourceUriOrPath.startsWith("file://") -> {
                    val file = File(Uri.parse(sourceUriOrPath).path ?: "")
                    if (file.exists()) {
                        retriever.setDataSource(file.absolutePath)
                    } else {
                        retriever.setDataSource(context, Uri.parse(sourceUriOrPath))
                    }
                }
                sourceUriOrPath.startsWith("/") -> {
                    retriever.setDataSource(sourceUriOrPath)
                }
                sourceUriOrPath.startsWith("http://") || sourceUriOrPath.startsWith("https://") -> {
                    retriever.setDataSource(sourceUriOrPath, HashMap<String, String>().apply {
                        put("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    })
                }
                else -> {
                    retriever.setDataSource(context, Uri.parse(sourceUriOrPath))
                }
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 10000L
            val validDuration = if (durationMs <= 0) 10000L else durationMs

            val actualFrames = frameCount.coerceIn(5, 20)
            val stepMs = validDuration / actualFrames

            for (i in 0 until actualFrames) {
                val timeMs = (i * stepMs).coerceIn(0L, validDuration)
                val timeUs = timeMs * 1000L

                val frameBitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            (targetHeightPx * 0.75f).toInt(),
                            targetHeightPx
                        ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                } catch (_: Exception) {
                    null
                }

                if (frameBitmap != null) {
                    frames.add(VideoFrameThumbnail(timeMs = timeMs, bitmap = frameBitmap))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        frames
    }
}
