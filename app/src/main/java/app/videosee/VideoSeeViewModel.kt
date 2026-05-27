package app.videosee

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.videosee.data.MediaRepository
import app.videosee.domain.MediaFolder
import app.videosee.domain.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoSeeUiState(
    val folders: List<MediaFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val viewerIndex: Int? = null,
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedFolder: MediaFolder?
        get() = folders.firstOrNull { it.id == selectedFolderId } ?: folders.firstOrNull()

    val selectedItems: List<MediaItem>
        get() = selectedFolder?.items.orEmpty()

    val viewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it) }

    val previousViewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it - 1) }

    val nextViewerItem: MediaItem?
        get() = viewerIndex?.let { selectedItems.getOrNull(it + 1) }
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadFolders() }
                .onSuccess { folders ->
                    _uiState.update { state ->
                        val selectedId = state.selectedFolderId
                            ?.takeIf { id -> folders.any { it.id == id } }
                            ?: folders.firstOrNull()?.id
                        state.copy(
                            folders = folders,
                            selectedFolderId = selectedId,
                            viewerIndex = null,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
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
