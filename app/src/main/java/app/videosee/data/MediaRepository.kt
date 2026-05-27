package app.videosee.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import app.videosee.domain.MediaFolder
import app.videosee.domain.MediaItem
import app.videosee.domain.MediaOrganizer
import app.videosee.domain.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {
    suspend fun loadFolders(): List<MediaFolder> = withContext(Dispatchers.IO) {
        val images = runCatching { loadImages() }.getOrDefault(emptyList())
        val videos = runCatching { loadVideos() }.getOrDefault(emptyList())
        MediaOrganizer.groupByFolder(images + videos)
    }

    private fun loadImages(): List<MediaItem> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        return context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            displayName = cursor.getString(nameColumn).orEmpty(),
                            bucketId = cursor.getString(bucketIdColumn).orEmpty(),
                            bucketName = cursor.getString(bucketNameColumn).orEmpty(),
                            mediaType = MediaType.Image,
                            dateModifiedSeconds = cursor.getLong(modifiedColumn),
                            durationMillis = null,
                            widthPixels = cursor.getInt(widthColumn).takeIf { it > 0 },
                            heightPixels = cursor.getInt(heightColumn).takeIf { it > 0 },
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    private fun loadVideos(): List<MediaItem> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )

        return context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            displayName = cursor.getString(nameColumn).orEmpty(),
                            bucketId = cursor.getString(bucketIdColumn).orEmpty(),
                            bucketName = cursor.getString(bucketNameColumn).orEmpty(),
                            mediaType = MediaType.Video,
                            dateModifiedSeconds = cursor.getLong(modifiedColumn),
                            durationMillis = cursor.getLong(durationColumn).takeIf { it > 0 },
                            widthPixels = cursor.getInt(widthColumn).takeIf { it > 0 },
                            heightPixels = cursor.getInt(heightColumn).takeIf { it > 0 },
                        ),
                    )
                }
            }
        } ?: emptyList()
    }
}
