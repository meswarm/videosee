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
    fun groups_rule_matching_media_by_author_id() {
        val items = listOf(
            mediaItem(
                id = 1,
                displayName = "xiaoxinmaila94_1778085537_7636819227521142267.mp4",
                dateModifiedSeconds = 10,
            ),
            mediaItem(
                id = 2,
                displayName = "xiaoxinmaila94_1778223655_7637412444436490170_001.jpg",
                dateModifiedSeconds = 30,
                mediaType = MediaType.Image,
            ),
            mediaItem(
                id = 3,
                displayName = "3x2a58a7j538qsy_176691377787_3xu3jys3tignesq.mp4",
                dateModifiedSeconds = 20,
            ),
        )

        val authors = MediaOrganizer.groupByAuthor(items)

        assertEquals(listOf("author:xiaoxinmaila94", "author:3x2a58a7j538qsy"), authors.map { it.id })
        assertEquals("xiaoxinmaila94", authors[0].name)
        assertEquals(2, authors[0].count)
        assertEquals("content://media/2", authors[0].previewUri)
        assertEquals(listOf(30L, 10L), authors[0].items.map { it.dateModifiedSeconds })
    }

    @Test
    fun ignores_media_without_two_underscores_when_grouping_by_author() {
        val authors = MediaOrganizer.groupByAuthor(
            listOf(
                mediaItem(id = 1, displayName = "normal.mp4", dateModifiedSeconds = 10),
                mediaItem(id = 2, displayName = "author_123.mp4", dateModifiedSeconds = 20),
                mediaItem(id = 3, displayName = "_123_456.mp4", dateModifiedSeconds = 30),
                mediaItem(id = 4, displayName = "author__456.mp4", dateModifiedSeconds = 40),
            ),
        )

        assertEquals(emptyList<MediaFolder>(), authors)
    }

    @Test
    fun groups_media_by_tag_names_and_sorts_tags_by_newest_item() {
        val items = listOf(
            mediaItem(id = 1, displayName = "river-old.mp4", dateModifiedSeconds = 10),
            mediaItem(id = 2, displayName = "pet.mp4", dateModifiedSeconds = 30),
            mediaItem(id = 3, displayName = "river-new.jpg", dateModifiedSeconds = 20, mediaType = MediaType.Image),
        )
        val tags = MediaOrganizer.groupByTag(
            items = items,
            tagNames = listOf("河流", "宠物", "空标签"),
            mediaTags = mapOf(
                "content://media/1" to setOf("河流"),
                "content://media/2" to setOf("宠物"),
                "content://media/3" to setOf("河流"),
            ),
        )

        assertEquals(listOf("tag:宠物", "tag:河流", "tag:空标签"), tags.map { it.id })
        assertEquals(1, tags[0].count)
        assertEquals(2, tags[1].count)
        assertEquals(0, tags[2].count)
        assertEquals(listOf(20L, 10L), tags[1].items.map { it.dateModifiedSeconds })
    }

    @Test
    fun filters_media_by_tag_intersection() {
        val items = listOf(
            mediaItem(id = 1, displayName = "river-family-old.mp4", dateModifiedSeconds = 10),
            mediaItem(id = 2, displayName = "river-only.mp4", dateModifiedSeconds = 30),
            mediaItem(id = 3, displayName = "river-family-new.jpg", dateModifiedSeconds = 20, mediaType = MediaType.Image),
            mediaItem(id = 4, displayName = "family-only.mp4", dateModifiedSeconds = 40),
        )

        val folder = MediaOrganizer.groupByTagIntersection(
            items = items,
            tagNames = listOf("风景", "家人"),
            mediaTags = mapOf(
                "content://media/1" to setOf("风景", "家人"),
                "content://media/2" to setOf("风景"),
                "content://media/3" to setOf("风景", "家人", "宠物"),
                "content://media/4" to setOf("家人"),
            ),
        )

        assertEquals("风景 + 家人", folder?.name)
        assertEquals(2, folder?.count)
        assertEquals(listOf("content://media/3", "content://media/1"), folder?.items?.map { it.uri })
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

    private fun mediaItem(
        id: Long,
        displayName: String,
        dateModifiedSeconds: Long,
        mediaType: MediaType = MediaType.Video,
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = "content://media/$id",
            displayName = displayName,
            bucketId = "downloads",
            bucketName = "Downloads",
            mediaType = mediaType,
            dateModifiedSeconds = dateModifiedSeconds,
            durationMillis = if (mediaType == MediaType.Video) 5_000 else null,
        )
    }
}
