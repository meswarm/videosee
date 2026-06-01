package app.videosee.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionSearchTest {
    @Test
    fun filters_collections_by_contiguous_name_text_case_insensitively() {
        val collections = listOf(
            folder(id = "1", name = "Cxyy Family"),
            folder(id = "2", name = "camera"),
            folder(id = "3", name = "my-cxyy-video"),
            folder(id = "4", name = "cx yy split"),
        )

        val filtered = CollectionSearch.filterByName(collections, "cxyy")

        assertEquals(listOf("Cxyy Family", "my-cxyy-video"), filtered.map { it.name })
    }

    private fun folder(id: String, name: String): MediaFolder {
        return MediaFolder(
            id = id,
            name = name,
            count = 0,
            previewUri = "",
            newestDateModifiedSeconds = 0,
            items = emptyList(),
        )
    }
}
