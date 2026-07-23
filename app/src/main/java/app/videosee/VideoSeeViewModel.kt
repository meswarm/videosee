package app.videosee

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.videosee.data.MediaRepository
import app.videosee.data.MobileSyncRepository
import app.videosee.data.SyncPendingFile
import app.videosee.data.TsConvertRepository
import app.videosee.data.TsConvertSettingsStore
import app.videosee.data.TsScanIssue
import app.videosee.data.TsSourcePathStatus
import app.videosee.data.TsVideoCandidate
import app.videosee.data.VideoThumbnailCacheRepository
import app.videosee.domain.CollectionSearch
import app.videosee.domain.CollectionSortField
import app.videosee.domain.MediaFolder
import app.videosee.domain.MediaItem
import app.videosee.domain.MediaOrganizer
import app.videosee.domain.MediaSort
import app.videosee.domain.MediaSortField
import app.videosee.domain.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class FavoriteFolder(
    val id: String,
    val name: String,
)

data class VideoSegment(
    val startMillis: Long,
    val endMillis: Long,
    val name: String? = null,
)

data class VideoSeeUiState(
    val appTheme: AppTheme = AppTheme.Midnight,
    val folders: List<MediaFolder> = emptyList(),
    val authors: List<MediaFolder> = emptyList(),
    val tags: List<String> = emptyList(),
    val mediaTags: Map<String, Set<String>> = emptyMap(),
    val tagFolders: List<MediaFolder> = emptyList(),
    val favoriteFolders: List<FavoriteFolder> = emptyList(),
    val favoriteFolderMediaUris: Map<String, Set<String>> = emptyMap(),
    val defaultFavoriteFolderId: String? = null,
    val authorFavoriteLevels: Map<String, Int> = emptyMap(),
    val mediaFavoriteLevels: Map<String, Int> = emptyMap(),
    val browserMode: BrowserMode = BrowserMode.Folder,
    val collectionSortField: CollectionSortField = CollectionSortField.ModifiedTime,
    val collectionSortDirection: SortDirection = SortDirection.Descending,
    val mediaSortField: MediaSortField = MediaSortField.ModifiedTime,
    val mediaSortDirection: SortDirection = SortDirection.Descending,
    val favoriteMediaSortUriOrder: List<String> = emptyList(),
    val recentPlaybackUris: List<String> = emptyList(),
    val videoSegmentsByUri: Map<String, List<VideoSegment>> = emptyMap(),
    val playbackMode: PlaybackMode = PlaybackMode.Sequential,
    val shuffleUriOrder: List<String> = emptyList(),
    val collectionSearchQuery: String = "",
    val selectedFolderId: String? = null,
    val selectedAuthorId: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
    val selectedFavoriteFolderId: String? = null,
    val viewerIndex: Int? = null,
    val gridReturnTargetUri: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasPermission: Boolean = false,
    val errorMessage: String? = null,
    val rightPaneMode: RightPaneMode = RightPaneMode.Browser,
    val syncHost: String = "192.168.1.23",
    val syncPort: String = "19827",
    val syncToken: String = "",
    val syncDeviceId: String = "videosee-phone",
    val syncPendingFiles: List<SyncPendingFile> = emptyList(),
    val syncDownloadingIds: Set<String> = emptySet(),
    val syncIsLoading: Boolean = false,
    val syncMessage: String? = null,
    val tsSourcePaths: List<String> = emptyList(),
    val tsNewSourcePath: String = "",
    val tsDownloadDirectory: String = TsConvertSettingsStore.DEFAULT_DOWNLOAD_DIRECTORY,
    val tsPrivateImportDirectory: String = "",
    val tsPathStatuses: List<TsSourcePathStatus> = emptyList(),
    val tsScanIssues: List<TsScanIssue> = emptyList(),
    val tsVideos: List<TsVideoCandidate> = emptyList(),
    val tsConvertingIds: Set<String> = emptySet(),
    val tsDownloadingIds: Set<String> = emptySet(),
    val tsConvertedOutputPaths: Map<String, String> = emptyMap(),
    val tsIsScanning: Boolean = false,
    val tsMessage: String? = null,
    val tsDownloadedCount: Int = 0,
    val tsBatchIsRunning: Boolean = false,
    val tsBatchTotal: Int = 0,
    val tsBatchCompleted: Int = 0,
    val tsBatchCurrentName: String = "",
    val tsBatchFailedCount: Int = 0,
    val appBannerMessage: String? = null,
    val videoThumbnailPaths: Map<String, String> = emptyMap(),
) {
    val selectedFolder: MediaFolder?
        get() = if (selectedFolderId == RECENT_PLAYBACK_COLLECTION_ID) {
            recentPlaybackCollection
        } else {
            folders.firstOrNull { it.id == selectedFolderId } ?: folders.firstOrNull()
        }

    val selectedAuthor: MediaFolder?
        get() = authors.firstOrNull { it.id == selectedAuthorId } ?: authors.firstOrNull()

    val visibleCollections: List<MediaFolder>
        get() {
            val field = collectionSortField.takeIf { browserMode == BrowserMode.Author || it != CollectionSortField.FavoriteLevel }
                ?: CollectionSortField.ModifiedTime
            val collections = when (browserMode) {
                BrowserMode.Folder -> listOf(recentPlaybackCollection) + MediaSort.sortCollections(folders, field, collectionSortDirection)
                BrowserMode.Author -> MediaSort.sortCollections(authors, field, collectionSortDirection)
                BrowserMode.Tag -> MediaSort.sortCollections(tagFolders, field, collectionSortDirection)
                BrowserMode.FavoriteFolder -> MediaSort.sortCollections(favoriteFolderCollections, field, collectionSortDirection)
            }
            return CollectionSearch.filterByName(collections, collectionSearchQuery)
        }

    val selectedCollection: MediaFolder?
        get() = when (browserMode) {
            BrowserMode.Folder -> selectedFolder
            BrowserMode.Author -> selectedAuthor
            BrowserMode.Tag -> selectedTagCollection
            BrowserMode.FavoriteFolder -> selectedFavoriteFolderCollection
        }

    private val selectedTagCollection: MediaFolder?
        get() {
            val effectiveTagIds = selectedTagIds.ifEmpty {
                tagFolders.firstOrNull()?.let { setOf(it.id) }.orEmpty()
            }
            if (effectiveTagIds.isEmpty()) return null
            if (effectiveTagIds.size == 1) {
                return tagFolders.firstOrNull { it.id in effectiveTagIds }
            }
            val selectedTagNames = tagFolders
                .filter { it.id in effectiveTagIds }
                .map { it.name }
            return MediaOrganizer.groupByTagIntersection(
                items = folders.flatMap { it.items }.distinctBy { it.uri },
                tagNames = selectedTagNames,
                mediaTags = mediaTags,
            )
        }

    private val favoriteFolderCollections: List<MediaFolder>
        get() {
            val allItems = folders.flatMap { it.items }.distinctBy { it.uri }
            return favoriteFolders.map { favoriteFolder ->
                val items = allItems.filter { it.uri in favoriteFolderMediaUris[favoriteFolder.id].orEmpty() }
                MediaFolder(
                    id = favoriteFolder.collectionId(),
                    name = favoriteFolder.name,
                    count = items.size,
                    previewUri = items.firstOrNull()?.uri.orEmpty(),
                    newestDateModifiedSeconds = items.maxOfOrNull { it.dateModifiedSeconds } ?: 0L,
                    items = items,
                )
            }
        }

    private val recentPlaybackCollection: MediaFolder
        get() {
            val itemsByUri = folders.flatMap { it.items }.distinctBy { it.uri }.associateBy { it.uri }
            val items = recentPlaybackUris.mapNotNull(itemsByUri::get)
            return MediaFolder(
                id = RECENT_PLAYBACK_COLLECTION_ID,
                name = "最近播放",
                count = items.size,
                previewUri = items.firstOrNull()?.uri.orEmpty(),
                newestDateModifiedSeconds = items.size.toLong(),
                items = items,
            )
        }

    private val selectedFavoriteFolderCollection: MediaFolder?
        get() {
            val selectedId = selectedFavoriteFolderId ?: favoriteFolders.firstOrNull()?.id ?: return null
            return favoriteFolderCollections.firstOrNull { it.id == FAVORITE_FOLDER_PREFIX + selectedId }
        }

    val sequentialSelectedItems: List<MediaItem>
        get() {
            val items = selectedCollection?.items.orEmpty()
            if (selectedCollection?.id == RECENT_PLAYBACK_COLLECTION_ID && mediaSortField == MediaSortField.ModifiedTime) {
                val order = recentPlaybackUris.filter { uri -> items.any { it.uri == uri } }
                val orderedUris = if (mediaSortDirection == SortDirection.Descending) order else order.reversed()
                return MediaSort.sortItemsByStableUriOrder(
                    items = items,
                    stableUriOrder = orderedUris,
                    fallbackField = mediaSortField,
                    fallbackDirection = mediaSortDirection,
                )
            }
            return if (mediaSortField == MediaSortField.FavoriteLevel && favoriteMediaSortUriOrder.isNotEmpty()) {
                MediaSort.sortItemsByStableUriOrder(
                    items = items,
                    stableUriOrder = favoriteMediaSortUriOrder,
                    fallbackField = mediaSortField,
                    fallbackDirection = mediaSortDirection,
                )
            } else {
                MediaSort.sortItems(
                    items = items,
                    field = mediaSortField,
                    direction = mediaSortDirection,
                )
            }
        }

    val selectedItems: List<MediaItem>
        get() {
            val sequentialItems = sequentialSelectedItems
            return if (playbackMode == PlaybackMode.Shuffle && shuffleUriOrder.matchesItems(sequentialItems)) {
                MediaSort.sortItemsByStableUriOrder(
                    items = sequentialItems,
                    stableUriOrder = shuffleUriOrder,
                    fallbackField = mediaSortField,
                    fallbackDirection = mediaSortDirection,
                )
            } else {
                sequentialItems
            }
        }

    val viewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it) }

    val previousViewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it - 1) }

    val nextViewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it + 1) }
}

enum class BrowserMode {
    Folder,
    Author,
    Tag,
    FavoriteFolder,
}

enum class PlaybackMode {
    Sequential,
    Shuffle,
}

enum class RightPaneMode {
    Browser,
    Settings,
    Tags,
    Sync,
    TsConvert,
    Backup,
}

/** Six deliberately restrained palettes: three dark and three light. */
enum class AppTheme(val label: String, val isDark: Boolean) {
    Midnight("午夜蓝", true),
    Graphite("石墨灰", true),
    Forest("深林绿", true),
    Snow("雪白", false),
    Mist("雾紫", false),
    Sand("暖沙", false),
}

class VideoSeeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val syncRepository = MobileSyncRepository(application)
    private val tsConvertRepository = TsConvertRepository(application)
    private val thumbnailCacheRepository = VideoThumbnailCacheRepository(application)
    private val syncSettingsStore = SyncSettingsStore(application)
    private val tsConvertSettingsStore = TsConvertSettingsStore(application)
    private val appearanceStore = AppearanceStore(application)
    private val authorFavoriteStore = FavoriteLevelStore(application, "author_favorite_levels")
    private val mediaFavoriteStore = FavoriteLevelStore(application, "media_favorite_levels")
    private val tagStore = TagDataStore(application)
    private val favoriteFolderStore = FavoriteFolderStore(application)
    private val initialFavoriteFolderData = favoriteFolderStore.load()
    private val recentPlaybackStore = RecentPlaybackStore(application)
    private val videoSegmentStore = VideoSegmentStore(application)
    private var shuffleResetJob: Job? = null
    private var tsBatchJob: Job? = null
    private val thumbnailRequestsInFlight = mutableSetOf<String>()
    private val _uiState = MutableStateFlow(
        VideoSeeUiState(
            appTheme = appearanceStore.theme,
            authorFavoriteLevels = authorFavoriteStore.load(),
            mediaFavoriteLevels = mediaFavoriteStore.load(),
            tags = tagStore.load().tagNames,
            mediaTags = tagStore.load().mediaTags,
            favoriteFolders = initialFavoriteFolderData.folders,
            favoriteFolderMediaUris = initialFavoriteFolderData.mediaUrisByFolderId,
            defaultFavoriteFolderId = initialFavoriteFolderData.defaultFolderId,
            recentPlaybackUris = recentPlaybackStore.load(),
            videoSegmentsByUri = videoSegmentStore.load(),
            syncHost = syncSettingsStore.host,
            syncPort = syncSettingsStore.port,
            syncToken = syncSettingsStore.token,
            syncDeviceId = syncSettingsStore.deviceId,
            tsSourcePaths = tsConvertSettingsStore.sourcePaths,
            tsDownloadDirectory = tsConvertSettingsStore.downloadDirectory,
            tsPrivateImportDirectory = tsConvertRepository.privateImportDirectory().absolutePath,
            tsDownloadedCount = tsConvertSettingsStore.downloadedRecords().size,
        ),
    )
    val uiState: StateFlow<VideoSeeUiState> = _uiState.asStateFlow()

    fun onPermissionChanged(hasPermission: Boolean) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
        if (hasPermission) {
            refresh()
        }
    }

    fun refresh() {
        refreshLibrary(viewerTargetUri = null)
    }

    fun refreshKeepingViewer(viewerTargetUri: String?) {
        refreshLibrary(viewerTargetUri = viewerTargetUri)
    }

    private fun refreshLibrary(viewerTargetUri: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.folders.isEmpty() && it.authors.isEmpty(),
                    isRefreshing = it.folders.isNotEmpty() || it.authors.isNotEmpty(),
                    errorMessage = null,
                )
            }
            runCatching { repository.loadLibrary() }
                .onSuccess { library ->
                    _uiState.update { state ->
                        val selectedFolderId = state.selectedFolderId
                            ?.takeIf { id -> id == RECENT_PLAYBACK_COLLECTION_ID || library.folders.any { it.id == id } }
                            ?: library.folders.firstOrNull()?.id
                        val selectedAuthorId = state.selectedAuthorId
                            ?.takeIf { id -> library.authors.any { it.id == id } }
                            ?: library.authors.firstOrNull()?.id
                        val browserMode = when {
                            state.browserMode == BrowserMode.Author && library.authors.isEmpty() -> BrowserMode.Folder
                            else -> state.browserMode
                        }
                        val folders = library.folders.withMediaFavoriteLevels(state.mediaFavoriteLevels)
                        val authors = library.authors
                            .withAuthorFavoriteLevels(state.authorFavoriteLevels)
                            .withMediaFavoriteLevels(state.mediaFavoriteLevels)
                        val tagFolders = MediaOrganizer.groupByTag(
                            items = (library.folders.flatMap { it.items }).distinctBy { it.uri }
                                .map { item ->
                                    item.copy(favoriteLevel = state.mediaFavoriteLevels[item.uri]?.coerceIn(0, MAX_FAVORITE_LEVEL) ?: 0)
                                },
                            tagNames = state.tags,
                            mediaTags = state.mediaTags,
                        )
                        val selectedTagIds = state.selectedTagIds
                            .filterTo(mutableSetOf()) { id -> tagFolders.any { it.id == id } }
                            .ifEmpty { tagFolders.firstOrNull()?.let { mutableSetOf(it.id) } ?: mutableSetOf() }
                        val refreshedState = state.copy(
                            folders = folders,
                            authors = authors,
                            tagFolders = tagFolders,
                            browserMode = browserMode,
                            selectedFolderId = selectedFolderId,
                            selectedAuthorId = selectedAuthorId,
                            selectedTagIds = selectedTagIds,
                            viewerIndex = null,
                            isLoading = false,
                            isRefreshing = false,
                        )
                        val viewerIndex = viewerTargetUri
                            ?.let { targetUri -> refreshedState.selectedItems.indexOfFirst { it.uri == targetUri } }
                            ?.takeIf { it >= 0 }
                        refreshedState.copy(viewerIndex = viewerIndex)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Failed to load media",
                        )
                    }
                }
        }
    }

    fun selectFolder(folderId: String) {
        _uiState.update { state ->
            state.copy(
                selectedFolderId = folderId,
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun selectAuthor(authorId: String) {
        _uiState.update { state ->
            state.copy(
                selectedAuthorId = authorId,
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun selectTag(tagId: String) {
        _uiState.update { state ->
            val nextSelectedTagIds = when {
                tagId in state.selectedTagIds && state.selectedTagIds.size > 1 -> state.selectedTagIds - tagId
                tagId in state.selectedTagIds -> state.selectedTagIds
                state.tagFolders.any { it.id == tagId } -> state.selectedTagIds + tagId
                else -> state.selectedTagIds
            }
            state.copy(
                selectedTagIds = nextSelectedTagIds,
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun setAuthorFavoriteLevel(authorId: String, favoriteLevel: Int) {
        val normalizedLevel = favoriteLevel.coerceIn(0, MAX_FAVORITE_LEVEL)
        _uiState.update { state ->
            val nextFavoriteLevels = if (normalizedLevel == 0) {
                state.authorFavoriteLevels - authorId
            } else {
                state.authorFavoriteLevels + (authorId to normalizedLevel)
            }
            authorFavoriteStore.save(nextFavoriteLevels)
            state.copy(
                authorFavoriteLevels = nextFavoriteLevels,
                authors = state.authors.withAuthorFavoriteLevels(nextFavoriteLevels),
            )
        }
    }

    fun setMediaFavoriteLevel(mediaUri: String, favoriteLevel: Int) {
        val normalizedLevel = favoriteLevel.coerceIn(0, MAX_FAVORITE_LEVEL)
        _uiState.update { state ->
            val nextFavoriteLevels = if (normalizedLevel == 0) {
                state.mediaFavoriteLevels - mediaUri
            } else {
                state.mediaFavoriteLevels + (mediaUri to normalizedLevel)
            }
            mediaFavoriteStore.save(nextFavoriteLevels)
            state.copy(
                mediaFavoriteLevels = nextFavoriteLevels,
                folders = state.folders.withMediaFavoriteLevels(nextFavoriteLevels),
                authors = state.authors.withMediaFavoriteLevels(nextFavoriteLevels),
            ).withTagFolders()
        }
    }

    fun addTag(name: String) {
        val normalizedName = name.trim().takeIf { it.isNotBlank() } ?: return
        _uiState.update { state ->
            if (normalizedName in state.tags) return@update state
            val nextTags = state.tags + normalizedName
            tagStore.save(TagData(nextTags, state.mediaTags))
            state.copy(tags = nextTags).withTagFolders()
        }
    }

    fun deleteTag(name: String) {
        _uiState.update { state ->
            val nextTags = state.tags - name
            val nextMediaTags = state.mediaTags
                .mapValues { (_, tags) -> tags - name }
                .filterValues { it.isNotEmpty() }
            tagStore.save(TagData(nextTags, nextMediaTags))
            state.copy(tags = nextTags, mediaTags = nextMediaTags).withTagFolders()
        }
    }

    fun toggleMediaTag(mediaUri: String, tagName: String) {
        _uiState.update { state ->
            if (tagName !in state.tags) return@update state
            val currentTags = state.mediaTags[mediaUri].orEmpty()
            val nextItemTags = if (tagName in currentTags) currentTags - tagName else currentTags + tagName
            val nextMediaTags = if (nextItemTags.isEmpty()) {
                state.mediaTags - mediaUri
            } else {
                state.mediaTags + (mediaUri to nextItemTags)
            }
            tagStore.save(TagData(state.tags, nextMediaTags))
            state.copy(mediaTags = nextMediaTags).withTagFolders()
        }
    }

    fun selectFavoriteFolder(collectionId: String) {
        val favoriteFolderId = collectionId.removePrefix(FAVORITE_FOLDER_PREFIX)
        _uiState.update { state ->
            if (state.favoriteFolders.none { it.id == favoriteFolderId }) return@update state
            state.copy(
                selectedFavoriteFolderId = favoriteFolderId,
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun createFavoriteFolder() {
        _uiState.update { state ->
            val newFolder = FavoriteFolder(
                id = UUID.randomUUID().toString(),
                name = LocalDate.now().format(FAVORITE_FOLDER_DATE_FORMAT),
            )
            val nextData = FavoriteFolderData(
                folders = state.favoriteFolders + newFolder,
                mediaUrisByFolderId = state.favoriteFolderMediaUris,
                defaultFolderId = state.defaultFavoriteFolderId ?: newFolder.id,
            )
            favoriteFolderStore.save(nextData)
            state.copy(
                favoriteFolders = nextData.folders,
                favoriteFolderMediaUris = nextData.mediaUrisByFolderId,
                defaultFavoriteFolderId = nextData.defaultFolderId,
                selectedFavoriteFolderId = newFolder.id,
                browserMode = BrowserMode.FavoriteFolder,
                rightPaneMode = RightPaneMode.Browser,
                viewerIndex = null,
            )
        }
    }

    fun renameFavoriteFolder(folderId: String, name: String) {
        val normalizedName = name.trim().takeIf { it.isNotBlank() } ?: return
        _uiState.update { state ->
            val nextFolders = state.favoriteFolders.map { folder ->
                if (folder.id == folderId) folder.copy(name = normalizedName) else folder
            }
            if (nextFolders == state.favoriteFolders) return@update state
            val nextData = FavoriteFolderData(nextFolders, state.favoriteFolderMediaUris, state.defaultFavoriteFolderId)
            favoriteFolderStore.save(nextData)
            state.copy(favoriteFolders = nextFolders)
        }
    }

    fun setDefaultFavoriteFolder(folderId: String) {
        _uiState.update { state ->
            if (state.favoriteFolders.none { it.id == folderId }) return@update state
            val nextData = FavoriteFolderData(state.favoriteFolders, state.favoriteFolderMediaUris, folderId)
            favoriteFolderStore.save(nextData)
            state.copy(defaultFavoriteFolderId = folderId)
        }
    }

    fun toggleMediaInDefaultFavoriteFolder(mediaUri: String) {
        _uiState.update { state ->
            val defaultFolderId = state.defaultFavoriteFolderId
            if (defaultFolderId == null) {
                val newFolder = FavoriteFolder(
                    id = UUID.randomUUID().toString(),
                    name = LocalDate.now().format(FAVORITE_FOLDER_DATE_FORMAT),
                )
                val nextData = FavoriteFolderData(
                    folders = state.favoriteFolders + newFolder,
                    mediaUrisByFolderId = state.favoriteFolderMediaUris + (newFolder.id to setOf(mediaUri)),
                    defaultFolderId = newFolder.id,
                )
                favoriteFolderStore.save(nextData)
                return@update state.copy(
                    favoriteFolders = nextData.folders,
                    favoriteFolderMediaUris = nextData.mediaUrisByFolderId,
                    defaultFavoriteFolderId = newFolder.id,
                )
            }
            val currentUris = state.favoriteFolderMediaUris[defaultFolderId].orEmpty()
            val nextUris = if (mediaUri in currentUris) currentUris - mediaUri else currentUris + mediaUri
            val nextMediaUris = if (nextUris.isEmpty()) {
                state.favoriteFolderMediaUris - defaultFolderId
            } else {
                state.favoriteFolderMediaUris + (defaultFolderId to nextUris)
            }
            val nextData = FavoriteFolderData(state.favoriteFolders, nextMediaUris, defaultFolderId)
            favoriteFolderStore.save(nextData)
            state.copy(favoriteFolderMediaUris = nextMediaUris)
        }
    }

    fun addVideoSegment(mediaUri: String, startMillis: Long, endMillis: Long) {
        val start = startMillis.coerceAtLeast(0L)
        val end = endMillis.coerceAtLeast(0L)
        if (end <= start) return
        val segment = VideoSegment(start, end)
        _uiState.update { state ->
            val nextSegments = (state.videoSegmentsByUri[mediaUri].orEmpty() + segment)
                .distinct()
                .sortedBy { it.startMillis }
            val nextByUri = state.videoSegmentsByUri + (mediaUri to nextSegments)
            videoSegmentStore.save(nextByUri)
            state.copy(videoSegmentsByUri = nextByUri)
        }
    }

    fun deleteVideoSegment(mediaUri: String, segment: VideoSegment) {
        _uiState.update { state ->
            val nextSegments = state.videoSegmentsByUri[mediaUri].orEmpty() - segment
            val nextByUri = if (nextSegments.isEmpty()) {
                state.videoSegmentsByUri - mediaUri
            } else {
                state.videoSegmentsByUri + (mediaUri to nextSegments)
            }
            videoSegmentStore.save(nextByUri)
            state.copy(videoSegmentsByUri = nextByUri)
        }
    }

    fun renameVideoSegment(mediaUri: String, segment: VideoSegment, name: String) {
        val normalizedName = name.trim().take(40).ifBlank { null }
        _uiState.update { state ->
            val nextSegments = state.videoSegmentsByUri[mediaUri].orEmpty()
                .map { current ->
                    if (current == segment) current.copy(name = normalizedName) else current
                }
                .distinct()
                .sortedBy { it.startMillis }
            val nextByUri = if (nextSegments.isEmpty()) {
                state.videoSegmentsByUri - mediaUri
            } else {
                state.videoSegmentsByUri + (mediaUri to nextSegments)
            }
            videoSegmentStore.save(nextByUri)
            state.copy(videoSegmentsByUri = nextByUri)
        }
    }

    fun ensureVideoThumbnail(item: MediaItem) {
        if (item.mediaType != app.videosee.domain.MediaType.Video) return
        if (_uiState.value.videoThumbnailPaths.containsKey(item.uri)) return
        if (!thumbnailRequestsInFlight.add(item.uri)) return
        viewModelScope.launch {
            val file = runCatching { thumbnailCacheRepository.getOrCreate(item) }.getOrNull()
            thumbnailRequestsInFlight.remove(item.uri)
            if (file != null) {
                _uiState.update { state ->
                    state.copy(videoThumbnailPaths = state.videoThumbnailPaths + (item.uri to file.absolutePath))
                }
            }
        }
    }

    fun exportFavoritesBackupJson(): String {
        val state = _uiState.value
        return FavoriteBackupJson.encode(
            authorFavoriteLevels = state.authorFavoriteLevels,
            mediaFavoriteLevels = state.mediaFavoriteLevels,
            favoriteFolderData = FavoriteFolderData(
                state.favoriteFolders,
                state.favoriteFolderMediaUris,
                state.defaultFavoriteFolderId,
            ),
            videoSegmentsByUri = state.videoSegmentsByUri,
        )
    }

    fun exportTagsBackupJson(): String {
        val state = _uiState.value
        return TagBackupJson.encode(TagData(state.tags, state.mediaTags))
    }

    fun exportAllBackupJson(): String {
        val state = _uiState.value
        return AppBackupJson.encode(
            authorFavoriteLevels = state.authorFavoriteLevels,
            mediaFavoriteLevels = state.mediaFavoriteLevels,
            tagData = TagData(state.tags, state.mediaTags),
            favoriteFolderData = FavoriteFolderData(
                state.favoriteFolders,
                state.favoriteFolderMediaUris,
                state.defaultFavoriteFolderId,
            ),
            videoSegmentsByUri = state.videoSegmentsByUri,
        )
    }

    fun importFavoritesBackupJson(json: String): FavoriteBackupImportResult {
        val backup = FavoriteBackupJson.decode(json)
        authorFavoriteStore.save(backup.authorFavoriteLevels)
        mediaFavoriteStore.save(backup.mediaFavoriteLevels)
        favoriteFolderStore.save(backup.favoriteFolderData)
        videoSegmentStore.save(backup.videoSegmentsByUri)
        _uiState.update { state ->
            state.copy(
                authorFavoriteLevels = backup.authorFavoriteLevels,
                mediaFavoriteLevels = backup.mediaFavoriteLevels,
                favoriteFolders = backup.favoriteFolderData.folders,
                favoriteFolderMediaUris = backup.favoriteFolderData.mediaUrisByFolderId,
                defaultFavoriteFolderId = backup.favoriteFolderData.defaultFolderId,
                selectedFavoriteFolderId = backup.favoriteFolderData.folders.firstOrNull()?.id,
                videoSegmentsByUri = backup.videoSegmentsByUri,
                folders = state.folders.withMediaFavoriteLevels(backup.mediaFavoriteLevels),
                authors = state.authors
                    .withAuthorFavoriteLevels(backup.authorFavoriteLevels)
                    .withMediaFavoriteLevels(backup.mediaFavoriteLevels),
                viewerIndex = null,
            )
        }
        return FavoriteBackupImportResult(
            authorCount = backup.authorFavoriteLevels.size,
            mediaCount = backup.mediaFavoriteLevels.size,
        )
    }

    fun importTagsBackupJson(json: String): TagBackupImportResult {
        val tagData = TagBackupJson.decode(json)
        tagStore.save(tagData)
        _uiState.update { state ->
            state.copy(tags = tagData.tagNames, mediaTags = tagData.mediaTags).withTagFolders()
        }
        return TagBackupImportResult(tagCount = tagData.tagNames.size, mediaCount = tagData.mediaTags.size)
    }

    fun importAllBackupJson(json: String): AppBackupImportResult {
        val backup = AppBackupJson.decode(json)
        authorFavoriteStore.save(backup.authorFavoriteLevels)
        mediaFavoriteStore.save(backup.mediaFavoriteLevels)
        tagStore.save(backup.tagData)
        favoriteFolderStore.save(backup.favoriteFolderData)
        videoSegmentStore.save(backup.videoSegmentsByUri)
        _uiState.update { state ->
            state.copy(
                authorFavoriteLevels = backup.authorFavoriteLevels,
                mediaFavoriteLevels = backup.mediaFavoriteLevels,
                tags = backup.tagData.tagNames,
                mediaTags = backup.tagData.mediaTags,
                favoriteFolders = backup.favoriteFolderData.folders,
                favoriteFolderMediaUris = backup.favoriteFolderData.mediaUrisByFolderId,
                defaultFavoriteFolderId = backup.favoriteFolderData.defaultFolderId,
                selectedFavoriteFolderId = backup.favoriteFolderData.folders.firstOrNull()?.id,
                videoSegmentsByUri = backup.videoSegmentsByUri,
                folders = state.folders.withMediaFavoriteLevels(backup.mediaFavoriteLevels),
                authors = state.authors
                    .withAuthorFavoriteLevels(backup.authorFavoriteLevels)
                    .withMediaFavoriteLevels(backup.mediaFavoriteLevels),
            ).withTagFolders()
        }
        return AppBackupImportResult(
            authorCount = backup.authorFavoriteLevels.size,
            favoriteMediaCount = backup.mediaFavoriteLevels.size,
            tagCount = backup.tagData.tagNames.size,
            taggedMediaCount = backup.tagData.mediaTags.size,
        )
    }

    fun selectBrowserMode(mode: BrowserMode) {
        _uiState.update {
            it.copy(
                browserMode = mode,
                rightPaneMode = RightPaneMode.Browser,
                collectionSortField = it.collectionSortField
                    .takeIf { field -> mode == BrowserMode.Author || field != CollectionSortField.FavoriteLevel }
                    ?: CollectionSortField.ModifiedTime,
                viewerIndex = null,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun openAuthorSearch(authorId: String) {
        val query = authorId.trim().takeIf { it.isNotBlank() } ?: return
        _uiState.update { state ->
            val matchingAuthorId = state.authors.firstOrNull { author ->
                author.name.equals(query, ignoreCase = true) || author.name.contains(query, ignoreCase = true)
            }?.id
            state.copy(
                browserMode = BrowserMode.Author,
                collectionSearchQuery = query,
                selectedAuthorId = matchingAuthorId ?: state.selectedAuthorId,
                rightPaneMode = RightPaneMode.Browser,
                viewerIndex = null,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun updateCollectionSearchQuery(query: String) {
        _uiState.update {
            it.copy(collectionSearchQuery = query, rightPaneMode = RightPaneMode.Browser)
        }
    }

    fun selectCollectionSortField(field: CollectionSortField) {
        _uiState.update {
            it.copy(collectionSortField = field, viewerIndex = null, rightPaneMode = RightPaneMode.Browser)
        }
    }

    fun toggleCollectionSortDirection() {
        _uiState.update {
            it.copy(
                collectionSortDirection = it.collectionSortDirection.toggle(),
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            )
        }
    }

    fun selectMediaSortField(field: MediaSortField) {
        _uiState.update { state ->
            state.copy(
                mediaSortField = field,
                favoriteMediaSortUriOrder = emptyList(),
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun toggleMediaSortDirection() {
        _uiState.update { state ->
            state.copy(
                mediaSortDirection = state.mediaSortDirection.toggle(),
                favoriteMediaSortUriOrder = emptyList(),
                viewerIndex = null,
                rightPaneMode = RightPaneMode.Browser,
            ).withFreshFavoriteMediaSortOrder()
        }
    }

    fun openSyncPane() {
        _uiState.update { it.copy(rightPaneMode = RightPaneMode.Sync, viewerIndex = null) }
    }

    fun openTsConvertPane() {
        _uiState.update { it.copy(rightPaneMode = RightPaneMode.TsConvert, viewerIndex = null) }
    }

    fun openSettingsPane() {
        _uiState.update { it.copy(rightPaneMode = RightPaneMode.Settings, viewerIndex = null) }
    }

    fun openTagSettingsPane() {
        _uiState.update { it.copy(rightPaneMode = RightPaneMode.Tags, viewerIndex = null) }
    }

    fun selectAppTheme(theme: AppTheme) {
        appearanceStore.theme = theme
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun openBackupPane() {
        _uiState.update { it.copy(rightPaneMode = RightPaneMode.Backup, viewerIndex = null) }
    }

    fun updateSyncHost(host: String) {
        syncSettingsStore.host = host
        _uiState.update { it.copy(syncHost = host) }
    }

    fun updateSyncPort(port: String) {
        syncSettingsStore.port = port
        _uiState.update { it.copy(syncPort = port.filter { char -> char.isDigit() }) }
    }

    fun updateSyncToken(token: String) {
        syncSettingsStore.token = token
        _uiState.update { it.copy(syncToken = token) }
    }

    fun updateSyncDeviceId(deviceId: String) {
        syncSettingsStore.deviceId = deviceId
        _uiState.update { it.copy(syncDeviceId = deviceId) }
    }

    fun updateTsDownloadDirectory(path: String) {
        tsConvertSettingsStore.downloadDirectory = path
        _uiState.update { it.copy(tsDownloadDirectory = path) }
    }

    fun updateTsNewSourcePath(path: String) {
        _uiState.update { it.copy(tsNewSourcePath = path) }
    }

    fun addTsSourcePath() {
        val path = _uiState.value.tsNewSourcePath.trim()
        if (path.isBlank()) {
            _uiState.update { it.copy(tsMessage = "请先填写源路径") }
            return
        }
        val nextPaths = (_uiState.value.tsSourcePaths + path).distinct()
        tsConvertSettingsStore.sourcePaths = nextPaths
        _uiState.update {
            it.copy(
                tsSourcePaths = nextPaths,
                tsNewSourcePath = "",
                tsMessage = null,
            )
        }
    }

    fun addTsSourceTreeUri(uri: String) {
        val nextPaths = (_uiState.value.tsSourcePaths + uri).distinct()
        tsConvertSettingsStore.sourcePaths = nextPaths
        _uiState.update {
            it.copy(
                tsSourcePaths = nextPaths,
                tsNewSourcePath = "",
                tsMessage = "已添加授权文件夹",
            )
        }
    }

    fun removeTsSourcePath(path: String) {
        val nextPaths = _uiState.value.tsSourcePaths.filterNot { it == path }
        tsConvertSettingsStore.sourcePaths = nextPaths
        _uiState.update {
            it.copy(
                tsSourcePaths = nextPaths,
                tsPathStatuses = it.tsPathStatuses.filterNot { status -> status.path == path },
            )
        }
    }

    fun scanTsVideos() {
        val sourcePaths = listOf(tsConvertRepository.privateImportDirectory().absolutePath)
        viewModelScope.launch {
            _uiState.update { it.copy(tsIsScanning = true, tsMessage = null) }
            runCatching {
                tsConvertRepository.scan(
                    sourcePaths = sourcePaths,
                    downloadedKeys = tsConvertSettingsStore.downloadedKeys(),
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        tsIsScanning = false,
                        tsPathStatuses = result.paths,
                        tsScanIssues = result.issues,
                        tsVideos = result.videos,
                        tsMessage = if (result.videos.isEmpty()) {
                            if (result.issues.isEmpty()) "没有新的视频待转换和下载" else "没有可转换视频，下面列出了跳过原因"
                        } else {
                            "有 ${result.videos.size} 个新的视频待转换和下载"
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        tsIsScanning = false,
                        tsMessage = error.localizedMessage ?: "扫描失败",
                    )
                }
            }
        }
    }

    fun convertTsVideo(video: TsVideoCandidate) {
        val state = _uiState.value
        if (video.id in state.tsConvertingIds || video.id in state.tsConvertedOutputPaths) return
        viewModelScope.launch {
            _uiState.update { it.copy(tsConvertingIds = it.tsConvertingIds + video.id, tsMessage = "开始转换: ${video.outputFileName}") }
            runCatching { tsConvertRepository.convert(video) }
                .onSuccess { outputFile ->
                    _uiState.update {
                        it.copy(
                            tsConvertingIds = it.tsConvertingIds - video.id,
                            tsConvertedOutputPaths = it.tsConvertedOutputPaths + (video.id to outputFile.absolutePath),
                            tsMessage = "已转换: ${video.outputFileName}",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            tsConvertingIds = it.tsConvertingIds - video.id,
                            tsMessage = "${video.outputFileName} 转换失败: ${error.localizedMessage ?: "Media3 不支持该清单"}",
                        )
                    }
                }
        }
    }

    fun downloadTsVideo(video: TsVideoCandidate) {
        val state = _uiState.value
        val outputPath = state.tsConvertedOutputPaths[video.id]
        if (video.id in state.tsDownloadingIds) return
        if (outputPath == null) {
            _uiState.update { it.copy(tsMessage = "请先转换: ${video.outputFileName}") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(tsDownloadingIds = it.tsDownloadingIds + video.id, tsMessage = "开始下载: ${video.outputFileName}") }
            runCatching {
                tsConvertRepository.publishConverted(
                    video = video,
                    sourceFile = java.io.File(outputPath),
                    downloadDirectory = state.tsDownloadDirectory,
                )
            }.onSuccess { uri ->
                tsConvertSettingsStore.markDownloaded(video, uri)
                _uiState.update {
                    it.copy(
                        tsDownloadingIds = it.tsDownloadingIds - video.id,
                        tsVideos = it.tsVideos.filterNot { candidate -> candidate.id == video.id },
                        tsConvertedOutputPaths = it.tsConvertedOutputPaths - video.id,
                        tsDownloadedCount = tsConvertSettingsStore.downloadedRecords().size,
                        tsMessage = "已下载: ${video.outputFileName}",
                    )
                }
                refresh()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        tsDownloadingIds = it.tsDownloadingIds - video.id,
                        tsMessage = "${video.outputFileName} 下载失败: ${error.localizedMessage}",
                    )
                }
            }
        }
    }

    fun convertAndDownloadAllTsVideos() {
        val state = _uiState.value
        if (state.tsBatchIsRunning || tsBatchJob?.isActive == true) return
        val videos = state.tsVideos.filterNot { video ->
            video.id in state.tsConvertingIds || video.id in state.tsDownloadingIds
        }
        if (videos.isEmpty()) {
            _uiState.update { it.copy(tsMessage = "没有待转换视频") }
            return
        }
        val downloadDirectory = state.tsDownloadDirectory
        tsBatchJob = viewModelScope.launch {
            var completed = 0
            var succeeded = 0
            var failed = 0
            val batchIds = videos.map { it.id }.toSet()
            _uiState.update {
                it.copy(
                    tsBatchIsRunning = true,
                    tsBatchTotal = videos.size,
                    tsBatchCompleted = 0,
                    tsBatchCurrentName = videos.first().outputFileName,
                    tsBatchFailedCount = 0,
                    tsMessage = "开始批量转换下载，共 ${videos.size} 个视频",
                )
            }
            for ((index, video) in videos.withIndex()) {
                _uiState.update {
                    it.copy(
                        tsBatchCurrentName = video.outputFileName,
                        tsMessage = "正在处理 ${index + 1}/${videos.size}: ${video.outputFileName}",
                    )
                }
                val outputFile = runCatching {
                    val existingPath = _uiState.value.tsConvertedOutputPaths[video.id]
                    if (existingPath != null && java.io.File(existingPath).isFile) {
                        java.io.File(existingPath)
                    } else {
                        _uiState.update { it.copy(tsConvertingIds = it.tsConvertingIds + video.id) }
                        tsConvertRepository.convert(video)
                    }
                }.onFailure { error ->
                    failed += 1
                    completed += 1
                    _uiState.update {
                        it.copy(
                            tsConvertingIds = it.tsConvertingIds - video.id,
                            tsBatchCompleted = completed,
                            tsBatchFailedCount = failed,
                            tsMessage = "${video.outputFileName} 转换失败: ${error.localizedMessage ?: "Media3 不支持该清单"}",
                        )
                    }
                }.getOrNull()
                _uiState.update { it.copy(tsConvertingIds = it.tsConvertingIds - video.id) }
                if (outputFile == null) continue

                _uiState.update {
                    it.copy(
                        tsConvertedOutputPaths = it.tsConvertedOutputPaths + (video.id to outputFile.absolutePath),
                        tsDownloadingIds = it.tsDownloadingIds + video.id,
                        tsMessage = "正在下载 ${index + 1}/${videos.size}: ${video.outputFileName}",
                    )
                }
                runCatching {
                    tsConvertRepository.publishConverted(
                        video = video,
                        sourceFile = outputFile,
                        downloadDirectory = downloadDirectory,
                    )
                }.onSuccess { uri ->
                    tsConvertSettingsStore.markDownloaded(video, uri)
                    succeeded += 1
                    completed += 1
                    _uiState.update {
                        it.copy(
                            tsDownloadingIds = it.tsDownloadingIds - video.id,
                            tsVideos = it.tsVideos.filterNot { candidate -> candidate.id == video.id },
                            tsConvertedOutputPaths = it.tsConvertedOutputPaths - video.id,
                            tsBatchCompleted = completed,
                            tsDownloadedCount = tsConvertSettingsStore.downloadedRecords().size,
                            tsMessage = "已下载 ${completed}/${videos.size}: ${video.outputFileName}",
                        )
                    }
                }.onFailure { error ->
                    failed += 1
                    completed += 1
                    _uiState.update {
                        it.copy(
                            tsDownloadingIds = it.tsDownloadingIds - video.id,
                            tsBatchCompleted = completed,
                            tsBatchFailedCount = failed,
                            tsMessage = "${video.outputFileName} 下载失败: ${error.localizedMessage}",
                        )
                    }
                }
            }
            val doneMessage = if (failed == 0) {
                "ts视频转换下载完成，共转换下载 $succeeded 个"
            } else {
                "ts视频批量处理完成，成功 $succeeded 个，失败 $failed 个"
            }
            _uiState.update {
                it.copy(
                    tsBatchIsRunning = false,
                    tsBatchCompleted = completed,
                    tsBatchCurrentName = "",
                    tsBatchFailedCount = failed,
                    tsConvertingIds = it.tsConvertingIds - batchIds,
                    tsDownloadingIds = it.tsDownloadingIds - batchIds,
                    tsMessage = doneMessage,
                    appBannerMessage = doneMessage,
                )
            }
            if (succeeded > 0) refresh()
        }
    }

    fun clearAppBannerMessage() {
        _uiState.update { it.copy(appBannerMessage = null) }
    }

    fun loadSyncPendingFiles() {
        val state = _uiState.value
        val baseUrl = state.syncBaseUrlOrNull()
        if (baseUrl == null || state.syncToken.isBlank()) {
            _uiState.update { it.copy(syncMessage = "请先填写 IP、端口和 Token") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(syncIsLoading = true, syncMessage = null) }
            runCatching {
                syncRepository.loadPending(
                    baseUrl = baseUrl,
                    token = state.syncToken.trim(),
                )
            }.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        syncPendingFiles = files,
                        syncIsLoading = false,
                        syncMessage = if (files.isEmpty()) "没有待同步文件" else "发现 ${files.size} 个待同步文件",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        syncIsLoading = false,
                        syncMessage = error.localizedMessage ?: "同步失败",
                    )
                }
            }
        }
    }

    fun downloadSyncFile(file: SyncPendingFile) {
        val state = _uiState.value
        val baseUrl = state.syncBaseUrlOrNull()
        if (file.id in state.syncDownloadingIds || baseUrl == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(syncDownloadingIds = it.syncDownloadingIds + file.id, syncMessage = null) }
            runCatching {
                syncRepository.downloadAndAck(
                    baseUrl = baseUrl,
                    token = state.syncToken.trim(),
                    deviceId = state.syncDeviceId.trim().ifBlank { "videosee-phone" },
                    file = file,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        syncDownloadingIds = it.syncDownloadingIds - file.id,
                        syncPendingFiles = it.syncPendingFiles.filterNot { pending -> pending.id == file.id },
                        syncMessage = "已下载: ${file.filename}",
                    )
                }
                refresh()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        syncDownloadingIds = it.syncDownloadingIds - file.id,
                        syncMessage = error.localizedMessage ?: "下载失败",
                    )
                }
            }
        }
    }

    fun downloadAllSyncFiles() {
        val state = _uiState.value
        val baseUrl = state.syncBaseUrlOrNull()
        val token = state.syncToken.trim()
        val deviceId = state.syncDeviceId.trim().ifBlank { "videosee-phone" }
        val files = state.syncPendingFiles.filterNot { it.id in state.syncDownloadingIds }
        if (baseUrl == null || token.isBlank()) {
            _uiState.update { it.copy(syncMessage = "请先填写 IP、端口和 Token") }
            return
        }
        if (files.isEmpty()) {
            _uiState.update { it.copy(syncMessage = "没有待下载文件") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    syncDownloadingIds = it.syncDownloadingIds + files.map { file -> file.id },
                    syncMessage = "开始下载 ${files.size} 个文件",
                )
            }
            var successCount = 0
            files.forEach { file ->
                runCatching {
                    syncRepository.downloadAndAck(
                        baseUrl = baseUrl,
                        token = token,
                        deviceId = deviceId,
                        file = file,
                    )
                }.onSuccess {
                    successCount += 1
                    _uiState.update {
                        it.copy(
                            syncDownloadingIds = it.syncDownloadingIds - file.id,
                            syncPendingFiles = it.syncPendingFiles.filterNot { pending -> pending.id == file.id },
                            syncMessage = "已下载 $successCount/${files.size}: ${file.filename}",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            syncDownloadingIds = it.syncDownloadingIds - file.id,
                            syncMessage = "${file.filename} 下载失败: ${error.localizedMessage}",
                        )
                    }
                }
            }
            refresh()
        }
    }

    fun openViewer(item: MediaItem) {
        shuffleResetJob?.cancel()
        _uiState.update { state ->
            val nextRecentPlaybackUris = (listOf(item.uri) + state.recentPlaybackUris.filterNot { it == item.uri })
                .take(MAX_RECENT_PLAYBACK_ITEMS)
            recentPlaybackStore.save(nextRecentPlaybackUris)
            val stateWithHistory = state.copy(recentPlaybackUris = nextRecentPlaybackUris)
            val nextShuffleOrder = stateWithHistory.shuffleUriOrderForCurrentCollection(item.uri)
            val nextState = stateWithHistory.copy(shuffleUriOrder = nextShuffleOrder)
            val index = nextState.selectedItems.indexOfFirst { it.uri == item.uri }
            nextState.copy(
                viewerIndex = index.takeIf { it >= 0 },
                gridReturnTargetUri = null,
            )
        }
    }

    fun closeViewer() {
        val shouldScheduleShuffleReset = _uiState.value.playbackMode == PlaybackMode.Shuffle &&
            _uiState.value.shuffleUriOrder.isNotEmpty()
        _uiState.update { state ->
            state.copy(
                viewerIndex = null,
                gridReturnTargetUri = state.viewerItem?.uri,
            )
        }
        if (shouldScheduleShuffleReset) scheduleShuffleOrderReset()
    }

    fun togglePlaybackMode() {
        _uiState.update { state ->
            val currentUri = state.viewerItem?.uri
            when (state.playbackMode) {
                PlaybackMode.Sequential -> {
                    val shuffleOrder = state.shuffleUriOrderForCurrentCollection(currentUri)
                    val nextState = state.copy(
                        playbackMode = PlaybackMode.Shuffle,
                        shuffleUriOrder = shuffleOrder,
                    )
                    nextState.copy(viewerIndex = currentUri?.let { uri -> nextState.selectedItems.indexOfFirst { it.uri == uri } } ?: state.viewerIndex)
                }

                PlaybackMode.Shuffle -> {
                    val nextState = state.copy(playbackMode = PlaybackMode.Sequential, shuffleUriOrder = emptyList())
                    nextState.copy(viewerIndex = currentUri?.let { uri -> nextState.selectedItems.indexOfFirst { it.uri == uri } } ?: state.viewerIndex)
                }
            }
        }
    }

    fun clearGridReturnTarget(uri: String) {
        _uiState.update { state ->
            if (state.gridReturnTargetUri == uri) {
                state.copy(gridReturnTargetUri = null)
            } else {
                state
            }
        }
    }

    fun showNext() {
        _uiState.update { state ->
            val current = state.viewerIndex ?: return@update state
            val next = (current + 1).coerceAtMost(state.selectedItems.lastIndex)
            state.copy(viewerIndex = next)
        }
    }

    fun showFirst() {
        _uiState.update { state ->
            if (state.selectedItems.isEmpty()) {
                state
            } else {
                state.copy(viewerIndex = 0)
            }
        }
    }

    fun showPrevious() {
        _uiState.update { state ->
            val current = state.viewerIndex ?: return@update state
            val previous = (current - 1).coerceAtLeast(0)
            state.copy(viewerIndex = previous)
        }
    }

    fun showLast() {
        _uiState.update { state ->
            if (state.selectedItems.isEmpty()) {
                state
            } else {
                state.copy(viewerIndex = state.selectedItems.lastIndex)
            }
        }
    }

    private fun scheduleShuffleOrderReset() {
        shuffleResetJob?.cancel()
        shuffleResetJob = viewModelScope.launch {
            delay(SHUFFLE_ORDER_RESET_DELAY_MILLIS)
            _uiState.update { state ->
                if (state.viewerIndex == null) state.copy(shuffleUriOrder = emptyList()) else state
            }
        }
    }
}

data class FavoriteBackupImportResult(
    val authorCount: Int,
    val mediaCount: Int,
)

data class TagBackupImportResult(
    val tagCount: Int,
    val mediaCount: Int,
)

data class AppBackupImportResult(
    val authorCount: Int,
    val favoriteMediaCount: Int,
    val tagCount: Int,
    val taggedMediaCount: Int,
)

private fun SortDirection.toggle(): SortDirection {
    return when (this) {
        SortDirection.Ascending -> SortDirection.Descending
        SortDirection.Descending -> SortDirection.Ascending
    }
}

private fun VideoSeeUiState.withFreshFavoriteMediaSortOrder(): VideoSeeUiState {
    if (mediaSortField != MediaSortField.FavoriteLevel) {
        return copy(favoriteMediaSortUriOrder = emptyList())
    }
    return copy(
        favoriteMediaSortUriOrder = MediaSort.sortItems(
            items = selectedCollection?.items.orEmpty(),
            field = mediaSortField,
            direction = mediaSortDirection,
        ).map { it.uri },
    )
}

private fun VideoSeeUiState.shuffleUriOrderForCurrentCollection(currentUri: String?): List<String> {
    val items = sequentialSelectedItems
    if (shuffleUriOrder.matchesItems(items)) return shuffleUriOrder
    val otherUris = items.map { it.uri }.filterNot { it == currentUri }.shuffled()
    return listOfNotNull(currentUri?.takeIf { uri -> items.any { it.uri == uri } }) + otherUris
}

private fun List<String>.matchesItems(items: List<MediaItem>): Boolean {
    return size == items.size && toSet() == items.map { it.uri }.toSet()
}

private fun VideoSeeUiState.withTagFolders(): VideoSeeUiState {
    val allItems = folders.flatMap { it.items }.distinctBy { it.uri }
    val nextTagFolders = MediaOrganizer.groupByTag(
        items = allItems,
        tagNames = tags,
        mediaTags = mediaTags,
    )
    val nextSelectedTagIds = selectedTagIds
        .filterTo(mutableSetOf()) { id -> nextTagFolders.any { it.id == id } }
        .ifEmpty { nextTagFolders.firstOrNull()?.let { mutableSetOf(it.id) } ?: mutableSetOf() }
    return copy(tagFolders = nextTagFolders, selectedTagIds = nextSelectedTagIds)
}

private fun VideoSeeUiState.syncBaseUrlOrNull(): String? {
    val host = syncHost.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
    val port = syncPort.trim().toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
    return host.takeIf { it.isNotBlank() }?.let { "http://$it:$port" }
}

private fun List<MediaFolder>.withAuthorFavoriteLevels(favoriteLevels: Map<String, Int>): List<MediaFolder> {
    return map { folder ->
        folder.copy(favoriteLevel = favoriteLevels[folder.id]?.coerceIn(0, MAX_FAVORITE_LEVEL) ?: 0)
    }
}

private fun List<MediaFolder>.withMediaFavoriteLevels(favoriteLevels: Map<String, Int>): List<MediaFolder> {
    return map { folder ->
        folder.copy(
            items = folder.items.map { item ->
                item.copy(favoriteLevel = favoriteLevels[item.uri]?.coerceIn(0, MAX_FAVORITE_LEVEL) ?: 0)
            },
        )
    }
}

private class FavoriteLevelStore(context: Context, preferencesName: String) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun load(): Map<String, Int> {
        return preferences.all.mapNotNull { (key, value) ->
            val level = (value as? Int)?.coerceIn(0, MAX_FAVORITE_LEVEL) ?: return@mapNotNull null
            key.takeIf { level > 0 }?.let { it to level }
        }.toMap()
    }

    fun save(favoriteLevels: Map<String, Int>) {
        preferences.edit().clear().apply {
            favoriteLevels.forEach { (id, level) ->
                val normalizedLevel = level.coerceIn(0, MAX_FAVORITE_LEVEL)
                if (normalizedLevel > 0) {
                    putInt(id, normalizedLevel)
                }
            }
        }.apply()
    }
}

private class RecentPlaybackStore(context: Context) {
    private val preferences = context.getSharedPreferences("recent_playback", Context.MODE_PRIVATE)

    fun load(): List<String> {
        val history = preferences.getString("uris", null)?.let(::JSONArray) ?: return emptyList()
        return buildList {
            for (index in 0 until history.length()) {
                history.optString(index).takeIf { it.isNotBlank() && it !in this }?.let(::add)
                if (size >= MAX_RECENT_PLAYBACK_ITEMS) return@buildList
            }
        }
    }

    fun save(uris: List<String>) {
        preferences.edit()
            .putString("uris", JSONArray(uris.take(MAX_RECENT_PLAYBACK_ITEMS)).toString())
            .apply()
    }
}

private class VideoSegmentStore(context: Context) {
    private val preferences = context.getSharedPreferences("video_segments", Context.MODE_PRIVATE)

    fun load(): Map<String, List<VideoSegment>> {
        val root = preferences.getString("data", null)?.let(::JSONObject) ?: return emptyMap()
        return VideoSegmentBackupJson.decode(root)
    }

    fun save(segmentsByUri: Map<String, List<VideoSegment>>) {
        preferences.edit().putString("data", VideoSegmentBackupJson.encode(segmentsByUri).toString()).apply()
    }
}

private data class TagData(
    val tagNames: List<String>,
    val mediaTags: Map<String, Set<String>>,
)

private class TagDataStore(context: Context) {
    private val preferences = context.getSharedPreferences("media_tags", Context.MODE_PRIVATE)

    fun load(): TagData {
        return preferences.getString("tag_data", null)
            ?.let { TagBackupJson.decode(it) }
            ?: TagData(emptyList(), emptyMap())
    }

    fun save(tagData: TagData) {
        preferences.edit().putString("tag_data", TagBackupJson.encode(tagData)).apply()
    }
}

private data class FavoriteFolderData(
    val folders: List<FavoriteFolder>,
    val mediaUrisByFolderId: Map<String, Set<String>>,
    val defaultFolderId: String?,
)

private class FavoriteFolderStore(context: Context) {
    private val preferences = context.getSharedPreferences("favorite_folders", Context.MODE_PRIVATE)

    fun load(): FavoriteFolderData {
        val root = preferences.getString("data", null)?.let(::JSONObject) ?: return FavoriteFolderData(emptyList(), emptyMap(), null)
        val foldersJson = root.optJSONArray("folders") ?: JSONArray()
        val folders: List<FavoriteFolder> = buildList {
            for (index in 0 until foldersJson.length()) {
                val folderJson = foldersJson.optJSONObject(index) ?: continue
                val id = folderJson.optString("id").trim()
                val name = folderJson.optString("name").trim()
                if (id.isNotBlank() && name.isNotBlank() && none { folder -> folder.id == id }) {
                    add(FavoriteFolder(id, name))
                }
            }
        }
        val mediaUrisJson = root.optJSONObject("mediaUris") ?: JSONObject()
        val mediaUrisByFolderId = buildMap {
            folders.forEach { folder ->
                val urisJson = mediaUrisJson.optJSONArray(folder.id) ?: return@forEach
                val uris = buildSet {
                    for (index in 0 until urisJson.length()) {
                        urisJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                if (uris.isNotEmpty()) put(folder.id, uris)
            }
        }
        val defaultFolderId = root.optString("defaultFolderId").takeIf { id -> folders.any { it.id == id } }
        return FavoriteFolderData(folders, mediaUrisByFolderId, defaultFolderId)
    }

    fun save(data: FavoriteFolderData) {
        val mediaUris = JSONObject()
        data.mediaUrisByFolderId.forEach { (folderId, uris) ->
            if (data.folders.any { it.id == folderId } && uris.isNotEmpty()) {
                mediaUris.put(folderId, JSONArray(uris.sorted()))
            }
        }
        val root = JSONObject()
            .put("version", 1)
            .put("folders", JSONArray().apply {
                data.folders.forEach { folder -> put(JSONObject().put("id", folder.id).put("name", folder.name)) }
            })
            .put("mediaUris", mediaUris)
            .put("defaultFolderId", data.defaultFolderId)
        preferences.edit().putString("data", root.toString()).apply()
    }
}

private fun FavoriteFolder.collectionId(): String = FAVORITE_FOLDER_PREFIX + id

private class SyncSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("mobile_sync_settings", Context.MODE_PRIVATE)

    var host: String
        get() = preferences.getString("host", null)
            ?: preferences.getString("base_url", null)?.parseHost()
            ?: "192.168.1.23"
        set(value) {
            preferences.edit().putString("host", value).apply()
        }

    var port: String
        get() = preferences.getString("port", null)
            ?: preferences.getString("base_url", null)?.parsePort()
            ?: "19827"
        set(value) {
            preferences.edit().putString("port", value.filter { it.isDigit() }).apply()
        }

    var token: String
        get() = preferences.getString("token", "").orEmpty()
        set(value) {
            preferences.edit().putString("token", value).apply()
        }

    var deviceId: String
        get() = preferences.getString("device_id", "videosee-phone").orEmpty()
        set(value) {
            preferences.edit().putString("device_id", value).apply()
        }
}

private fun String.parseHost(): String? {
    return runCatching { URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun String.parsePort(): String? {
    return runCatching { URI(this).port }.getOrNull()
        ?.takeIf { it > 0 }
        ?.toString()
}

private data class FavoriteBackup(
    val authorFavoriteLevels: Map<String, Int>,
    val mediaFavoriteLevels: Map<String, Int>,
    val favoriteFolderData: FavoriteFolderData,
    val videoSegmentsByUri: Map<String, List<VideoSegment>>,
)

private object FavoriteBackupJson {
    fun encode(
        authorFavoriteLevels: Map<String, Int>,
        mediaFavoriteLevels: Map<String, Int>,
        favoriteFolderData: FavoriteFolderData,
        videoSegmentsByUri: Map<String, List<VideoSegment>>,
    ): String {
        return JSONObject()
            .put("version", 3)
            .put("authors", authorFavoriteLevels.toJsonObject())
            .put("media", mediaFavoriteLevels.toJsonObject())
            .put("favoriteFolders", FavoriteFolderBackupJson.encode(favoriteFolderData))
            .put("videoSegments", VideoSegmentBackupJson.encode(videoSegmentsByUri))
            .toString(2)
    }

    fun decode(json: String): FavoriteBackup {
        val root = JSONObject(json)
        return FavoriteBackup(
            authorFavoriteLevels = root.optJSONObject("authors").toFavoriteLevelMap(),
            mediaFavoriteLevels = root.optJSONObject("media").toFavoriteLevelMap(),
            favoriteFolderData = root.optJSONObject("favoriteFolders")
                ?.let { FavoriteFolderBackupJson.decode(it) }
                ?: FavoriteFolderData(emptyList(), emptyMap(), null),
            videoSegmentsByUri = root.optJSONObject("videoSegments")
                ?.let { VideoSegmentBackupJson.decode(it) }
                ?: emptyMap(),
        )
    }

    private fun Map<String, Int>.toJsonObject(): JSONObject {
        val json = JSONObject()
        toSortedMap().forEach { (id, level) ->
            val normalizedLevel = level.coerceIn(0, MAX_FAVORITE_LEVEL)
            if (normalizedLevel > 0) {
                json.put(id, normalizedLevel)
            }
        }
        return json
    }

    private fun JSONObject?.toFavoriteLevelMap(): Map<String, Int> {
        val json = this ?: return emptyMap()
        val favorites = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val level = json.optInt(key, 0).coerceIn(0, MAX_FAVORITE_LEVEL)
            if (level > 0) {
                favorites[key] = level
            }
        }
        return favorites
    }
}

private object FavoriteFolderBackupJson {
    fun encode(data: FavoriteFolderData): JSONObject {
        val mediaUris = JSONObject()
        data.mediaUrisByFolderId.forEach { (folderId, uris) ->
            if (data.folders.any { it.id == folderId } && uris.isNotEmpty()) {
                mediaUris.put(folderId, JSONArray(uris.sorted()))
            }
        }
        return JSONObject()
            .put("folders", JSONArray().apply {
                data.folders.forEach { folder -> put(JSONObject().put("id", folder.id).put("name", folder.name)) }
            })
            .put("mediaUris", mediaUris)
            .put("defaultFolderId", data.defaultFolderId)
    }

    fun decode(root: JSONObject): FavoriteFolderData {
        val foldersJson = root.optJSONArray("folders") ?: JSONArray()
        val folders: List<FavoriteFolder> = buildList {
            for (index in 0 until foldersJson.length()) {
                val folderJson = foldersJson.optJSONObject(index) ?: continue
                val id = folderJson.optString("id").trim()
                val name = folderJson.optString("name").trim()
                if (id.isNotBlank() && name.isNotBlank() && none { folder -> folder.id == id }) {
                    add(FavoriteFolder(id, name))
                }
            }
        }
        val mediaUrisJson = root.optJSONObject("mediaUris") ?: JSONObject()
        val mediaUrisByFolderId = buildMap {
            folders.forEach { folder ->
                val urisJson = mediaUrisJson.optJSONArray(folder.id) ?: return@forEach
                val uris = buildSet {
                    for (index in 0 until urisJson.length()) {
                        urisJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                if (uris.isNotEmpty()) put(folder.id, uris)
            }
        }
        val defaultFolderId = root.optString("defaultFolderId").takeIf { id -> folders.any { it.id == id } }
        return FavoriteFolderData(folders, mediaUrisByFolderId, defaultFolderId)
    }
}

private object VideoSegmentBackupJson {
    fun encode(segmentsByUri: Map<String, List<VideoSegment>>): JSONObject {
        val root = JSONObject()
        segmentsByUri.toSortedMap().forEach { (uri, segments) ->
            val normalizedSegments = segments
                .filter { it.endMillis > it.startMillis && it.startMillis >= 0L }
                .distinct()
                .sortedBy { it.startMillis }
            if (normalizedSegments.isNotEmpty()) {
                root.put(uri, JSONArray().apply {
                    normalizedSegments.forEach { segment ->
                        put(
                            JSONObject()
                                .put("start", segment.startMillis)
                                .put("end", segment.endMillis)
                                .apply {
                                    segment.name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                                },
                        )
                    }
                })
            }
        }
        return root
    }

    fun decode(root: JSONObject): Map<String, List<VideoSegment>> {
        val result = mutableMapOf<String, List<VideoSegment>>()
        val uris = root.keys()
        while (uris.hasNext()) {
            val uri = uris.next()
            val segmentsJson = root.optJSONArray(uri) ?: continue
            val segments = buildList {
                for (index in 0 until segmentsJson.length()) {
                    val segmentJson = segmentsJson.optJSONObject(index) ?: continue
                    val start = segmentJson.optLong("start", -1L)
                    val end = segmentJson.optLong("end", -1L)
                    val name = segmentJson.optString("name").trim().takeIf { it.isNotEmpty() }
                    if (start >= 0L && end > start) add(VideoSegment(start, end, name))
                }
            }.distinct().sortedBy { it.startMillis }
            if (segments.isNotEmpty()) result[uri] = segments
        }
        return result
    }
}

private data class AppBackup(
    val authorFavoriteLevels: Map<String, Int>,
    val mediaFavoriteLevels: Map<String, Int>,
    val tagData: TagData,
    val favoriteFolderData: FavoriteFolderData,
    val videoSegmentsByUri: Map<String, List<VideoSegment>>,
)

private object AppBackupJson {
    fun encode(
        authorFavoriteLevels: Map<String, Int>,
        mediaFavoriteLevels: Map<String, Int>,
        tagData: TagData,
        favoriteFolderData: FavoriteFolderData,
        videoSegmentsByUri: Map<String, List<VideoSegment>>,
    ): String {
        return JSONObject()
            .put("version", 4)
            .put("favorites", JSONObject(FavoriteBackupJson.encode(authorFavoriteLevels, mediaFavoriteLevels, favoriteFolderData, videoSegmentsByUri)))
            .put("tags", JSONObject(TagBackupJson.encode(tagData)))
            .toString(2)
    }

    fun decode(json: String): AppBackup {
        val root = JSONObject(json)
        val favorites = root.optJSONObject("favorites") ?: root
        val favoriteBackup = FavoriteBackupJson.decode(favorites.toString())
        val tagData = root.optJSONObject("tags")
            ?.let { TagBackupJson.decode(it.toString()) }
            ?: TagBackupJson.decode(json)
        return AppBackup(
            authorFavoriteLevels = favoriteBackup.authorFavoriteLevels,
            mediaFavoriteLevels = favoriteBackup.mediaFavoriteLevels,
            tagData = tagData,
            favoriteFolderData = favoriteBackup.favoriteFolderData,
            videoSegmentsByUri = favoriteBackup.videoSegmentsByUri,
        )
    }
}

private object TagBackupJson {
    fun encode(tagData: TagData): String {
        val mediaTags = JSONObject()
        tagData.mediaTags.toSortedMap().forEach { (uri, tags) ->
            mediaTags.put(uri, JSONArray(tags.sorted()))
        }
        return JSONObject()
            .put("version", 1)
            .put("tagNames", JSONArray(tagData.tagNames.distinct()))
            .put("mediaTags", mediaTags)
            .toString(2)
    }

    fun decode(json: String): TagData {
        val root = JSONObject(json)
        val tagNamesJson = root.optJSONArray("tagNames") ?: JSONArray()
        val tagNames = buildList {
            for (index in 0 until tagNamesJson.length()) {
                val name = tagNamesJson.optString(index).trim()
                if (name.isNotBlank() && name !in this) add(name)
            }
        }
        val mediaTagsJson = root.optJSONObject("mediaTags") ?: JSONObject()
        val mediaTags = mutableMapOf<String, Set<String>>()
        val keys = mediaTagsJson.keys()
        while (keys.hasNext()) {
            val uri = keys.next()
            val tagsJson = mediaTagsJson.optJSONArray(uri) ?: continue
            val tags = buildSet {
                for (index in 0 until tagsJson.length()) {
                    val name = tagsJson.optString(index).trim()
                    if (name.isNotBlank()) add(name)
                }
            }
            if (tags.isNotEmpty()) {
                mediaTags[uri] = tags
            }
        }
        val allTagNames = (tagNames + mediaTags.values.flatten()).distinct()
        return TagData(tagNames = allTagNames, mediaTags = mediaTags)
    }
}

private const val MAX_FAVORITE_LEVEL = 3
private const val FAVORITE_FOLDER_PREFIX = "favorite-folder:"
private val FAVORITE_FOLDER_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private const val SHUFFLE_ORDER_RESET_DELAY_MILLIS = 10 * 60 * 1_000L
private const val RECENT_PLAYBACK_COLLECTION_ID = "recent-playback"
private const val MAX_RECENT_PLAYBACK_ITEMS = 1_000
