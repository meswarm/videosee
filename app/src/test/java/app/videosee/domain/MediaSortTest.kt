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
    fun sorts_collections_by_favorite_level() {
        val collections = listOf(
            folder(id = "a", name = "Alpha", count = 2, newestDateModifiedSeconds = 20, favoriteLevel = 1),
            folder(id = "c", name = "Camera", count = 1, newestDateModifiedSeconds = 30, favoriteLevel = 3),
            folder(id = "b", name = "Beta", count = 5, newestDateModifiedSeconds = 10, favoriteLevel = 2),
        )

        assertEquals(
            listOf("Camera", "Beta", "Alpha"),
            MediaSort.sortCollections(collections, CollectionSortField.FavoriteLevel, SortDirection.Descending).map { it.name },
        )
        assertEquals(
            listOf("Alpha", "Beta", "Camera"),
            MediaSort.sortCollections(collections, CollectionSortField.FavoriteLevel, SortDirection.Ascending).map { it.name },
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

    @Test
    fun sorts_media_by_favorite_level() {
        val items = listOf(
            item(id = 1, displayName = "a.mp4", dateModifiedSeconds = 20, favoriteLevel = 1),
            item(id = 3, displayName = "c.mp4", dateModifiedSeconds = 30, favoriteLevel = 3),
            item(id = 2, displayName = "b.mp4", dateModifiedSeconds = 10, favoriteLevel = 2),
        )

        assertEquals(
            listOf("c.mp4", "b.mp4", "a.mp4"),
            MediaSort.sortItems(items, MediaSortField.FavoriteLevel, SortDirection.Descending).map { it.displayName },
        )
        assertEquals(
            listOf("a.mp4", "b.mp4", "c.mp4"),
            MediaSort.sortItems(items, MediaSortField.FavoriteLevel, SortDirection.Ascending).map { it.displayName },
        )
    }

    @Test
    fun keeps_media_in_stable_uri_order_after_favorite_levels_change() {
        val items = listOf(
            item(id = 1, displayName = "a.mp4", dateModifiedSeconds = 20, favoriteLevel = 3),
            item(id = 3, displayName = "c.mp4", dateModifiedSeconds = 30, favoriteLevel = 1),
            item(id = 2, displayName = "b.mp4", dateModifiedSeconds = 10, favoriteLevel = 2),
        )

        assertEquals(
            listOf("c.mp4", "b.mp4", "a.mp4"),
            MediaSort.sortItemsByStableUriOrder(
                items = items,
                stableUriOrder = listOf("content://media/3", "content://media/2", "content://media/1"),
                fallbackField = MediaSortField.FavoriteLevel,
                fallbackDirection = SortDirection.Descending,
            ).map { it.displayName },
        )
    }

    private fun folder(
        id: String,
        name: String,
        count: Int,
        newestDateModifiedSeconds: Long,
        favoriteLevel: Int = 0,
    ): MediaFolder {
        return MediaFolder(
            id = id,
            name = name,
            count = count,
            previewUri = "content://media/$id",
            newestDateModifiedSeconds = newestDateModifiedSeconds,
            favoriteLevel = favoriteLevel,
            items = emptyList(),
        )
    }

    private fun item(
        id: Long,
        displayName: String,
        dateModifiedSeconds: Long,
        favoriteLevel: Int = 0,
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
            favoriteLevel = favoriteLevel,
        )
    }
}
