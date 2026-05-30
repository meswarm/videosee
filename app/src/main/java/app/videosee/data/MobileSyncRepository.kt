package app.videosee.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class MobileSyncRepository(private val context: Context) {
    suspend fun loadPending(baseUrl: String, token: String): List<SyncPendingFile> = withContext(Dispatchers.IO) {
        val response = request(
            url = baseUrl.joinPath("/sync/pending"),
            token = token,
            method = "GET",
        ) { connection ->
            connection.inputStream.bufferedReader().use { it.readText() }
        }
        val files = JSONObject(response).optJSONArray("files") ?: JSONArray()
        buildList {
            for (index in 0 until files.length()) {
                val file = files.getJSONObject(index)
                add(
                    SyncPendingFile(
                        id = file.getString("id"),
                        filename = file.getString("filename"),
                        mediaType = file.optString("media_type", "file"),
                        sizeBytes = file.optLong("size", 0L),
                        downloadedAt = file.optString("downloaded_at", ""),
                        downloadUrl = file.getString("download_url"),
                    ),
                )
            }
        }
    }

    suspend fun downloadAndAck(
        baseUrl: String,
        token: String,
        deviceId: String,
        file: SyncPendingFile,
    ): Uri = withContext(Dispatchers.IO) {
        val uri = saveFile(
            filename = file.filename,
            mediaType = file.mediaType,
            contentType = file.contentType,
        ) { output ->
            request(
                url = baseUrl.joinPath(file.downloadUrl),
                token = token,
                method = "GET",
            ) { connection ->
                BufferedInputStream(connection.inputStream).use { input ->
                    input.copyTo(output)
                }
            }
        }
        ack(baseUrl = baseUrl, token = token, deviceId = deviceId, fileIds = listOf(file.id))
        uri
    }

    private fun ack(baseUrl: String, token: String, deviceId: String, fileIds: List<String>) {
        val body = JSONObject()
            .put("device_id", deviceId)
            .put("file_ids", JSONArray(fileIds))
            .toString()
        request(
            url = baseUrl.joinPath("/sync/ack"),
            token = token,
            method = "POST",
            contentType = "application/json",
            body = body.toByteArray(Charsets.UTF_8),
        ) { connection ->
            connection.inputStream.close()
        }
    }

    private fun <T> request(
        url: String,
        token: String,
        method: String,
        contentType: String? = null,
        body: ByteArray? = null,
        read: (HttpURLConnection) -> T,
    ): T {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) {
                setRequestProperty("Content-Type", contentType)
            }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body) }
            }
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("Sync request failed ($code): $errorBody")
            }
            return read(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun saveFile(
        filename: String,
        mediaType: String,
        contentType: String,
        write: (java.io.OutputStream) -> Unit,
    ): Uri {
        val resolver = context.contentResolver
        val collection = when (mediaType) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }
        }
        val relativePath = when (mediaType) {
            "image" -> "Pictures/VideoSee"
            "video" -> "Movies/VideoSee"
            else -> "Download/VideoSee"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, contentType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: error("Cannot create media file")
        runCatching {
            resolver.openOutputStream(uri)?.use(write) ?: error("Cannot open media file")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
        }.onFailure {
            resolver.delete(uri, null, null)
        }.getOrThrow()
        return uri
    }

    private fun String.joinPath(path: String): String {
        val base = trimEnd('/')
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            "$base/${path.trimStart('/')}"
        }
    }
}

data class SyncPendingFile(
    val id: String,
    val filename: String,
    val mediaType: String,
    val sizeBytes: Long,
    val downloadedAt: String,
    val downloadUrl: String,
) {
    val contentType: String
        get() = when (mediaType) {
            "image" -> "image/jpeg"
            "video" -> "video/mp4"
            else -> "application/octet-stream"
        }
}
