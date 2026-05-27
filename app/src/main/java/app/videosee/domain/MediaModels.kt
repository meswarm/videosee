package app.videosee.domain

enum class MediaType {
    Image,
    Video,
}

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val bucketId: String,
    val bucketName: String,
    val mediaType: MediaType,
    val dateModifiedSeconds: Long,
    val durationMillis: Long?,
    val widthPixels: Int? = null,
    val heightPixels: Int? = null,
) {
    val displayAspectRatio: Float?
        get() {
            val width = widthPixels ?: return null
            val height = heightPixels ?: return null
            return if (width > 0 && height > 0) width.toFloat() / height.toFloat() else null
        }
}

data class MediaFolder(
    val id: String,
    val name: String,
    val count: Int,
    val previewUri: String,
    val newestDateModifiedSeconds: Long,
    val items: List<MediaItem>,
)
