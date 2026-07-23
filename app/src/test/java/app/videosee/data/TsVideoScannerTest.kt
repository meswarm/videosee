package app.videosee.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class TsVideoScannerTest {
    @Test
    fun scanFindsLocalEncryptedHlsVideosAndIgnoresRemoteOuterPlaylists() {
        val root = createTempDirectory(prefix = "ts-scan").toFile()
        try {
            createHlsVideo(root, "875ed5061f9d73341767d4b6516ee768", segmentCount = 2)
            createHlsVideo(root, "aa9c3adf7cf1d5c765da2d84bc28cd58", segmentCount = 3)

            val result = TsVideoScanner().scan(listOf(root.absolutePath))

            assertEquals(listOf("aa9c3adf7cf1d5c765da2d84bc28cd58.mp4", "875ed5061f9d73341767d4b6516ee768.mp4").sorted(), result.videos.map { it.outputFileName }.sorted())
            assertEquals(2, result.videos.size)
            assertEquals(1, result.paths.size)
            assertTrue(result.paths.single().isReadable)
            assertTrue(result.videos.all { it.segmentCount >= 2 })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanSkipsAlreadyConvertedRecordKeys() {
        val root = createTempDirectory(prefix = "ts-scan").toFile()
        try {
            createHlsVideo(root, "video-one", segmentCount = 2)

            val firstScan = TsVideoScanner().scan(listOf(root.absolutePath))
            val result = TsVideoScanner().scan(
                sourcePaths = listOf(root.absolutePath),
                downloadedKeys = setOf(firstScan.videos.single().recordKey),
            )

            assertTrue(result.videos.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanReportsMissingSegmentsAsNoCandidate() {
        val root = createTempDirectory(prefix = "ts-scan").toFile()
        try {
            val directory = File(root, "broken").apply { mkdirs() }
            File(directory, "tsKey").writeBytes(ByteArray(16))
            File(directory, "broken.m3u8").writeText(
                """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="tsKey",IV=0x00000000000000000000000000000000
                #EXTINF:5.0,
                missing.ts
                #EXT-X-ENDLIST
                """.trimIndent(),
            )

            val result = TsVideoScanner().scan(listOf(root.absolutePath))

            assertTrue(result.videos.isEmpty())
            assertTrue(result.paths.single().isReadable)
            assertTrue(result.issues.any { it.reason.contains("缺少分片") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanAcceptsLocalKeyUriWithQueryString() {
        val root = createTempDirectory(prefix = "ts-scan").toFile()
        try {
            val directory = File(root, "video-query").apply { mkdirs() }
            File(directory, "tsKey").writeBytes(ByteArray(16))
            File(directory, "video-query0.ts").writeBytes(ByteArray(128))
            File(directory, "video-query.m3u8").writeText(
                """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="tsKey?auth=1",IV=0x00000000000000000000000000000000
                #EXTINF:5.0,
                video-query0.ts
                #EXT-X-ENDLIST
                """.trimIndent(),
            )

            val result = TsVideoScanner().scan(listOf(root.absolutePath))

            assertEquals(1, result.videos.size)
            assertEquals("video-query.mp4", result.videos.single().outputFileName)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanUsesSiblingLocalPlaylistWhenOuterPlaylistIsRemote() {
        val root = createTempDirectory(prefix = "ts-scan").toFile()
        try {
            createHlsVideo(root, "outer-video", segmentCount = 2)

            val result = TsVideoScanner().scan(listOf(root.absolutePath))

            assertEquals(1, result.videos.size)
            assertEquals("outer-video.mp4", result.videos.single().outputFileName)
            assertTrue(result.issues.any { it.reason.contains("远程清单") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanReportsRestrictedAndroidDataPathWhenUnreadable() {
        val result = TsVideoScanner().scan(listOf("/storage/emulated/0/Android/data/example.app/files/Movies"))

        assertFalse(result.paths.single().isReadable)
        assertEquals("系统限制，无法读取该目录；请复制到 Movies 或 Download 后再扫描", result.paths.single().message)
    }

    private fun createHlsVideo(root: File, id: String, segmentCount: Int) {
        File(root, "$id.m3u8").writeText(
            """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="https://example.com/$id/crypt.key"
            #EXTINF:5.0,
            https://example.com/$id/${id}0.ts
            #EXT-X-ENDLIST
            """.trimIndent(),
        )
        val directory = File(root, id).apply { mkdirs() }
        File(directory, "tsKey").writeBytes(ByteArray(16))
        val segments = buildString {
            repeat(segmentCount) { index ->
                appendLine("#EXTINF:5.0,")
                appendLine("${id}${index}.ts")
                File(directory, "${id}${index}.ts").writeBytes(ByteArray(128))
            }
        }
        File(directory, "$id.m3u8").writeText(
            """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-KEY:METHOD=AES-128,URI="tsKey",IV=0x00000000000000000000000000000000
            $segments#EXT-X-ENDLIST
            """.trimIndent(),
        )
    }
}
