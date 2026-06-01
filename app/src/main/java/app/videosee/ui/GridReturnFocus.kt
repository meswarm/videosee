package app.videosee.ui

import app.videosee.domain.MediaItem

object GridReturnFocus {
    fun targetIndex(items: List<MediaItem>, targetUri: String?): Int? {
        val uri = targetUri?.takeIf { it.isNotBlank() } ?: return null
        return items.indexOfFirst { it.uri == uri }.takeIf { it >= 0 }
    }

    fun centeredScrollIndex(targetIndex: Int, columnCount: Int, visibleRowCount: Int): Int {
        val safeColumnCount = columnCount.coerceAtLeast(1)
        val safeVisibleRowCount = visibleRowCount.coerceAtLeast(1)
        val targetRow = targetIndex.coerceAtLeast(0) / safeColumnCount
        val firstVisibleRow = (targetRow - safeVisibleRowCount / 2).coerceAtLeast(0)
        return firstVisibleRow * safeColumnCount
    }
}
