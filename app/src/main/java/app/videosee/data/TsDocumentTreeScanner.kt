package app.videosee.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

class TsDocumentTreeScanner(private val context: Context) {
    fun scan(sourceUris: List<String>, downloadedKeys: Set<String>): TsScanResult {
        val statuses = mutableListOf<TsSourcePathStatus>()
        val candidatesByDirectory = linkedMapOf<String, TsVideoCandidate>()
        val issues = mutableListOf<TsScanIssue>()

        sourceUris.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { uriText ->
            val treeUri = runCatching { Uri.parse(uriText) }.getOrNull()
            val root = treeUri?.let { DocumentFile.fromTreeUri(context, it) }
            if (treeUri == null || root == null || !root.exists() || !root.isDirectory) {
                statuses += TsSourcePathStatus(uriText, isReadable = false, message = "授权文件夹不可用，请重新选择")
                return@forEach
            }
            statuses += TsSourcePathStatus(uriText, isReadable = true, message = "已授权文件夹: ${root.name.orEmpty()}")

            val playlists = mutableListOf<DocumentPlaylist>()
            walk(root = root, current = root, relativeDirectoryPath = "", output = playlists)
            if (playlists.isEmpty()) {
                issues += TsScanIssue(root.uri.toString(), "授权文件夹内没有找到 .m3u8 文件")
            }
            playlists.forEach { playlist ->
                when (val parsed = parsePlaylist(treeUri, root, playlist)) {
                    is ParsedDocumentPlaylist.Candidate -> putBestCandidate(candidatesByDirectory, parsed.video)
                    is ParsedDocumentPlaylist.Issue -> {
                        issues += parsed.issue
                        parseSiblingLocalPlaylist(treeUri, root, playlist)?.let { putBestCandidate(candidatesByDirectory, it) }
                    }
                }
            }
        }

        val videos = candidatesByDirectory.values
            .filterNot { it.recordKey in downloadedKeys || it.contentKey in downloadedKeys }
            .sortedByDescending { it.modifiedTimeMillis }
        return TsScanResult(
            paths = statuses,
            videos = videos,
            issues = issues.distinctBy { it.path to it.reason }.take(MAX_ISSUES),
        )
    }

    fun materialize(video: TsVideoCandidate, destinationRoot: java.io.File): java.io.File {
        val treeUri = video.sourceTreeUri?.let(Uri::parse) ?: error("不是授权文件夹来源")
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("授权文件夹不可用，请重新选择")
        val sourceDirectory = video.relativeDirectoryPath
            ?.split('/')
            ?.filter { it.isNotBlank() }
            ?.fold(root) { current, name -> current.findFile(name) ?: error("找不到源目录: $name") }
            ?: root
        val playlist = sourceDirectory.findFile("${video.name}.m3u8") ?: error("找不到清单: ${video.name}.m3u8")
        val lines = readText(playlist)
        val segmentNames = lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .filterNot { it.isNetworkReference() }
            .map { it.stripQueryAndFragment() }
        val keyName = lines.firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("#EXT-X-KEY")) return@firstNotNullOfOrNull null
            URI_ATTRIBUTE.find(trimmed)?.groupValues?.getOrNull(1)?.stripQueryAndFragment()
        }

        val destinationDirectory = java.io.File(destinationRoot, video.name).apply {
            deleteRecursively()
            mkdirs()
        }
        java.io.File(destinationDirectory, playlist.name ?: "${video.name}.m3u8").writeText(lines.normalizedLocalPlaylist())
        if (keyName != null) {
            val key = sourceDirectory.findFile(keyName) ?: error("找不到 key 文件: $keyName")
            copyDocumentToFile(key, java.io.File(destinationDirectory, keyName))
        }
        segmentNames.forEach { segmentName ->
            val segment = sourceDirectory.findFile(segmentName) ?: error("找不到分片: $segmentName")
            copyDocumentToFile(segment, java.io.File(destinationDirectory, segmentName))
        }
        return java.io.File(destinationDirectory, "${video.name}.m3u8")
    }

    private fun walk(
        root: DocumentFile,
        current: DocumentFile,
        relativeDirectoryPath: String,
        output: MutableList<DocumentPlaylist>,
    ) {
        current.listFiles().forEach { child ->
            when {
                child.isDirectory -> {
                    val childName = child.name.orEmpty()
                    val childRelativePath = listOf(relativeDirectoryPath, childName)
                        .filter { it.isNotBlank() }
                        .joinToString("/")
                    walk(root = root, current = child, relativeDirectoryPath = childRelativePath, output = output)
                }
                child.isFile && child.name.orEmpty().endsWith(".m3u8", ignoreCase = true) -> {
                    output += DocumentPlaylist(root, current, relativeDirectoryPath, child)
                }
            }
        }
    }

    private fun parseSiblingLocalPlaylist(treeUri: Uri, root: DocumentFile, outerPlaylist: DocumentPlaylist): TsVideoCandidate? {
        val siblingDirectory = outerPlaylist.directory.findFile(outerPlaylist.file.nameWithoutExtension()) ?: return null
        if (!siblingDirectory.isDirectory) return null
        val siblingPlaylist = siblingDirectory.findFile(outerPlaylist.file.name.orEmpty()) ?: return null
        val siblingRelativePath = listOf(outerPlaylist.relativeDirectoryPath, siblingDirectory.name.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("/")
        return (parsePlaylist(treeUri, root, DocumentPlaylist(root, siblingDirectory, siblingRelativePath, siblingPlaylist)) as? ParsedDocumentPlaylist.Candidate)?.video
    }

    private fun parsePlaylist(treeUri: Uri, root: DocumentFile, playlist: DocumentPlaylist): ParsedDocumentPlaylist {
        val lines = runCatching { readText(playlist.file) }.getOrNull()
            ?: return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "无法读取清单文件"))
        if (lines.none { it.trim() == "#EXTM3U" }) {
            return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "不是有效的 m3u8 清单"))
        }
        if (lines.none { it.trim().startsWith("#EXTINF") }) {
            return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "清单没有分片条目"))
        }

        val keyUri = lines.firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("#EXT-X-KEY")) return@firstNotNullOfOrNull null
            URI_ATTRIBUTE.find(trimmed)?.groupValues?.getOrNull(1)
        }
        if (keyUri?.isNetworkReference() == true) {
            return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "这是远程清单，已忽略；需要同名子目录里的本地清单"))
        }
        val keySize = if (keyUri != null) {
            val keyName = keyUri.stripQueryAndFragment()
            val keyFile = playlist.directory.findFile(keyName)
                ?: return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "缺少 key 文件: $keyName"))
            if (keyFile.length() != AES_128_KEY_BYTES) {
                return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "key 文件不是 16 字节: ${keyFile.name}"))
            }
            keyFile.length()
        } else {
            0L
        }

        val segmentFiles = lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .filterNot { it.isNetworkReference() }
            .map { it.stripQueryAndFragment() to playlist.directory.findFile(it.stripQueryAndFragment()) }
        if (segmentFiles.isEmpty()) {
            return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "没有本地 ts 分片；可能仍是远程清单"))
        }
        val missingSegment = segmentFiles.firstOrNull { it.second == null }
        if (missingSegment != null) {
            return ParsedDocumentPlaylist.Issue(TsScanIssue(playlist.file.uri.toString(), "缺少分片: ${missingSegment.first}"))
        }

        val durationSeconds = lines.sumOf { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("#EXTINF:")) {
                0.0
            } else {
                trimmed.substringAfter("#EXTINF:")
                    .substringBefore(",")
                    .toDoubleOrNull() ?: 0.0
            }
        }
        val segmentSize = segmentFiles.sumOf { it.second?.length() ?: 0L }
        val name = playlist.file.nameWithoutExtension()
        return ParsedDocumentPlaylist.Candidate(
            TsVideoCandidate(
                id = playlist.file.uri.toString(),
                name = name,
                playlistPath = playlist.file.uri.toString(),
                sourcePath = root.uri.toString(),
                sourceTreeUri = treeUri.toString(),
                relativeDirectoryPath = playlist.relativeDirectoryPath,
                segmentCount = segmentFiles.size,
                durationSeconds = durationSeconds,
                totalSizeBytes = segmentSize + keySize + playlist.file.length(),
                modifiedTimeMillis = maxOf(playlist.file.lastModified(), segmentFiles.maxOfOrNull { it.second?.lastModified() ?: 0L } ?: 0L),
                outputFileName = "${name.sanitizeFileName()}.mp4",
            ),
        )
    }

    private fun readText(file: DocumentFile): List<String> {
        return context.contentResolver.openInputStream(file.uri)
            ?.bufferedReader()
            ?.use { it.readLines() }
            ?: error("无法读取文件")
    }

    private fun copyDocumentToFile(source: DocumentFile, destination: java.io.File) {
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取文件: ${source.name}")
    }

    private fun List<String>.normalizedLocalPlaylist(): String {
        return joinToString(separator = "\n") { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXT-X-KEY") -> {
                    URI_ATTRIBUTE.replace(line) { match ->
                        "URI=\"${match.groupValues[1].stripQueryAndFragment()}\""
                    }
                }
                trimmed.isNotBlank() && !trimmed.startsWith("#") && !trimmed.isNetworkReference() -> trimmed.stripQueryAndFragment()
                else -> line
            }
        }
    }

    private fun putBestCandidate(candidatesByDirectory: MutableMap<String, TsVideoCandidate>, candidate: TsVideoCandidate) {
        val directoryKey = candidate.relativeDirectoryPath ?: candidate.playlistPath.substringBeforeLast("/")
        val existing = candidatesByDirectory[directoryKey]
        if (existing == null || candidate.segmentCount > existing.segmentCount) {
            candidatesByDirectory[directoryKey] = candidate
        }
    }

    private data class DocumentPlaylist(
        val root: DocumentFile,
        val directory: DocumentFile,
        val relativeDirectoryPath: String,
        val file: DocumentFile,
    )

    private sealed interface ParsedDocumentPlaylist {
        data class Candidate(val video: TsVideoCandidate) : ParsedDocumentPlaylist
        data class Issue(val issue: TsScanIssue) : ParsedDocumentPlaylist
    }

    private fun DocumentFile.nameWithoutExtension(): String {
        return name.orEmpty().substringBeforeLast(".")
    }

    private fun String.isNetworkReference(): Boolean {
        val normalized = lowercase(Locale.US)
        return normalized.startsWith("http://") || normalized.startsWith("https://")
    }

    private fun String.stripQueryAndFragment(): String {
        return substringBefore("?").substringBefore("#")
    }

    private fun String.sanitizeFileName(): String {
        return replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "video" }
    }

    private companion object {
        const val AES_128_KEY_BYTES = 16L
        const val MAX_ISSUES = 20
        val URI_ATTRIBUTE = Regex("""URI="([^"]+)"""")
    }
}
