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
}

enum class MediaSortField {
    Name,
    ModifiedTime,
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
        }
        return items.sortedWith(comparator.oriented(direction))
    }

    private fun <T> Comparator<T>.oriented(direction: SortDirection): Comparator<T> {
        return when (direction) {
            SortDirection.Ascending -> this
            SortDirection.Descending -> reversed()
        }
    }
}
