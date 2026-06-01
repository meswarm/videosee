package app.videosee

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.videosee.data.MediaRepository
import app.videosee.data.MobileSyncRepository
import app.videosee.data.SyncPendingFile
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class VideoSeeUiState(
    val folders: List<MediaFolder> = emptyList(),
    val authors: List<MediaFolder> = emptyList(),
    val tags: List<String> = emptyList(),
    val mediaTags: Map<String, Set<String>> = emptyMap(),
    val tagFolders: List<MediaFolder> = emptyList(),
    val authorFavoriteLevels: Map<String, Int> = emptyMap(),
    val mediaFavoriteLevels: Map<String, Int> = emptyMap(),
    val browserMode: BrowserMode = BrowserMode.Folder,
    val collectionSortField: CollectionSortField = CollectionSortField.ModifiedTime,
    val collectionSortDirection: SortDirection = SortDirection.Descending,
    val mediaSortField: MediaSortField = MediaSortField.ModifiedTime,
    val mediaSortDirection: SortDirection = SortDirection.Descending,
    val favoriteMediaSortUriOrder: List<String> = emptyList(),
    val collectionSearchQuery: String = "",
    val selectedFolderId: String? = null,
    val selectedAuthorId: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
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
    val videoThumbnailPaths: Map<String, String> = emptyMap(),
) {
    val selectedFolder: MediaFolder?
        get() = folders.firstOrNull { it.id == selectedFolderId } ?: folders.firstOrNull()

    val selectedAuthor: MediaFolder?
        get() = authors.firstOrNull { it.id == selectedAuthorId } ?: authors.firstOrNull()

    val visibleCollections: List<MediaFolder>
        get() = CollectionSearch.filterByName(
            collections = MediaSort.sortCollections(
                collections = when (browserMode) {
                    BrowserMode.Folder -> folders
                    BrowserMode.Author -> authors
                    BrowserMode.Tag -> tagFolders
                },
                field = collectionSortField.takeIf { browserMode == BrowserMode.Author || it != CollectionSortField.FavoriteLevel }
                    ?: CollectionSortField.ModifiedTime,
                direction = collectionSortDirection,
            ),
            query = collectionSearchQuery,
        )

    val selectedCollection: MediaFolder?
        get() = when (browserMode) {
            BrowserMode.Folder -> selectedFolder
            BrowserMode.Author -> selectedAuthor
            BrowserMode.Tag -> selectedTagCollection
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

    val selectedItems: List<MediaItem>
        get() {
            val items = selectedCollection?.items.orEmpty()
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
}

enum class RightPaneMode {
    Browser,
    Sync,
    Backup,
}

class VideoSeeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val syncRepository = MobileSyncRepository(application)
    private val thumbnailCacheRepository = VideoThumbnailCacheRepository(application)
    private val syncSettingsStore = SyncSettingsStore(application)
    private val authorFavoriteStore = FavoriteLevelStore(application, "author_favorite_levels")
    private val mediaFavoriteStore = FavoriteLevelStore(application, "media_favorite_levels")
    private val tagStore = TagDataStore(application)
    private val thumbnailRequestsInFlight = mutableSetOf<String>()
    private val _uiState = MutableStateFlow(
        VideoSeeUiState(
            authorFavoriteLevels = authorFavoriteStore.load(),
            mediaFavoriteLevels = mediaFavoriteStore.load(),
            tags = tagStore.load().tagNames,
            mediaTags = tagStore.load().mediaTags,
            syncHost = syncSettingsStore.host,
            syncPort = syncSettingsStore.port,
            syncToken = syncSettingsStore.token,
            syncDeviceId = syncSettingsStore.deviceId,
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
                            ?.takeIf { id -> library.folders.any { it.id == id } }
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
        )
    }

    fun importFavoritesBackupJson(json: String): FavoriteBackupImportResult {
        val backup = FavoriteBackupJson.decode(json)
        authorFavoriteStore.save(backup.authorFavoriteLevels)
        mediaFavoriteStore.save(backup.mediaFavoriteLevels)
        _uiState.update { state ->
            state.copy(
                authorFavoriteLevels = backup.authorFavoriteLevels,
                mediaFavoriteLevels = backup.mediaFavoriteLevels,
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
        _uiState.update { state ->
            state.copy(
                authorFavoriteLevels = backup.authorFavoriteLevels,
                mediaFavoriteLevels = backup.mediaFavoriteLevels,
                tags = backup.tagData.tagNames,
                mediaTags = backup.tagData.mediaTags,
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
        _uiState.update { state ->
            val index = state.selectedItems.indexOfFirst { it.uri == item.uri }
            state.copy(
                viewerIndex = index.takeIf { it >= 0 },
                gridReturnTargetUri = null,
            )
        }
    }

    fun closeViewer() {
        _uiState.update { state ->
            state.copy(
                viewerIndex = null,
                gridReturnTargetUri = state.viewerItem?.uri,
            )
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
)

private object FavoriteBackupJson {
    fun encode(
        authorFavoriteLevels: Map<String, Int>,
        mediaFavoriteLevels: Map<String, Int>,
    ): String {
        return JSONObject()
            .put("version", 1)
            .put("authors", authorFavoriteLevels.toJsonObject())
            .put("media", mediaFavoriteLevels.toJsonObject())
            .toString(2)
    }

    fun decode(json: String): FavoriteBackup {
        val root = JSONObject(json)
        return FavoriteBackup(
            authorFavoriteLevels = root.optJSONObject("authors").toFavoriteLevelMap(),
            mediaFavoriteLevels = root.optJSONObject("media").toFavoriteLevelMap(),
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

private data class AppBackup(
    val authorFavoriteLevels: Map<String, Int>,
    val mediaFavoriteLevels: Map<String, Int>,
    val tagData: TagData,
)

private object AppBackupJson {
    fun encode(
        authorFavoriteLevels: Map<String, Int>,
        mediaFavoriteLevels: Map<String, Int>,
        tagData: TagData,
    ): String {
        return JSONObject()
            .put("version", 2)
            .put("favorites", JSONObject(FavoriteBackupJson.encode(authorFavoriteLevels, mediaFavoriteLevels)))
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
