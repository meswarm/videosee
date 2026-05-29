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

    fun groupByAuthor(items: List<MediaItem>): List<MediaFolder> {
        return items
            .mapNotNull { item ->
                val authorId = item.authorIdFromRuleName() ?: return@mapNotNull null
                authorId to item
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (authorId, authorItems) ->
                val sortedItems = authorItems.sortedByDescending { it.dateModifiedSeconds }
                val newest = sortedItems.first()
                MediaFolder(
                    id = "$AUTHOR_PREFIX$authorId",
                    name = authorId,
                    count = sortedItems.size,
                    previewUri = newest.uri,
                    newestDateModifiedSeconds = newest.dateModifiedSeconds,
                    items = sortedItems,
                )
            }
            .sortedByDescending { it.newestDateModifiedSeconds }
    }

    private fun MediaItem.authorIdFromRuleName(): String? {
        val parts = displayName.substringBeforeLast('.').split('_')
        if (parts.size < 3) return null
        val authorId = parts[0]
        val createTime = parts[1]
        val contentId = parts[2]
        return authorId.takeIf {
            it.isNotBlank() && createTime.isNotBlank() && contentId.isNotBlank()
        }
    }

    private const val AUTHOR_PREFIX = "author:"
    private const val UNKNOWN_ID = "unknown"
    private const val UNKNOWN_NAME = "Unknown"
}
