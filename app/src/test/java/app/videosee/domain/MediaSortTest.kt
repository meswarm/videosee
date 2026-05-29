package app.videosee.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSortTest {
    @Test
    fun sorts_collections_by_name_count_and_modified_time() {
        val collections = listOf(
            folder(id = "b", name = "Beta", count = 2, newestDateModifiedSeconds = 20),
            folder(id = "a", name = "Alpha", count = 5, newestDateModifiedSeconds = 10),
            folder(id = "c", name = "Camera", count = 1, newestDateModifiedSeconds = 30),
        )

        assertEquals(
            listOf("Alpha", "Beta", "Camera"),
            MediaSort.sortCollections(collections, CollectionSortField.Name, SortDirection.Ascending).map { it.name },
        )
        assertEquals(
            listOf("Alpha", "Beta", "Camera"),
            MediaSort.sortCollections(collections, CollectionSortField.Count, SortDirection.Descending).map { it.name },
        )
        assertEquals(
            listOf("Camera", "Beta", "Alpha"),
            MediaSort.sortCollections(collections, CollectionSortField.ModifiedTime, SortDirection.Descending).map { it.name },
        )
    }

    @Test
    fun sorts_media_by_name_and_modified_time() {
        val items = listOf(
            item(id = 1, displayName = "b.mp4", dateModifiedSeconds = 20),
            item(id = 2, displayName = "a.mp4", dateModifiedSeconds = 30),
            item(id = 3, displayName = "c.mp4", dateModifiedSeconds = 10),
        )

        assertEquals(
            listOf("a.mp4", "b.mp4", "c.mp4"),
            MediaSort.sortItems(items, MediaSortField.Name, SortDirection.Ascending).map { it.displayName },
        )
        assertEquals(
            listOf("a.mp4", "b.mp4", "c.mp4"),
            MediaSort.sortItems(items, MediaSortField.ModifiedTime, SortDirection.Descending).map { it.displayName },
        )
    }

    private fun folder(
        id: String,
        name: String,
        count: Int,
        newestDateModifiedSeconds: Long,
    ): MediaFolder {
        return MediaFolder(
            id = id,
            name = name,
            count = count,
            previewUri = "content://media/$id",
            newestDateModifiedSeconds = newestDateModifiedSeconds,
            items = emptyList(),
        )
    }

    private fun item(
        id: Long,
        displayName: String,
        dateModifiedSeconds: Long,
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = "content://media/$id",
            displayName = displayName,
            bucketId = "bucket",
            bucketName = "Bucket",
            mediaType = MediaType.Video,
            dateModifiedSeconds = dateModifiedSeconds,
            durationMillis = 1_000,
        )
    }
}
