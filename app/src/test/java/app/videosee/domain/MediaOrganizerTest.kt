package app.videosee.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaOrganizerTest {
    @Test
    fun groups_media_by_bucket_and_sorts_folders_by_newest_item() {
        val items = listOf(
            MediaItem(
                id = 1,
                uri = "content://media/1",
                displayName = "old.jpg",
                bucketId = "camera",
                bucketName = "Camera",
                mediaType = MediaType.Image,
                dateModifiedSeconds = 10,
                durationMillis = null,
            ),
            MediaItem(
                id = 2,
                uri = "content://media/2",
                displayName = "clip.mp4",
                bucketId = "downloads",
                bucketName = "Downloads",
                mediaType = MediaType.Video,
                dateModifiedSeconds = 30,
                durationMillis = 5_000,
            ),
            MediaItem(
                id = 3,
                uri = "content://media/3",
                displayName = "new.jpg",
                bucketId = "camera",
                bucketName = "Camera",
                mediaType = MediaType.Image,
                dateModifiedSeconds = 20,
                durationMillis = null,
            ),
        )

        val folders = MediaOrganizer.groupByFolder(items)

        assertEquals(listOf("downloads", "camera"), folders.map { it.id })
        assertEquals(1, folders[0].count)
        assertEquals("content://media/2", folders[0].previewUri)
        assertEquals(listOf(20L, 10L), folders[1].items.map { it.dateModifiedSeconds })
    }

    @Test
    fun uses_unknown_folder_when_bucket_metadata_is_missing() {
        val folders = MediaOrganizer.groupByFolder(
            listOf(
                MediaItem(
                    id = 9,
                    uri = "content://media/9",
                    displayName = "loose.png",
                    bucketId = "",
                    bucketName = "",
                    mediaType = MediaType.Image,
                    dateModifiedSeconds = 4,
                    durationMillis = null,
                ),
            ),
        )

        assertEquals("unknown", folders.single().id)
        assertEquals("Unknown", folders.single().name)
    }

    @Test
    fun exposes_display_aspect_ratio_when_dimensions_are_available() {
        val item = MediaItem(
            id = 10,
            uri = "content://media/10",
            displayName = "portrait.mp4",
            bucketId = "videos",
            bucketName = "Videos",
            mediaType = MediaType.Video,
            dateModifiedSeconds = 5,
            durationMillis = 8_000,
            widthPixels = 720,
            heightPixels = 1280,
        )

        assertEquals(0.5625f, item.displayAspectRatio!!, 0.0001f)
    }
}
