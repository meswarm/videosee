package app.videosee

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.videosee.data.MediaRepository
import app.videosee.domain.CollectionSortField
import app.videosee.domain.MediaFolder
import app.videosee.domain.MediaItem
import app.videosee.domain.MediaSort
import app.videosee.domain.MediaSortField
import app.videosee.domain.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoSeeUiState(
    val folders: List<MediaFolder> = emptyList(),
    val authors: List<MediaFolder> = emptyList(),
    val browserMode: BrowserMode = BrowserMode.Folder,
    val collectionSortField: CollectionSortField = CollectionSortField.ModifiedTime,
    val collectionSortDirection: SortDirection = SortDirection.Descending,
    val mediaSortField: MediaSortField = MediaSortField.ModifiedTime,
    val mediaSortDirection: SortDirection = SortDirection.Descending,
    val selectedFolderId: String? = null,
    val selectedAuthorId: String? = null,
    val viewerIndex: Int? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasPermission: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedFolder: MediaFolder?
        get() = folders.firstOrNull { it.id == selectedFolderId } ?: folders.firstOrNull()

    val selectedAuthor: MediaFolder?
        get() = authors.firstOrNull { it.id == selectedAuthorId } ?: authors.firstOrNull()

    val visibleCollections: List<MediaFolder>
        get() = MediaSort.sortCollections(
            collections = when (browserMode) {
            BrowserMode.Folder -> folders
            BrowserMode.Author -> authors
            },
            field = collectionSortField,
            direction = collectionSortDirection,
        )

    val selectedCollection: MediaFolder?
        get() = when (browserMode) {
            BrowserMode.Folder -> selectedFolder
            BrowserMode.Author -> selectedAuthor
        }

    val selectedItems: List<MediaItem>
        get() = MediaSort.sortItems(
            items = selectedCollection?.items.orEmpty(),
            field = mediaSortField,
            direction = mediaSortDirection,
        )

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
}

class VideoSeeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(VideoSeeUiState())
    val uiState: StateFlow<VideoSeeUiState> = _uiState.asStateFlow()

    fun onPermissionChanged(hasPermission: Boolean) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
        if (hasPermission) {
            refresh()
        }
    }

    fun refresh() {
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
                        state.copy(
                            folders = library.folders,
                            authors = library.authors,
                            browserMode = browserMode,
                            selectedFolderId = selectedFolderId,
                            selectedAuthorId = selectedAuthorId,
                            viewerIndex = null,
                            isLoading = false,
                            isRefreshing = false,
                        )
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
        _uiState.update {
            it.copy(selectedFolderId = folderId, viewerIndex = null)
        }
    }

    fun selectAuthor(authorId: String) {
        _uiState.update {
            it.copy(selectedAuthorId = authorId, viewerIndex = null)
        }
    }

    fun selectBrowserMode(mode: BrowserMode) {
        _uiState.update {
            it.copy(browserMode = mode, viewerIndex = null)
        }
    }

    fun selectCollectionSortField(field: CollectionSortField) {
        _uiState.update {
            it.copy(collectionSortField = field, viewerIndex = null)
        }
    }

    fun toggleCollectionSortDirection() {
        _uiState.update {
            it.copy(
                collectionSortDirection = it.collectionSortDirection.toggle(),
                viewerIndex = null,
            )
        }
    }

    fun selectMediaSortField(field: MediaSortField) {
        _uiState.update {
            it.copy(mediaSortField = field, viewerIndex = null)
        }
    }

    fun toggleMediaSortDirection() {
        _uiState.update {
            it.copy(
                mediaSortDirection = it.mediaSortDirection.toggle(),
                viewerIndex = null,
            )
        }
    }

    fun openViewer(item: MediaItem) {
        _uiState.update { state ->
            val index = state.selectedItems.indexOfFirst { it.uri == item.uri }
            state.copy(viewerIndex = index.takeIf { it >= 0 })
        }
    }

    fun closeViewer() {
        _uiState.update { it.copy(viewerIndex = null) }
    }

    fun showNext() {
        _uiState.update { state ->
            val current = state.viewerIndex ?: return@update state
            val next = (current + 1).coerceAtMost(state.selectedItems.lastIndex)
            state.copy(viewerIndex = next)
        }
    }

    fun showPrevious() {
        _uiState.update { state ->
            val current = state.viewerIndex ?: return@update state
            val previous = (current - 1).coerceAtLeast(0)
            state.copy(viewerIndex = previous)
        }
    }
}

private fun SortDirection.toggle(): SortDirection {
    return when (this) {
        SortDirection.Ascending -> SortDirection.Descending
        SortDirection.Descending -> SortDirection.Ascending
    }
}
