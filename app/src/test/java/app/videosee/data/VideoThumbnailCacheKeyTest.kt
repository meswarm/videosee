package app.videosee.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoThumbnailCacheKeyTest {
    @Test
    fun creates_stable_filename_for_same_media_fingerprint() {
        val first = VideoThumbnailCacheKey.filename(
            uri = "content://media/external/video/media/42",
            dateModifiedSeconds = 100,
            durationMillis = 2000,
        )
        val second = VideoThumbnailCacheKey.filename(
            uri = "content://media/external/video/media/42",
            dateModifiedSeconds = 100,
            durationMillis = 2000,
        )

        assertEquals(first, second)
        assertTrue(first.endsWith(".jpg"))
        assertTrue(first.removeSuffix(".jpg").all { it in 'a'..'f' || it in '0'..'9' })
    }

    @Test
    fun changes_filename_when_media_changes() {
        val original = VideoThumbnailCacheKey.filename(
            uri = "content://media/external/video/media/42",
            dateModifiedSeconds = 100,
            durationMillis = 2000,
        )
        val changed = VideoThumbnailCacheKey.filename(
            uri = "content://media/external/video/media/42",
            dateModifiedSeconds = 101,
            durationMillis = 2000,
        )

        assertNotEquals(original, changed)
    }
}
