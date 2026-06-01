package app.videosee.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import app.videosee.domain.MediaItem
import app.videosee.domain.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class VideoThumbnailCacheRepository(private val context: Context) {
    private val semaphore = Semaphore(3)
    private val cacheDir: File
        get() = File(context.cacheDir, "video_thumbnails").apply { mkdirs() }

    suspend fun getOrCreate(item: MediaItem): File? {
        if (item.mediaType != MediaType.Video) return null
        val output = File(cacheDir, VideoThumbnailCacheKey.filename(item))
        if (output.isFile && output.length() > 0L) {
            output.setLastModified(System.currentTimeMillis())
            return output
        }
        return semaphore.withPermit {
            if (output.isFile && output.length() > 0L) {
                output.setLastModified(System.currentTimeMillis())
                output
            } else {
                createThumbnail(item, output)
            }
        }
    }

    private suspend fun createThumbnail(item: MediaItem, output: File): File? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, Uri.parse(item.uri))
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.frameAtTime
            } ?: return@withContext null
            val scaled = bitmap.scaleToMaxWidth(320)
            output.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            }
            if (scaled !== bitmap) {
                scaled.recycle()
            }
            bitmap.recycle()
            output
        }.getOrNull()
    }

    private fun Bitmap.scaleToMaxWidth(maxWidth: Int): Bitmap {
        if (width <= maxWidth || width <= 0 || height <= 0) return this
        val targetHeight = (height * (maxWidth.toFloat() / width.toFloat())).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, maxWidth, targetHeight, true)
    }
}

object VideoThumbnailCacheKey {
    fun filename(item: MediaItem): String {
        return filename(
            uri = item.uri,
            dateModifiedSeconds = item.dateModifiedSeconds,
            durationMillis = item.durationMillis,
        )
    }

    fun filename(uri: String, dateModifiedSeconds: Long, durationMillis: Long?): String {
        return sha256("$uri|$dateModifiedSeconds|${durationMillis ?: 0L}") + ".jpg"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
