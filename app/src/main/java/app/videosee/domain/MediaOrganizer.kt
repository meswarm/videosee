package app.videosee.domain

object MediaOrganizer {
    fun groupByFolder(items: List<MediaItem>): List<MediaFolder> {
        return items
            .groupBy { item -> item.bucketId.ifBlank { UNKNOWN_ID } }
            .map { (bucketId, folderItems) ->
                val sortedItems = folderItems.sortedByDescending { it.dateModifiedSeconds }
                val newest = sortedItems.first()
                MediaFolder(
                    id = bucketId,
                    name = newest.bucketName.ifBlank { UNKNOWN_NAME },
                    count = sortedItems.size,
                    previewUri = newest.uri,
                    newestDateModifiedSeconds = newest.dateModifiedSeconds,
                    items = sortedItems,
                )
            }
            .sortedByDescending { it.newestDateModifiedSeconds }
    }

    private const val UNKNOWN_ID = "unknown"
    private const val UNKNOWN_NAME = "Unknown"
}
