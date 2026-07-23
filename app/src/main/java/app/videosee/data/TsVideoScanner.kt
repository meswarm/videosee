package app.videosee.data

import java.io.File
import java.util.Locale

data class TsSourcePathStatus(
    val path: String,
    val isReadable: Boolean,
    val message: String,
)

data class TsScanIssue(
    val path: String,
    val reason: String,
)

data class TsVideoCandidate(
    val id: String,
    val name: String,
    val playlistPath: String,
    val sourcePath: String,
    val sourceTreeUri: String? = null,
    val relativeDirectoryPath: String? = null,
    val segmentCount: Int,
    val durationSeconds: Double,
    val totalSizeBytes: Long,
    val modifiedTimeMillis: Long,
    val outputFileName: String,
) {
    val recordKey: String
        get() = "$playlistPath:$totalSizeBytes:$modifiedTimeMillis"

    val contentKey: String
        get() = "$name:$segmentCount:${durationSeconds.roundForKey()}:$totalSizeBytes"

    val isDocumentTreeSource: Boolean
        get() = sourceTreeUri != null
}

data class TsScanResult(
    val paths: List<TsSourcePathStatus>,
    val videos: List<TsVideoCandidate>,
    val issues: List<TsScanIssue> = emptyList(),
)

class TsVideoScanner {
    fun scan(sourcePaths: List<String>, downloadedKeys: Set<String> = emptySet()): TsScanResult {
        val pathStatuses = mutableListOf<TsSourcePathStatus>()
        val candidatesByDirectory = linkedMapOf<String, TsVideoCandidate>()
        val issues = mutableListOf<TsScanIssue>()
        sourcePaths.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { path ->
            val root = File(path)
            val status = root.readabilityStatus(path)
            pathStatuses += status
            if (!status.isReadable) return@forEach

            val playlists = root.walkTopDown()
                .onEnter { directory -> directory.canRead() }
                .filter { file -> file.isFile && file.extension.equals("m3u8", ignoreCase = true) }
                .toList()
            if (playlists.isEmpty()) {
                issues += TsScanIssue(
                    path = root.absolutePath,
                    reason = "没有找到 .m3u8 文件；如果目录确实有文件，可能是系统不允许按路径读取这些非媒体文件",
                )
            }
            playlists.forEach { playlist ->
                when (val parsed = parsePlaylist(playlist, root)) {
                    is ParsedPlaylist.Candidate -> putBestCandidate(candidatesByDirectory, parsed.video)
                    is ParsedPlaylist.Issue -> {
                        issues += parsed.issue
                        parseSiblingLocalPlaylist(playlist, root)?.let { putBestCandidate(candidatesByDirectory, it) }
                    }
                }
            }
        }

        val videos = candidatesByDirectory.values
            .filterNot { it.recordKey in downloadedKeys || it.contentKey in downloadedKeys }
            .sortedByDescending { it.modifiedTimeMillis }
        return TsScanResult(
            paths = pathStatuses,
            videos = videos,
            issues = issues.distinctBy { it.path to it.reason }.take(MAX_ISSUES),
        )
    }

    private fun putBestCandidate(candidatesByDirectory: MutableMap<String, TsVideoCandidate>, candidate: TsVideoCandidate) {
        val directoryKey = File(candidate.playlistPath).parentFile?.canonicalPathOrAbsolute().orEmpty()
        val existing = candidatesByDirectory[directoryKey]
        if (existing == null || candidate.segmentCount > existing.segmentCount) {
            candidatesByDirectory[directoryKey] = candidate
        }
    }

    private fun parseSiblingLocalPlaylist(outerPlaylist: File, sourceRoot: File): TsVideoCandidate? {
        val siblingDirectory = File(outerPlaylist.parentFile ?: return null, outerPlaylist.nameWithoutExtension)
        val siblingPlaylist = File(siblingDirectory, outerPlaylist.name)
        if (!siblingPlaylist.isFile) return null
        return (parsePlaylist(siblingPlaylist, sourceRoot) as? ParsedPlaylist.Candidate)?.video
    }

    private fun parsePlaylist(playlist: File, sourceRoot: File): ParsedPlaylist {
        val lines = runCatching { playlist.readLines() }.getOrNull()
            ?: return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "无法读取清单文件"))
        if (lines.none { it.trim() == "#EXTM3U" }) {
            return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "不是有效的 m3u8 清单"))
        }
        if (lines.none { it.trim().startsWith("#EXTINF") }) {
            return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "清单没有分片条目"))
        }

        val parent = playlist.parentFile
            ?: return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "清单没有父目录"))

        val keyUri = lines.firstNotNullOfOrNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("#EXT-X-KEY")) return@firstNotNullOfOrNull null
            URI_ATTRIBUTE.find(trimmed)?.groupValues?.getOrNull(1)
        }
        if (keyUri?.isNetworkReference() == true) {
            return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "这是远程清单，已忽略；需要同名子目录里的本地清单"))
        }
        if (keyUri != null) {
            val keyFile = File(parent, keyUri.stripQueryAndFragment())
            if (!keyFile.isFile) {
                return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "缺少 key 文件: ${keyUri.stripQueryAndFragment()}"))
            }
            if (keyFile.length() != AES_128_KEY_BYTES) {
                return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "key 文件不是 16 字节: ${keyFile.name}"))
            }
        }

        val segmentFiles = lines
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .filterNot { it.isNetworkReference() }
            .map { File(parent, it.stripQueryAndFragment()) }
        if (segmentFiles.isEmpty()) {
            return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "没有本地 ts 分片；可能仍是远程清单"))
        }
        val missingSegment = segmentFiles.firstOrNull { !it.isFile }
        if (missingSegment != null) {
            return ParsedPlaylist.Issue(TsScanIssue(playlist.absolutePath, "缺少分片: ${missingSegment.name}"))
        }

        val keySize = keyUri
            ?.let { File(parent, it.stripQueryAndFragment()) }
            ?.length()
            ?: 0L
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
        val totalSizeBytes = segmentFiles.sumOf { it.length() } + keySize + playlist.length()
        val name = playlist.nameWithoutExtension
        return ParsedPlaylist.Candidate(
            TsVideoCandidate(
                id = playlist.canonicalPathOrAbsolute(),
                name = name,
                playlistPath = playlist.absolutePath,
                sourcePath = sourceRoot.absolutePath,
                segmentCount = segmentFiles.size,
                durationSeconds = durationSeconds,
                totalSizeBytes = totalSizeBytes,
                modifiedTimeMillis = maxOf(playlist.lastModified(), segmentFiles.maxOfOrNull { it.lastModified() } ?: 0L),
                outputFileName = "${name.sanitizeFileName()}.mp4",
            ),
        )
    }

    private sealed interface ParsedPlaylist {
        data class Candidate(val video: TsVideoCandidate) : ParsedPlaylist
        data class Issue(val issue: TsScanIssue) : ParsedPlaylist
    }

    private fun File.readabilityStatus(path: String): TsSourcePathStatus {
        return when {
            path.isAndroidDataPath() && (!exists() || !canRead()) -> TsSourcePathStatus(
                path = path,
                isReadable = false,
                message = "系统限制，无法读取该目录；请复制到 Movies 或 Download 后再扫描",
            )
            !exists() -> TsSourcePathStatus(path = path, isReadable = false, message = "路径不存在")
            !isDirectory -> TsSourcePathStatus(path = path, isReadable = false, message = "不是文件夹")
            !canRead() -> TsSourcePathStatus(path = path, isReadable = false, message = "无法读取")
            else -> TsSourcePathStatus(path = path, isReadable = true, message = "可读取")
        }
    }

    private fun String.isAndroidDataPath(): Boolean {
        return replace('\\', '/').lowercase(Locale.US).contains("/android/data/")
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

    private fun File.canonicalPathOrAbsolute(): String {
        return runCatching { canonicalPath }.getOrDefault(absolutePath)
    }

    private companion object {
        const val AES_128_KEY_BYTES = 16L
        const val MAX_ISSUES = 20
        val URI_ATTRIBUTE = Regex("""URI="([^"]+)"""")
    }
}

private fun Double.roundForKey(): String {
    return "%.3f".format(Locale.US, this)
}
