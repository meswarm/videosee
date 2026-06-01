package app.videosee.domain

object CollectionSearch {
    fun filterByName(collections: List<MediaFolder>, query: String): List<MediaFolder> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return collections
        return collections.filter { collection ->
            collection.name.contains(normalizedQuery, ignoreCase = true)
        }
    }
}
