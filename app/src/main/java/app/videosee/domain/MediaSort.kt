package app.videosee.domain

import java.util.Locale

enum class SortDirection {
    Ascending,
    Descending,
}

enum class CollectionSortField {
    Name,
    Count,
    ModifiedTime,
    FavoriteLevel,
}

enum class MediaSortField {
    Name,
    ModifiedTime,
    FavoriteLevel,
}

object MediaSort {
    fun sortCollections(
        collections: List<MediaFolder>,
        field: CollectionSortField,
        direction: SortDirection,
    ): List<MediaFolder> {
        val comparator = when (field) {
            CollectionSortField.Name -> compareBy<MediaFolder> { it.name.lowercase(Locale.ROOT) }
            CollectionSortField.Count -> compareBy { it.count }
            CollectionSortField.ModifiedTime -> compareBy { it.newestDateModifiedSeconds }
            CollectionSortField.FavoriteLevel -> compareBy<MediaFolder> { it.favoriteLevel }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        }
        return collections.sortedWith(comparator.oriented(direction))
    }

    fun sortItems(
        items: List<MediaItem>,
        field: MediaSortField,
        direction: SortDirection,
    ): List<MediaItem> {
        val comparator = when (field) {
            MediaSortField.Name -> compareBy<MediaItem> { it.displayName.lowercase(Locale.ROOT) }
            MediaSortField.ModifiedTime -> compareBy { it.dateModifiedSeconds }
            MediaSortField.FavoriteLevel -> compareBy<MediaItem> { it.favoriteLevel }
                .thenBy { it.displayName.lowercase(Locale.ROOT) }
        }
        return items.sortedWith(comparator.oriented(direction))
    }

    fun sortItemsByStableUriOrder(
        items: List<MediaItem>,
        stableUriOrder: List<String>,
        fallbackField: MediaSortField,
        fallbackDirection: SortDirection,
    ): List<MediaItem> {
        if (stableUriOrder.isEmpty()) {
            return sortItems(items, fallbackField, fallbackDirection)
        }
        val orderByUri = stableUriOrder.withIndex().associate { it.value to it.index }
        val fallbackItems = sortItems(items, fallbackField, fallbackDirection)
        return fallbackItems.sortedWith(
            compareBy<MediaItem> { orderByUri[it.uri] ?: Int.MAX_VALUE },
        )
    }

    private fun <T> Comparator<T>.oriented(direction: SortDirection): Comparator<T> {
        return when (direction) {
            SortDirection.Ascending -> this
            SortDirection.Descending -> reversed()
        }
    }
}
