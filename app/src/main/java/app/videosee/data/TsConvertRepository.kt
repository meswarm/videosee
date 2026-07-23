package app.videosee.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

class TsConvertRepository(private val context: Context) {
    private val scanner = TsVideoScanner()
    private val documentTreeScanner = TsDocumentTreeScanner(context)

    fun scan(sourcePaths: List<String>, downloadedKeys: Set<String>): TsScanResult {
        val documentSources = sourcePaths.filter { it.trim().startsWith("content://") }
        val fileSources = sourcePaths.filterNot { it.trim().startsWith("content://") }
        val fileResult = scanner.scan(sourcePaths = fileSources, downloadedKeys = downloadedKeys)
        val documentResult = documentTreeScanner.scan(sourceUris = documentSources, downloadedKeys = downloadedKeys)
        return TsScanResult(
            paths = fileResult.paths + documentResult.paths,
            videos = (fileResult.videos + documentResult.videos)
                .distinctBy { it.recordKey }
                .sortedByDescending { it.modifiedTimeMillis },
            issues = fileResult.issues + documentResult.issues,
        )
    }

    fun outputFileFor(video: TsVideoCandidate): File {
        return File(workDirectory, video.outputFileName)
    }

    fun privateImportDirectory(): File {
        val base = context.getExternalFilesDir(null) ?: File(context.filesDir, "external_files")
        val appExternalRoot = base.parentFile ?: base
        return File(appExternalRoot, PRIVATE_IMPORT_DIRECTORY_NAME).apply {
            mkdirs()
            runCatching { File(this, ".nomedia").createNewFile() }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun convert(video: TsVideoCandidate): File = withContext(Dispatchers.Main) {
        val outputFile = withContext(Dispatchers.IO) {
            workDirectory.mkdirs()
            outputFileFor(video).also { file ->
                if (file.exists()) file.delete()
            }
        }
        val inputPlaylist = withContext(Dispatchers.IO) {
            if (video.isDocumentTreeSource) {
                documentTreeScanner.materialize(video, File(workDirectory, "document_sources"))
            } else {
                File(video.playlistPath)
            }
        }
        val input = MediaItem.Builder()
            .setUri(Uri.fromFile(inputPlaylist))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        val editedMediaItem = EditedMediaItem.Builder(input).build()

        suspendCancellableCoroutine { continuation ->
            lateinit var transformer: Transformer
            transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(outputFile)
                    }

                    override fun onError(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(exportException))
                        }
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                runCatching { transformer.cancel() }
                runCatching { outputFile.delete() }
            }
            transformer.start(editedMediaItem, outputFile.absolutePath)
        }
    }

    suspend fun publishConverted(video: TsVideoCandidate, sourceFile: File, downloadDirectory: String): Uri = withContext(Dispatchers.IO) {
        if (!sourceFile.isFile) error("请先转换视频")
        val relativePath = downloadDirectory.toMediaStoreRelativePath()
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, video.outputFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: error("无法创建下载文件")
        runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("无法写入下载文件")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            }
            sourceFile.delete()
        }.onFailure {
            resolver.delete(uri, null, null)
        }.getOrThrow()
        uri
    }

    private val workDirectory: File
        get() = File(context.cacheDir, "ts_converted").apply { mkdirs() }

    private companion object {
        const val PRIVATE_IMPORT_DIRECTORY_NAME = "share91"
    }
}

data class TsDownloadRecord(
    val name: String,
    val outputFileName: String,
    val playlistPath: String,
    val recordKey: String,
    val contentKey: String,
    val downloadedUri: String,
    val downloadedAtMillis: Long,
)

class TsConvertSettingsStore(private val context: Context) {
    private val filesDir = context.filesDir
    private val preferences = context.getSharedPreferences("ts_convert_settings", Context.MODE_PRIVATE)
    private val downloadRecordFile: File
        get() = File(filesDir, "ts_convert_download_records.json")

    var sourcePaths: List<String>
        get() = listOf(defaultPrivateImportDirectoryPath())
        set(value) {
            preferences.edit().remove("source_paths").apply()
        }

    var downloadDirectory: String
        get() = preferences.getString("download_directory", DEFAULT_DOWNLOAD_DIRECTORY).orEmpty()
            .ifBlank { DEFAULT_DOWNLOAD_DIRECTORY }
        set(value) {
            preferences.edit().putString("download_directory", value.ifBlank { DEFAULT_DOWNLOAD_DIRECTORY }).apply()
        }

    private var legacyConvertedRecordKeys: Set<String>
        get() {
            val raw = preferences.getString("converted_record_keys", null) ?: return emptySet()
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptySet()
            return buildSet {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { array.put(it) }
            preferences.edit().putString("converted_record_keys", array.toString()).apply()
        }

    fun downloadedRecords(): List<TsDownloadRecord> {
        val root = runCatching {
            if (!downloadRecordFile.isFile) return emptyList()
            JSONObject(downloadRecordFile.readText())
        }.getOrNull() ?: return emptyList()
        val records = root.optJSONArray("records") ?: return emptyList()
        return buildList {
            for (index in 0 until records.length()) {
                val record = records.optJSONObject(index) ?: continue
                add(
                    TsDownloadRecord(
                        name = record.optString("name"),
                        outputFileName = record.optString("outputFileName"),
                        playlistPath = record.optString("playlistPath"),
                        recordKey = record.optString("recordKey"),
                        contentKey = record.optString("contentKey"),
                        downloadedUri = record.optString("downloadedUri"),
                        downloadedAtMillis = record.optLong("downloadedAtMillis"),
                    ),
                )
            }
        }
    }

    fun downloadedKeys(): Set<String> {
        val recordKeys = downloadedRecords().flatMap { listOf(it.recordKey, it.contentKey) }.filter { it.isNotBlank() }
        return legacyConvertedRecordKeys + recordKeys
    }

    fun markDownloaded(video: TsVideoCandidate, uri: Uri) {
        val nextRecords = (downloadedRecords().filterNot {
            it.recordKey == video.recordKey || it.contentKey == video.contentKey
        } + TsDownloadRecord(
            name = video.name,
            outputFileName = video.outputFileName,
            playlistPath = video.playlistPath,
            recordKey = video.recordKey,
            contentKey = video.contentKey,
            downloadedUri = uri.toString(),
            downloadedAtMillis = System.currentTimeMillis(),
        )).sortedByDescending { it.downloadedAtMillis }
        saveDownloadedRecords(nextRecords)
        legacyConvertedRecordKeys = legacyConvertedRecordKeys + video.recordKey + video.contentKey
    }

    private fun saveDownloadedRecords(records: List<TsDownloadRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("name", record.name)
                    .put("outputFileName", record.outputFileName)
                    .put("playlistPath", record.playlistPath)
                    .put("recordKey", record.recordKey)
                    .put("contentKey", record.contentKey)
                    .put("downloadedUri", record.downloadedUri)
                    .put("downloadedAtMillis", record.downloadedAtMillis),
            )
        }
        val root = JSONObject()
            .put("version", 1)
            .put("records", array)
        downloadRecordFile.parentFile?.mkdirs()
        downloadRecordFile.writeText(root.toString())
    }

    companion object {
        const val DEFAULT_DOWNLOAD_DIRECTORY = "/storage/emulated/0/Movies/VideoSee"
        const val PRIVATE_IMPORT_DIRECTORY_NAME = "share91"
    }

    private fun defaultPrivateImportDirectoryPath(): String {
        val base = context.getExternalFilesDir(null) ?: File(context.filesDir, "external_files")
        val appExternalRoot = base.parentFile ?: base
        return File(appExternalRoot, PRIVATE_IMPORT_DIRECTORY_NAME).apply {
            mkdirs()
            runCatching { File(this, ".nomedia").createNewFile() }
        }.absolutePath
    }
}

private fun String.toMediaStoreRelativePath(): String {
    val marker = "/storage/emulated/0/"
    val rawRelative = if (startsWith(marker)) substringAfter(marker) else trimStart('/')
    return rawRelative.trim('/').ifBlank { "Movies/VideoSee" }
}
