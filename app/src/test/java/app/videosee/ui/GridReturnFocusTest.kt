package app.videosee.ui

import app.videosee.domain.MediaItem
import app.videosee.domain.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class GridReturnFocusTest {
    @Test
    fun returns_index_for_matching_uri() {
        val items = listOf(
            item(uri = "content://media/1"),
            item(uri = "content://media/2"),
            item(uri = "content://media/3"),
        )

        assertEquals(1, GridReturnFocus.targetIndex(items, "content://media/2"))
    }

    @Test
    fun returns_null_when_target_is_missing_or_blank() {
        val items = listOf(item(uri = "content://media/1"))

        assertEquals(null, GridReturnFocus.targetIndex(items, null))
        assertEquals(null, GridReturnFocus.targetIndex(items, ""))
        assertEquals(null, GridReturnFocus.targetIndex(items, "content://media/missing"))
    }

    @Test
    fun scrolls_to_row_that_keeps_target_near_center() {
        assertEquals(12, GridReturnFocus.centeredScrollIndex(targetIndex = 24, columnCount = 4, visibleRowCount = 7))
    }

    @Test
    fun centered_scroll_index_never_goes_below_first_item() {
        assertEquals(0, GridReturnFocus.centeredScrollIndex(targetIndex = 3, columnCount = 4, visibleRowCount = 7))
    }

    private fun item(uri: String): MediaItem {
        return MediaItem(
            id = uri.substringAfterLast('/').toLongOrNull() ?: 0L,
            uri = uri,
            displayName = "$uri.mp4",
            bucketId = "bucket",
            bucketName = "Bucket",
            mediaType = MediaType.Video,
            dateModifiedSeconds = 0,
            durationMillis = 1000,
        )
    }
}
