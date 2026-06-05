package app.videosee

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem.fromUri
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.videosee.domain.MediaFolder
import app.videosee.domain.MediaItem
import app.videosee.domain.MediaType
import app.videosee.domain.CollectionSortField
import app.videosee.domain.MediaSortField
import app.videosee.domain.SortDirection
import app.videosee.data.SyncPendingFile
import app.videosee.ui.PlaybackSpeedOptions
import app.videosee.ui.GridReturnFocus
import app.videosee.ui.PlaybackTimeFormatter
import app.videosee.ui.SwipeIntent
import app.videosee.ui.SwipeTransitionSpec
import app.videosee.ui.VideoSeekGesture
import app.videosee.ui.VideoSnapshotFileName
import app.videosee.ui.ViewerUiSpec
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoSeeTheme {
                VideoSeeRoute()
            }
        }
    }
}

@Composable
private fun VideoSeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF120F1A),
            surface = Color(0xFF181321),
            surfaceVariant = Color(0xFF241D31),
            primary = Color(0xFF3DDC84),
            onBackground = Color(0xFFE8EAED),
            onSurface = Color(0xFFE8EAED),
            onSurfaceVariant = Color(0xFFAEB4BD),
        ),
        content = content,
    )
}

@Composable
private fun VideoSeeRoute(viewModel: VideoSeeViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    var selectedMediaUrisForDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingViewerDeleteTargetUri by remember { mutableStateOf<String?>(null) }
    var backupTransferKind by remember { mutableStateOf("all") }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionChanged(context.hasMediaPermission())
    }
    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "已删除 ${pendingDeleteUris.size} 个媒体", Toast.LENGTH_SHORT).show()
            selectedMediaUrisForDelete = emptySet()
            val targetUri = pendingViewerDeleteTargetUri
            if (targetUri != null) {
                viewModel.refreshKeepingViewer(targetUri)
            } else {
                viewModel.refresh()
            }
        }
        pendingDeleteUris = emptySet()
        pendingViewerDeleteTargetUri = null
    }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val json = when (backupTransferKind) {
                    "favorites" -> viewModel.exportFavoritesBackupJson()
                    "tags" -> viewModel.exportTagsBackupJson()
                    else -> viewModel.exportAllBackupJson()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)
                        ?.bufferedWriter()
                        ?.use { it.write(json) }
                        ?: error("Cannot open backup file")
                }
            }.onSuccess {
                Toast.makeText(context, "数据已导出", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "导出失败: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Cannot open backup file")
                }
                when (backupTransferKind) {
                    "favorites" -> viewModel.importFavoritesBackupJson(json)
                    "tags" -> viewModel.importTagsBackupJson(json)
                    else -> viewModel.importAllBackupJson(json)
                }
            }.onSuccess { result ->
                Toast.makeText(
                    context,
                    "已导入数据",
                    Toast.LENGTH_LONG,
                ).show()
            }.onFailure { error ->
                Toast.makeText(context, "导入失败: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (context.hasMediaPermission()) {
            viewModel.onPermissionChanged(true)
        } else {
            launcher.launch(requiredPermissions())
        }
    }

    VideoSeeApp(
        state = state,
        onRequestPermission = { launcher.launch(requiredPermissions()) },
        onSelectFolder = viewModel::selectFolder,
        onSelectAuthor = viewModel::selectAuthor,
        onSelectTag = viewModel::selectTag,
        onSelectBrowserMode = viewModel::selectBrowserMode,
        onCollectionSearchQueryChange = viewModel::updateCollectionSearchQuery,
        onSelectCollectionSortField = viewModel::selectCollectionSortField,
        onToggleCollectionSortDirection = viewModel::toggleCollectionSortDirection,
        onSetAuthorFavoriteLevel = viewModel::setAuthorFavoriteLevel,
        onSelectMediaSortField = viewModel::selectMediaSortField,
        onToggleMediaSortDirection = viewModel::toggleMediaSortDirection,
        onSetMediaFavoriteLevel = viewModel::setMediaFavoriteLevel,
        onEnsureVideoThumbnail = viewModel::ensureVideoThumbnail,
        onOpenItem = viewModel::openViewer,
        onCloseViewer = viewModel::closeViewer,
        onNext = viewModel::showNext,
        onPrevious = viewModel::showPrevious,
        onFirst = viewModel::showFirst,
        onLast = viewModel::showLast,
        onRefresh = viewModel::refresh,
        onOpenSync = viewModel::openSyncPane,
        onOpenBackup = viewModel::openBackupPane,
        onSyncHostChange = viewModel::updateSyncHost,
        onSyncPortChange = viewModel::updateSyncPort,
        onSyncTokenChange = viewModel::updateSyncToken,
        onSyncDeviceIdChange = viewModel::updateSyncDeviceId,
        onLoadSyncPendingFiles = viewModel::loadSyncPendingFiles,
        onDownloadSyncFile = viewModel::downloadSyncFile,
        onDownloadAllSyncFiles = viewModel::downloadAllSyncFiles,
        onAddTag = viewModel::addTag,
        onDeleteTag = viewModel::deleteTag,
        onToggleMediaTag = viewModel::toggleMediaTag,
        onGridReturnTargetHandled = viewModel::clearGridReturnTarget,
        selectedMediaUrisForDelete = selectedMediaUrisForDelete,
        onSelectedMediaUrisForDeleteChange = { selectedMediaUrisForDelete = it },
        onDeleteMediaUris = { uris ->
            if (uris.isEmpty()) return@VideoSeeApp
            pendingViewerDeleteTargetUri = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pendingDeleteUris = uris
                val request = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    uris.map { Uri.parse(it) },
                )
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                coroutineScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        uris.count { context.contentResolver.delete(Uri.parse(it), null, null) > 0 }
                    }
                    Toast.makeText(context, "已删除 $deleted 个媒体", Toast.LENGTH_SHORT).show()
                    pendingDeleteUris = emptySet()
                    selectedMediaUrisForDelete = emptySet()
                    viewModel.refresh()
                }
            }
        },
        onDeleteViewerMedia = { uri, targetUri ->
            pendingViewerDeleteTargetUri = targetUri
            val uris = setOf(uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pendingDeleteUris = uris
                val request = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    uris.map { Uri.parse(it) },
                )
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                coroutineScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        context.contentResolver.delete(Uri.parse(uri), null, null)
                    }
                    Toast.makeText(context, "已删除 $deleted 个媒体", Toast.LENGTH_SHORT).show()
                    pendingDeleteUris = emptySet()
                    pendingViewerDeleteTargetUri = null
                    viewModel.refreshKeepingViewer(targetUri)
                }
            }
        },
        onExportBackup = { kind ->
            backupTransferKind = kind
            exportLauncher.launch("videosee-$kind-backup.json")
        },
        onImportBackup = { kind ->
            backupTransferKind = kind
            importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        },
    )
}

@Composable
private fun VideoSeeApp(
    state: VideoSeeUiState,
    onRequestPermission: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectAuthor: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    onCollectionSearchQueryChange: (String) -> Unit,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSetAuthorFavoriteLevel: (String, Int) -> Unit,
    onSelectMediaSortField: (MediaSortField) -> Unit,
    onToggleMediaSortDirection: () -> Unit,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    onEnsureVideoThumbnail: (MediaItem) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onCloseViewer: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onSyncHostChange: (String) -> Unit,
    onSyncPortChange: (String) -> Unit,
    onSyncTokenChange: (String) -> Unit,
    onSyncDeviceIdChange: (String) -> Unit,
    onLoadSyncPendingFiles: () -> Unit,
    onDownloadSyncFile: (SyncPendingFile) -> Unit,
    onDownloadAllSyncFiles: () -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onToggleMediaTag: (String, String) -> Unit,
    onGridReturnTargetHandled: (String) -> Unit,
    selectedMediaUrisForDelete: Set<String>,
    onSelectedMediaUrisForDeleteChange: (Set<String>) -> Unit,
    onDeleteMediaUris: (Set<String>) -> Unit,
    onDeleteViewerMedia: (String, String?) -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackup: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val contentState = when {
            !state.hasPermission -> AppContentState.Permission
            state.isLoading -> AppContentState.Loading
            state.folders.isEmpty() -> AppContentState.Empty
            else -> AppContentState.Browser
        }

        Crossfade(
            targetState = contentState,
            animationSpec = tween(ViewerUiSpec.SCREEN_TRANSITION_DURATION_MILLIS),
            label = "app_content_transition",
        ) { target ->
            when (target) {
                AppContentState.Permission -> PermissionScreen(onRequestPermission)
                AppContentState.Loading -> LoadingScreen()
                AppContentState.Empty -> EmptyScreen("No images or videos found")
                AppContentState.Browser -> BrowserScreen(
                    state = state,
            onSelectFolder = onSelectFolder,
            onSelectAuthor = onSelectAuthor,
                    onSelectTag = onSelectTag,
                    onSelectBrowserMode = onSelectBrowserMode,
                    onCollectionSearchQueryChange = onCollectionSearchQueryChange,
                    onSelectCollectionSortField = onSelectCollectionSortField,
                    onToggleCollectionSortDirection = onToggleCollectionSortDirection,
                    onSetAuthorFavoriteLevel = onSetAuthorFavoriteLevel,
                    onSelectMediaSortField = onSelectMediaSortField,
                    onToggleMediaSortDirection = onToggleMediaSortDirection,
                    onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
                    onEnsureVideoThumbnail = onEnsureVideoThumbnail,
                    onOpenItem = onOpenItem,
                    onRefresh = onRefresh,
                    onOpenSync = onOpenSync,
                    onOpenBackup = onOpenBackup,
                    onSyncHostChange = onSyncHostChange,
                    onSyncPortChange = onSyncPortChange,
                    onSyncTokenChange = onSyncTokenChange,
                    onSyncDeviceIdChange = onSyncDeviceIdChange,
                    onLoadSyncPendingFiles = onLoadSyncPendingFiles,
                    onDownloadSyncFile = onDownloadSyncFile,
                    onDownloadAllSyncFiles = onDownloadAllSyncFiles,
                    onAddTag = onAddTag,
                    onDeleteTag = onDeleteTag,
                    onGridReturnTargetHandled = onGridReturnTargetHandled,
                    selectedMediaUrisForDelete = selectedMediaUrisForDelete,
                    onSelectedMediaUrisForDeleteChange = onSelectedMediaUrisForDeleteChange,
                    onDeleteMediaUris = onDeleteMediaUris,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                )
            }
        }

        state.viewerItem?.uri?.let { uri ->
            val item = state.selectedItems.firstOrNull { media -> media.uri == uri } ?: return@let
            val viewerIndex = state.selectedItems.indexOfFirst { media -> media.uri == uri }
            val viewerDeleteTargetUri = state.selectedItems.getOrNull(viewerIndex + 1)?.uri
                ?: state.selectedItems.getOrNull(viewerIndex - 1)?.uri
            MediaViewer(
                item = item,
                hasPrevious = viewerIndex > 0,
                hasNext = viewerIndex >= 0 && viewerIndex < state.selectedItems.lastIndex,
                onClose = onCloseViewer,
                onNext = onNext,
                onPrevious = onPrevious,
                onFirst = onFirst,
                onLast = onLast,
                onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
                allTags = state.tags,
                selectedTags = state.mediaTags[item.uri].orEmpty(),
                onToggleMediaTag = { tagName -> onToggleMediaTag(item.uri, tagName) },
                onDeleteCurrentMedia = { onDeleteViewerMedia(item.uri, viewerDeleteTargetUri) },
            )
        }
    }
}

private enum class AppContentState {
    Permission,
    Loading,
    Empty,
    Browser,
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("VideoSee needs media access", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Grant access to show local image and video folders.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant access")
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BrowserScreen(
    state: VideoSeeUiState,
    onSelectFolder: (String) -> Unit,
    onSelectAuthor: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    onCollectionSearchQueryChange: (String) -> Unit,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSetAuthorFavoriteLevel: (String, Int) -> Unit,
    onSelectMediaSortField: (MediaSortField) -> Unit,
    onToggleMediaSortDirection: () -> Unit,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    onEnsureVideoThumbnail: (MediaItem) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onRefresh: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onSyncHostChange: (String) -> Unit,
    onSyncPortChange: (String) -> Unit,
    onSyncTokenChange: (String) -> Unit,
    onSyncDeviceIdChange: (String) -> Unit,
    onLoadSyncPendingFiles: () -> Unit,
    onDownloadSyncFile: (SyncPendingFile) -> Unit,
    onDownloadAllSyncFiles: () -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onGridReturnTargetHandled: (String) -> Unit,
    selectedMediaUrisForDelete: Set<String>,
    onSelectedMediaUrisForDeleteChange: (Set<String>) -> Unit,
    onDeleteMediaUris: (Set<String>) -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackup: (String) -> Unit,
) {
    val collections = state.visibleCollections
    val selectedCollection = state.selectedCollection
    val selectedCollectionId = selectedCollection?.id
    val selectedCollectionIds = when (state.browserMode) {
        BrowserMode.Tag -> state.selectedTagIds.ifEmpty { selectedCollection?.id?.let { setOf(it) }.orEmpty() }
        else -> selectedCollection?.id?.let { setOf(it) }.orEmpty()
    }
    val onSelectCollection = when (state.browserMode) {
        BrowserMode.Folder -> onSelectFolder
        BrowserMode.Author -> onSelectAuthor
        BrowserMode.Tag -> onSelectTag
    }

    Row(Modifier.fillMaxSize()) {
        FolderRail(
            collections = collections,
            selectedCollectionIds = selectedCollectionIds,
            browserMode = state.browserMode,
            hasAuthors = state.authors.isNotEmpty(),
            hasTags = state.tags.isNotEmpty(),
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            onSelectBrowserMode = onSelectBrowserMode,
            searchQuery = state.collectionSearchQuery,
            onSearchQueryChange = onCollectionSearchQueryChange,
            collectionSortField = state.collectionSortField,
            collectionSortDirection = state.collectionSortDirection,
            onSelectCollectionSortField = onSelectCollectionSortField,
            onToggleCollectionSortDirection = onToggleCollectionSortDirection,
            onSetAuthorFavoriteLevel = onSetAuthorFavoriteLevel,
            onSelectCollection = onSelectCollection,
            onOpenBackup = onOpenBackup,
            onOpenSync = onOpenSync,
            modifier = Modifier.width(214.dp).fillMaxHeight(),
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF2B2238)),
        )
        Crossfade(
            targetState = state.rightPaneMode to selectedCollectionId,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            animationSpec = tween(ViewerUiSpec.FOLDER_SWITCH_DURATION_MILLIS),
            label = "media_collection_switch",
        ) { (rightPaneMode, collectionId) ->
            when (rightPaneMode) {
                RightPaneMode.Sync -> SyncScreen(
                    host = state.syncHost,
                    port = state.syncPort,
                    token = state.syncToken,
                    deviceId = state.syncDeviceId,
                    pendingFiles = state.syncPendingFiles,
                    downloadingIds = state.syncDownloadingIds,
                    isLoading = state.syncIsLoading,
                    message = state.syncMessage,
                    onHostChange = onSyncHostChange,
                    onPortChange = onSyncPortChange,
                    onTokenChange = onSyncTokenChange,
                    onDeviceIdChange = onSyncDeviceIdChange,
                    onRefresh = onLoadSyncPendingFiles,
                    onDownload = onDownloadSyncFile,
                    onDownloadAll = onDownloadAllSyncFiles,
                    modifier = Modifier.fillMaxSize(),
                )
                RightPaneMode.Backup -> BackupScreen(
                    tags = state.tags,
                    onAddTag = onAddTag,
                    onDeleteTag = onDeleteTag,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    modifier = Modifier.fillMaxSize(),
                )
                RightPaneMode.Browser -> if (collectionId == null || selectedCollection == null) {
                    EmptyScreen("No media found")
                } else {
                    Column(Modifier.fillMaxHeight()) {
                        Box(Modifier.fillMaxWidth()) {
                            MediaSortToolbar(
                                sortField = state.mediaSortField,
                                sortDirection = state.mediaSortDirection,
                                onSelectSortField = onSelectMediaSortField,
                                onToggleSortDirection = onToggleMediaSortDirection,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (selectedMediaUrisForDelete.isNotEmpty()) {
                                DeleteSelectedButton(
                                    count = selectedMediaUrisForDelete.size,
                                    onClick = { onDeleteMediaUris(selectedMediaUrisForDelete) },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp),
                                )
                            }
                        }
                        MediaGrid(
                            items = state.selectedItems,
                            onOpenItem = onOpenItem,
                            onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
                            onEnsureVideoThumbnail = onEnsureVideoThumbnail,
                            videoThumbnailPaths = state.videoThumbnailPaths,
                            returnTargetUri = state.gridReturnTargetUri,
                            onReturnTargetHandled = onGridReturnTargetHandled,
                            selectedMediaUrisForDelete = selectedMediaUrisForDelete,
                            onSelectedMediaUrisForDeleteChange = onSelectedMediaUrisForDeleteChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSortToolbar(
    sortField: MediaSortField,
    sortDirection: SortDirection,
    onSelectSortField: (MediaSortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SortChip(
            text = "名称",
            selected = sortField == MediaSortField.Name,
            onClick = { onSelectSortField(MediaSortField.Name) },
        )
        SortChip(
            text = "最新",
            selected = sortField == MediaSortField.ModifiedTime,
            onClick = { onSelectSortField(MediaSortField.ModifiedTime) },
        )
        SortChip(
            text = "爱心",
            selected = sortField == MediaSortField.FavoriteLevel,
            onClick = { onSelectSortField(MediaSortField.FavoriteLevel) },
        )
        SortChip(
            text = sortDirection.label(),
            selected = true,
            onClick = onToggleSortDirection,
        )
    }
}

@Composable
private fun DeleteSelectedButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "删除 $count",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFD93025))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun CollectionSortToolbar(
    browserMode: BrowserMode,
    sortField: CollectionSortField,
    sortDirection: SortDirection,
    onSelectSortField: (CollectionSortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SortChip(
            text = "名称",
            selected = sortField == CollectionSortField.Name,
            onClick = { onSelectSortField(CollectionSortField.Name) },
            compact = true,
        )
        SortChip(
            text = "数量",
            selected = sortField == CollectionSortField.Count,
            onClick = { onSelectSortField(CollectionSortField.Count) },
            compact = true,
        )
        SortChip(
            text = "最新",
            selected = sortField == CollectionSortField.ModifiedTime,
            onClick = { onSelectSortField(CollectionSortField.ModifiedTime) },
            compact = true,
        )
        if (browserMode == BrowserMode.Author) {
            SortChip(
                text = "爱心",
                selected = sortField == CollectionSortField.FavoriteLevel,
                onClick = { onSelectSortField(CollectionSortField.FavoriteLevel) },
                compact = true,
            )
        }
        SortChip(
            text = sortDirection.label(),
            selected = true,
            onClick = onToggleSortDirection,
            compact = true,
        )
    }
}

@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val background by animateColorAsState(
        targetValue = if (selected) Color(0xFFFF8A00) else Color(0xFF241D31),
        animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
        label = "sort_chip_background",
    )
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = 6.dp),
        color = if (selected) Color(0xFF1A0D00) else Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

private fun SortDirection.label(): String {
    return when (this) {
        SortDirection.Ascending -> "正序"
        SortDirection.Descending -> "倒序"
    }
}

@Composable
private fun FolderRail(
    collections: List<MediaFolder>,
    selectedCollectionIds: Set<String>,
    browserMode: BrowserMode,
    hasAuthors: Boolean,
    hasTags: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    collectionSortField: CollectionSortField,
    collectionSortDirection: SortDirection,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSetAuthorFavoriteLevel: (String, Int) -> Unit,
    onSelectCollection: (String) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Color(0xFF15111F))) {
        FavoritesBackupToolbar(
            onOpenBackup = onOpenBackup,
            onOpenSync = onOpenSync,
            modifier = Modifier.fillMaxWidth(),
        )
        RailDivider()
        FolderToolbar(
            browserMode = browserMode,
            hasAuthors = hasAuthors,
            hasTags = hasTags,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onSelectBrowserMode = onSelectBrowserMode,
            modifier = Modifier.fillMaxWidth(),
        )
        RailDivider()
        CollectionSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        RailDivider()
        CollectionSortToolbar(
            browserMode = browserMode,
            sortField = collectionSortField,
            sortDirection = collectionSortDirection,
            onSelectSortField = onSelectCollectionSortField,
            onToggleSortDirection = onToggleCollectionSortDirection,
            modifier = Modifier.fillMaxWidth(),
        )
        RailDivider()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(collections, key = { it.id }) { collection ->
                val selected = collection.id in selectedCollectionIds
                val rowBackground by animateColorAsState(
                    targetValue = if (selected) Color(0xFF2D2540) else Color.Transparent,
                    animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
                    label = "folder_selection_background",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(rowBackground)
                        .clickable { onSelectCollection(collection.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = videoFrameImageModel(collection.previewItem),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF241D31)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            collection.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${collection.count}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (browserMode == BrowserMode.Author) {
                                Spacer(Modifier.weight(1f))
                                AuthorFavoritePicker(
                                    favoriteLevel = collection.favoriteLevel,
                                    onFavoriteLevelChange = { level ->
                                        onSetAuthorFavoriteLevel(collection.id, level)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF352B42))
                    .border(
                        width = 1.dp,
                        color = Color(0x663DDC84),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "搜索",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Text(
                        "×",
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onQueryChange("") }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFFE8EAED),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    )
}

@Composable
private fun RailDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF2B2238)),
    )
}

@Composable
private fun FavoritesBackupToolbar(
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SortChip(
            text = "数据备份",
            selected = false,
            onClick = onOpenBackup,
            compact = true,
        )
        SortChip(
            text = "下载",
            selected = false,
            onClick = onOpenSync,
            compact = true,
        )
    }
}

@Composable
private fun AuthorFavoritePicker(
    favoriteLevel: Int,
    onFavoriteLevelChange: (Int) -> Unit,
    heartSize: androidx.compose.ui.unit.Dp = 14.dp,
    touchTargetSize: androidx.compose.ui.unit.Dp = 20.dp,
    spacing: androidx.compose.ui.unit.Dp = 6.dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(3) { index ->
            val level = index + 1
            val selected = level <= favoriteLevel
            Box(
                modifier = Modifier
                    .size(touchTargetSize)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onFavoriteLevelChange(if (favoriteLevel == level) 0 else level)
                    },
                contentAlignment = Alignment.Center,
            ) {
                HeartButton(
                    selected = selected,
                    heartSize = heartSize,
                )
            }
        }
    }
}

@Composable
private fun HeartButton(
    selected: Boolean,
    heartSize: androidx.compose.ui.unit.Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(heartSize)) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.88f)
            cubicTo(
                size.width * 0.16f,
                size.height * 0.62f,
                size.width * 0.04f,
                size.height * 0.42f,
                size.width * 0.08f,
                size.height * 0.24f,
            )
            cubicTo(
                size.width * 0.12f,
                size.height * 0.06f,
                size.width * 0.34f,
                size.height * 0.02f,
                size.width * 0.5f,
                size.height * 0.22f,
            )
            cubicTo(
                size.width * 0.66f,
                size.height * 0.02f,
                size.width * 0.88f,
                size.height * 0.06f,
                size.width * 0.92f,
                size.height * 0.24f,
            )
            cubicTo(
                size.width * 0.96f,
                size.height * 0.42f,
                size.width * 0.84f,
                size.height * 0.62f,
                size.width * 0.5f,
                size.height * 0.88f,
            )
            close()
        }
        if (selected) {
            drawPath(path = path, color = Color(0xFFFF3446), style = Fill)
        } else {
            drawPath(
                path = path,
                color = Color(0xFFAEB4BD),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun ViewerFavoritePicker(
    favoriteLevel: Int,
    onFavoriteLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x88000000))
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        AuthorFavoritePicker(
            favoriteLevel = favoriteLevel,
            onFavoriteLevelChange = onFavoriteLevelChange,
            heartSize = 22.dp,
            touchTargetSize = 27.dp,
            spacing = 2.dp,
        )
    }
}

@Composable
private fun ViewerDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "删除",
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x99D93025))
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 10.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ViewerTagRail(
    tags: List<String>,
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    LazyColumn(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight(0.58f),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        items(tags, key = { it }) { tagName ->
            val selected = tagName in selectedTags
            Text(
                tagName,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color(0xCC3DDC84) else Color(0x99000000))
                    .border(
                        width = 1.dp,
                        color = Color(0x66FFFFFF),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onToggleTag(tagName) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                color = if (selected) Color(0xFF06120B) else Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FolderToolbar(
    browserMode: BrowserMode,
    hasAuthors: Boolean,
    hasTags: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BrowserModeButton(
            text = "文件夹",
            selected = browserMode == BrowserMode.Folder,
            enabled = true,
            onClick = { onSelectBrowserMode(BrowserMode.Folder) },
        )
        BrowserModeButton(
            text = "作者",
            selected = browserMode == BrowserMode.Author,
            enabled = hasAuthors,
            onClick = { onSelectBrowserMode(BrowserMode.Author) },
        )
        BrowserModeButton(
            text = "标签",
            selected = browserMode == BrowserMode.Tag,
            enabled = hasTags,
            onClick = { onSelectBrowserMode(BrowserMode.Tag) },
        )
        Text(
            if (isRefreshing) "刷新" else "刷新",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF241D31))
                .clickable(enabled = !isRefreshing) { onRefresh() }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            color = if (isRefreshing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        AnimatedVisibility(
            visible = isRefreshing,
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)),
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BrowserModeButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF241D31),
        animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
        label = "browser_mode_background",
    )
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(enabled = enabled && !selected) { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            selected -> Color(0xFF06120B)
            else -> Color.White
        },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BackupScreen(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTagName by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .background(Color(0xFF120F1A))
            .padding(14.dp),
    ) {
        Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip("导出爱心和标签数据", selected = true, onClick = { onExportBackup("all") })
            SortChip("导入爱心和标签数据", selected = false, onClick = { onImportBackup("all") })
        }
        Spacer(Modifier.height(18.dp))
        Text("标签设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("新标签") },
                singleLine = true,
                modifier = Modifier.width(220.dp),
            )
            SortChip(
                text = "添加",
                selected = true,
                onClick = {
                    onAddTag(newTagName)
                    newTagName = ""
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags, key = { it }) { tagName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF241D31))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tagName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    SortChip("删除", selected = false, onClick = { onDeleteTag(tagName) })
                }
            }
        }
    }
}

@Composable
private fun SyncScreen(
    host: String,
    port: String,
    token: String,
    deviceId: String,
    pendingFiles: List<SyncPendingFile>,
    downloadingIds: Set<String>,
    isLoading: Boolean,
    message: String?,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onDeviceIdChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDownload: (SyncPendingFile) -> Unit,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF120F1A))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "同步",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SortChip(
                text = if (isLoading) "刷新中" else "刷新",
                selected = true,
                onClick = onRefresh,
            )
            SortChip(
                text = "全部下载",
                selected = true,
                onClick = onDownloadAll,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.weight(1f),
                label = { Text("IP") },
                singleLine = true,
            )
            TextField(
                value = port,
                onValueChange = onPortChange,
                modifier = Modifier.weight(0.45f),
                label = { Text("端口") },
                singleLine = true,
            )
            TextField(
                value = deviceId,
                onValueChange = onDeviceIdChange,
                modifier = Modifier.weight(0.8f),
                label = { Text("设备名") },
                singleLine = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SYNC_TOKEN") },
            singleLine = true,
        )
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pendingFiles.isEmpty()) {
            EmptyScreen("没有待同步文件")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(pendingFiles, key = { it.id }) { file ->
                    SyncFileRow(
                        file = file,
                        isDownloading = file.id in downloadingIds,
                        onDownload = { onDownload(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncFileRow(
    file: SyncPendingFile,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF241D31))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                file.filename,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${file.mediaType} · ${file.sizeBytes.formatBytes()} · ${file.downloadedAt}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SortChip(
            text = if (isDownloading) "下载中" else "下载",
            selected = true,
            onClick = {
                if (!isDownloading) onDownload()
            },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaGrid(
    items: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    onEnsureVideoThumbnail: (MediaItem) -> Unit,
    videoThumbnailPaths: Map<String, String>,
    returnTargetUri: String?,
    onReturnTargetHandled: (String) -> Unit,
    selectedMediaUrisForDelete: Set<String>,
    onSelectedMediaUrisForDeleteChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteSelectionMode = selectedMediaUrisForDelete.isNotEmpty()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }
    val returnTargetScale = remember { Animatable(1f) }
    val returnTargetBorderAlpha = remember { Animatable(0f) }
    val returnTargetIndex = remember(items, returnTargetUri) {
        GridReturnFocus.targetIndex(items, returnTargetUri)
    }
    BoxWithConstraints(modifier = modifier) {
        val columnCount = remember(maxWidth) {
            ((maxWidth + 8.dp) / (124.dp + 8.dp)).toInt().coerceAtLeast(1)
        }
        val cellSize = remember(maxWidth, columnCount) {
            (maxWidth - 20.dp - 8.dp * (columnCount - 1)) / columnCount
        }
        val visibleRowCount = remember(maxHeight, cellSize) {
            ((maxHeight + 8.dp) / (cellSize + 8.dp)).toInt().coerceAtLeast(1)
        }
        val centeredScrollIndex = remember(returnTargetIndex, columnCount, visibleRowCount) {
            returnTargetIndex?.let {
                GridReturnFocus.centeredScrollIndex(
                    targetIndex = it,
                    columnCount = columnCount,
                    visibleRowCount = visibleRowCount,
                )
            }
        }
        LaunchedEffect(returnTargetUri, returnTargetIndex, centeredScrollIndex) {
            val targetUri = returnTargetUri ?: return@LaunchedEffect
            centeredScrollIndex ?: return@LaunchedEffect
            gridState.animateScrollToItem(centeredScrollIndex)
            returnTargetScale.snapTo(1f)
            returnTargetBorderAlpha.snapTo(1f)
            returnTargetScale.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            )
            returnTargetScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            )
            delay(300)
            returnTargetBorderAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            )
            onReturnTargetHandled(targetUri)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(124.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.uri }) { item ->
                val selectedForDelete = item.uri in selectedMediaUrisForDelete
                val isReturnTarget = item.uri == returnTargetUri
                val thumbnailPath = videoThumbnailPaths[item.uri]
                LaunchedEffect(item.uri, thumbnailPath) {
                    if (item.mediaType == MediaType.Video && thumbnailPath == null) {
                        delay(180)
                        onEnsureVideoThumbnail(item)
                    }
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = if (isReturnTarget) returnTargetScale.value else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF241D31))
                        .combinedClickable(
                            onClick = {
                                if (deleteSelectionMode) {
                                    onSelectedMediaUrisForDeleteChange(
                                        if (selectedForDelete) {
                                            selectedMediaUrisForDelete - item.uri
                                        } else {
                                            selectedMediaUrisForDelete + item.uri
                                        },
                                    )
                                } else {
                                    onOpenItem(item)
                                }
                            },
                            onLongClick = {
                                onSelectedMediaUrisForDeleteChange(selectedMediaUrisForDelete + item.uri)
                            },
                        ),
                ) {
                    AsyncImage(
                        model = mediaImageModel(item, thumbnailPath),
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    if (item.mediaType == MediaType.Video) {
                        Text(
                            text = "▶ ${item.durationMillis.formatDuration()}",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color(0x99000000))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color(0x99000000))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        AuthorFavoritePicker(
                            favoriteLevel = item.favoriteLevel,
                            onFavoriteLevelChange = { level ->
                                onSetMediaFavoriteLevel(item.uri, level)
                            },
                        )
                    }
                    if (deleteSelectionMode) {
                        DeleteSelectionCheckbox(
                            selected = selectedForDelete,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(7.dp),
                        )
                    }
                    if (isReturnTarget && returnTargetBorderAlpha.value > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(
                                    width = 3.dp,
                                    color = Color(0xFFFFD54F).copy(alpha = returnTargetBorderAlpha.value),
                                    shape = RoundedCornerShape(8.dp),
                                ),
                        )
                    }
                }
            }
        }
        MediaFastScroller(
            gridState = gridState,
            itemCount = items.size,
            columnCount = columnCount,
            visibleRowCount = visibleRowCount,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp, top = 64.dp, bottom = 72.dp),
        )
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 18.dp),
            enter = fadeIn(animationSpec = tween(160)) + scaleIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(140)) + scaleOut(animationSpec = tween(140)),
        ) {
            ScrollToTopButton(
                onClick = {
                    coroutineScope.launch {
                        gridState.scrollToItem(0)
                    }
                },
            )
        }
    }
}

@Composable
private fun MediaFastScroller(
    gridState: LazyGridState,
    itemCount: Int,
    columnCount: Int,
    visibleRowCount: Int,
    modifier: Modifier = Modifier,
) {
    val totalRows = remember(itemCount, columnCount) {
        ((itemCount + columnCount - 1) / columnCount).coerceAtLeast(0)
    }
    val maxScrollableRow = (totalRows - visibleRowCount).coerceAtLeast(0)
    if (maxScrollableRow == 0) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val handleHeightPx = with(density) { 56.dp.toPx() }
    val dragRangePx = (trackHeightPx - handleHeightPx).coerceAtLeast(1f)
    val firstVisibleRow by remember {
        derivedStateOf { gridState.firstVisibleItemIndex / columnCount }
    }
    val handleOffsetPx = (firstVisibleRow.toFloat() / maxScrollableRow.toFloat())
        .coerceIn(0f, 1f) * dragRangePx

    fun scrollToTrackPosition(y: Float) {
        if (trackHeightPx <= 0f) return
        val ratio = ((y - handleHeightPx / 2f) / dragRangePx).coerceIn(0f, 1f)
        val targetRow = (ratio * maxScrollableRow).roundToInt().coerceIn(0, maxScrollableRow)
        coroutineScope.launch {
            gridState.scrollToItem(targetRow * columnCount)
        }
    }

    Box(
        modifier = modifier
            .width(36.dp)
            .onSizeChanged { size -> trackHeightPx = size.height.toFloat() }
            .pointerInput(maxScrollableRow, columnCount, trackHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> scrollToTrackPosition(offset.y) },
                    onVerticalDrag = { change, _ ->
                        scrollToTrackPosition(change.position.y)
                    },
                )
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x331F242C)),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = handleOffsetPx.roundToInt()) }
                .width(28.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xCCEEF2F7))
                .border(
                    width = 1.dp,
                    color = Color(0x99FFFFFF),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun ScrollToTopButton(onClick: () -> Unit) {
    Text(
        "顶部",
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6241D31))
            .border(
                width = 1.dp,
                color = Color(0x66FFFFFF),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun DeleteSelectionCheckbox(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFFD93025) else Color(0xCC181321)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (selected) "✓" else "",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MediaViewer(
    item: MediaItem,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    allTags: List<String>,
    selectedTags: Set<String>,
    onToggleMediaTag: (String) -> Unit,
    onDeleteCurrentMedia: () -> Unit,
) {
    var scale by remember(item.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(item.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.uri) { mutableFloatStateOf(0f) }
    var swipeOffsetY by remember(item.uri) { mutableFloatStateOf(0f) }
    val settleOffsetY = remember(item.uri) { Animatable(0f) }
    var settleRequest by remember(item.uri) { mutableStateOf<SwipeSettleRequest?>(null) }
    var settleSerial by remember(item.uri) { mutableStateOf(0) }
    var armedBoundaryIntent by remember { mutableStateOf<SwipeIntent?>(null) }
    var boundaryFeedbackText by remember { mutableStateOf<String?>(null) }
    var viewportHeight by remember { mutableFloatStateOf(0f) }
    val visibleSwipeOffsetY = settleRequest?.let { settleOffsetY.value } ?: swipeOffsetY

    BackHandler(onBack = onClose)

    LaunchedEffect(settleRequest?.serial) {
        val request = settleRequest ?: return@LaunchedEffect
        settleOffsetY.snapTo(request.startOffsetY)
        settleOffsetY.animateTo(
            targetValue = request.targetOffsetY,
            animationSpec = tween(
                durationMillis = if (request.intent == null) {
                    SwipeTransitionSpec.CANCEL_DURATION_MILLIS
                } else {
                    SwipeTransitionSpec.SETTLE_DURATION_MILLIS
                },
                easing = FastOutSlowInEasing,
            ),
        )
        when (request.intent) {
            SwipeIntent.Next -> if (request.wrapAround) onFirst() else onNext()
            SwipeIntent.Previous -> if (request.wrapAround) onLast() else onPrevious()
            null -> {
                settleRequest = null
                swipeOffsetY = 0f
                settleOffsetY.snapTo(0f)
            }
        }
    }

    LaunchedEffect(boundaryFeedbackText) {
        if (boundaryFeedbackText != null) {
            delay(900)
            boundaryFeedbackText = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewportHeight = it.height.toFloat() }
            .pointerInput(item.uri, hasPrevious, hasNext, viewportHeight) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalSwipe = 0f
                    var verticalSwipeCaptured = false
                    var usedMultiTouch = false

                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.count { it.pressed }
                        if (pressedPointers >= 2) {
                            usedMultiTouch = true
                            if (settleRequest != null) {
                                settleRequest = null
                            }
                            swipeOffsetY = 0f
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = nextScale
                            if (nextScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            event.changes
                                .filter { it.positionChanged() }
                                .forEach { it.consume() }
                        } else if (pressedPointers == 1 && !usedMultiTouch) {
                            val change = event.changes.firstOrNull { it.pressed }
                            val dragY = change?.positionChange()?.y ?: 0f
                            if (dragY != 0f) {
                                totalSwipe += dragY
                                if (
                                    verticalSwipeCaptured ||
                                    kotlin.math.abs(totalSwipe) > viewConfiguration.touchSlop
                                ) {
                                    verticalSwipeCaptured = true
                                    if (settleRequest != null) {
                                        settleRequest = null
                                    }
                                    change?.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val intent = if (usedMultiTouch) null else SwipeIntent.fromVerticalDrag(totalSwipe)
                    settleSerial += 1
                    when (intent) {
                        SwipeIntent.Next -> {
                            if (hasNext && viewportHeight > 0f) {
                                armedBoundaryIntent = null
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = -viewportHeight,
                                    intent = SwipeIntent.Next,
                                )
                            } else if (armedBoundaryIntent == SwipeIntent.Next && viewportHeight > 0f) {
                                boundaryFeedbackText = null
                                armedBoundaryIntent = null
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = -viewportHeight,
                                    intent = SwipeIntent.Next,
                                    wrapAround = true,
                                )
                            } else {
                                boundaryFeedbackText = "已经是最后一张了"
                                armedBoundaryIntent = SwipeIntent.Next
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = 0f,
                                    intent = null,
                                )
                            }
                        }

                        SwipeIntent.Previous -> {
                            if (hasPrevious && viewportHeight > 0f) {
                                armedBoundaryIntent = null
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = viewportHeight,
                                    intent = SwipeIntent.Previous,
                                )
                            } else if (armedBoundaryIntent == SwipeIntent.Previous && viewportHeight > 0f) {
                                boundaryFeedbackText = null
                                armedBoundaryIntent = null
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = viewportHeight,
                                    intent = SwipeIntent.Previous,
                                    wrapAround = true,
                                )
                            } else {
                                boundaryFeedbackText = "已经是第一张了"
                                armedBoundaryIntent = SwipeIntent.Previous
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = 0f,
                                    intent = null,
                                )
                            }
                        }

                        null -> {
                            armedBoundaryIntent = null
                            settleRequest = SwipeSettleRequest(
                                serial = settleSerial,
                                startOffsetY = 0f,
                                targetOffsetY = 0f,
                                intent = null,
                            )
                        }
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = visibleSwipeOffsetY.toInt()) },
        ) {
            MediaSurface(
                item = item,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                activeVideo = true,
                onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
            )
        }

        if (item.mediaType == MediaType.Image) {
            ViewerFavoritePicker(
                favoriteLevel = item.favoriteLevel,
                onFavoriteLevelChange = { level ->
                    onSetMediaFavoriteLevel(item.uri, level)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = (maxHeight / 3) - 64.dp),
            )
            InfoButton(
                displayName = item.displayName,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        ViewerDeleteButton(
            onClick = onDeleteCurrentMedia,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = (maxHeight / 3) + 54.dp),
        )
        ViewerTagRail(
            tags = allTags,
            selectedTags = selectedTags,
            onToggleTag = onToggleMediaTag,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = maxHeight / 8),
        )
        AnimatedVisibility(
            visible = boundaryFeedbackText != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                scaleIn(
                    animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS),
                    initialScale = 0.92f,
                ),
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                scaleOut(
                    animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS),
                    targetScale = 0.92f,
                ),
        ) {
            Text(
                boundaryFeedbackText.orEmpty(),
                modifier = Modifier
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class SwipeSettleRequest(
    val serial: Int,
    val startOffsetY: Float,
    val targetOffsetY: Float,
    val intent: SwipeIntent?,
    val wrapAround: Boolean = false,
)

private const val VideoGestureDoubleTapTimeoutMillis = 280L

@Composable
private fun MediaSurface(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    activeVideo: Boolean,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
) {
    when {
        item.mediaType == MediaType.Video && activeVideo -> VideoPlayer(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
        )

        else -> StableAspectMediaFrame(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
        ) {
            AsyncImage(
                model = mediaImageModel(item),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun StableAspectMediaFrame(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    aspectRatioOverride: Float? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val aspectRatio = aspectRatioOverride ?: item.displayAspectRatio
        val containerRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else null
        val aspectModifier = when {
            aspectRatio == null || containerRatio == null -> Modifier.fillMaxSize()
            aspectRatio > containerRatio -> Modifier.fillMaxWidth().aspectRatio(aspectRatio)
            else -> Modifier.fillMaxHeight().aspectRatio(aspectRatio)
        }

        Box(
            modifier = aspectModifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
        ) {
            content()
        }
    }
}

private val MediaFolder.previewItem: MediaItem?
    get() = items.firstOrNull { it.uri == previewUri } ?: items.firstOrNull()

@Composable
private fun mediaImageModel(item: MediaItem?, thumbnailPath: String? = null): Any? {
    if (item == null) return null
    if (item.mediaType != MediaType.Video) return item.uri
    if (thumbnailPath != null) return File(thumbnailPath)
    return null
}

@Composable
private fun videoFrameImageModel(item: MediaItem?): Any? {
    if (item == null) return null
    if (item.mediaType != MediaType.Video) return item.uri
    return ImageRequest.Builder(LocalContext.current)
        .data(item.uri)
        .videoFrameMillis(1_000)
        .build()
}

@Composable
private fun VideoPlayer(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPaused by remember(item.uri) { mutableStateOf(false) }
    var playerView by remember(item.uri) { mutableStateOf<PlayerView?>(null) }
    var currentPosition by remember(item.uri) { mutableStateOf(0L) }
    var duration by remember(item.uri) { mutableStateOf(0L) }
    var controlsVisible by remember(item.uri) { mutableStateOf(ViewerUiSpec.CONTROLS_VISIBLE_BY_DEFAULT) }
    var playbackSpeed by remember(item.uri) { mutableFloatStateOf(1f) }
    var seekFeedbackText by remember(item.uri) { mutableStateOf<String?>(null) }
    var isSavingSnapshot by remember(item.uri) { mutableStateOf(false) }
    var videoAspectRatio by remember(item.uri) { mutableStateOf(item.displayAspectRatio) }
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(fromUri(Uri.parse(item.uri)))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, playerView) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoAspectRatio = videoSize.displayAspectRatio() ?: item.displayAspectRatio
                player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                playerView?.requestLayout()
                playerView?.invalidate()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                duration = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                currentPosition = player.currentPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            duration = player.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            delay(700)
            seekFeedbackText = null
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        StableAspectMediaFrame(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            aspectRatioOverride = videoAspectRatio,
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = mediaImageModel(item),
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )

                key(item.uri) {
                    AndroidView(
                        factory = { viewContext ->
                            (LayoutInflater.from(viewContext)
                                .inflate(R.layout.view_video_player, null, false) as PlayerView).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setEnableComposeSurfaceSyncWorkaround(true)
                                this.player = player
                                playerView = this
                            }
                        },
                        update = {
                            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                            it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            it.setEnableComposeSurfaceSyncWorkaround(true)
                            it.player = player
                            it.requestLayout()
                            it.invalidate()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(item.uri) {
                    detectVideoGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = {
                            player.playWhenReady = !player.playWhenReady
                            isPaused = !player.playWhenReady
                            controlsVisible = true
                        },
                        onHorizontalSwipe = { seekOffset ->
                            val targetPosition = (player.currentPosition + seekOffset)
                                .coerceIn(0L, duration.coerceAtLeast(0L))
                            player.seekTo(targetPosition)
                            currentPosition = targetPosition
                            seekFeedbackText = VideoSeekGesture.feedbackText(seekOffset)
                        },
                    )
                },
        )

        SnapshotButton(
            isSaving = isSavingSnapshot,
            onClick = {
                if (isSavingSnapshot) return@SnapshotButton
                isSavingSnapshot = true
                coroutineScope.launch {
                    val result = saveVideoSnapshot(
                        context = context,
                        item = item,
                        positionMillis = player.currentPosition.coerceAtLeast(0L),
                    )
                    isSavingSnapshot = false
                    Toast.makeText(
                        context,
                        result.fold(
                            onSuccess = { "截图已保存: $it" },
                            onFailure = { "截图失败" },
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = maxHeight / 3),
        )
        ViewerFavoritePicker(
            favoriteLevel = item.favoriteLevel,
            onFavoriteLevelChange = { level ->
                onSetMediaFavoriteLevel(item.uri, level)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = (maxHeight / 3) - 64.dp),
        )

        AnimatedVisibility(
            visible = isPaused,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideInVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { -it / 2 },
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideOutVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { -it / 2 },
        ) {
            Text(
                "暂停",
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        AnimatedVisibility(
            visible = seekFeedbackText != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                scaleIn(
                    animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS),
                    initialScale = 0.92f,
                ),
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                scaleOut(
                    animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS),
                    targetScale = 0.92f,
                ),
        ) {
            Text(
                seekFeedbackText.orEmpty(),
                modifier = Modifier
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                slideInVertically(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) { it / 2 },
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                slideOutVertically(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) { it / 2 },
        ) {
            VideoProgressBar(
                displayName = item.displayName,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                onSeek = { position ->
                    player.seekTo(position)
                    currentPosition = position
                },
                onPlaybackSpeedChange = { speed ->
                    playbackSpeed = speed
                },
            )
        }
    }
}

@Composable
private fun SnapshotButton(
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        if (isSaving) "保存中" else "截屏",
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSaving) Color(0x884A5A4E) else Color(0x9934A853))
            .clickable(enabled = !isSaving) { onClick() }
            .padding(horizontal = 15.dp, vertical = 10.dp),
        color = Color.White,
                style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

private suspend fun saveVideoSnapshot(
    context: Context,
    item: MediaItem,
    positionMillis: Long,
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val fileName = VideoSnapshotFileName.create(item.displayName, System.currentTimeMillis())
        val bitmap = MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, Uri.parse(item.uri))
            retriever.getFrameAtTime(
                positionMillis * 1_000L,
                MediaMetadataRetriever.OPTION_CLOSEST,
            ) ?: error("No frame at position")
        }
        saveBitmapToMediaStore(context, bitmap, fileName)
        fileName
    }
}

private fun saveBitmapToMediaStore(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VideoSee")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create snapshot file")
    try {
        resolver.openOutputStream(uri)?.use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                "Could not encode snapshot"
            }
        } ?: error("Could not open snapshot file")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
        }
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    } finally {
        bitmap.recycle()
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectVideoGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onHorizontalSwipe: (Long) -> Unit,
) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        var totalDragX = 0f
        var totalDragY = 0f
        var moved = false

        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }
            if (pressedPointers > 1) return@awaitEachGesture

            event.changes.forEach { change ->
                if (change.pressed) {
                    val positionChange = change.positionChange()
                    totalDragX += positionChange.x
                    totalDragY += positionChange.y
                    if (positionChange.x != 0f || positionChange.y != 0f) {
                        moved = true
                    }
                }
            }
        } while (event.changes.any { it.pressed })

        val seekOffset = if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
            VideoSeekGesture.seekOffsetMillis(totalDragX)
        } else {
            null
        }
        when {
            seekOffset != null -> onHorizontalSwipe(seekOffset)
            !moved -> {
                val secondDown = withTimeoutOrNull(VideoGestureDoubleTapTimeoutMillis) {
                    awaitFirstDown(requireUnconsumed = false)
                }
                if (secondDown == null) {
                    onTap()
                } else {
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onDoubleTap()
                }
            }
        }

        firstDown.consume()
    }
}

@Composable
private fun VideoProgressBar(
    displayName: String,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    onSeek: (Long) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showInfo by remember(displayName) { mutableStateOf(false) }
    val safeDuration = duration.coerceAtLeast(1L)
    val safePosition = currentPosition.coerceIn(0L, safeDuration)
    val controlInteractionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showInfo,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideInVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { it / 2 },
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideOutVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { it / 2 },
        ) {
            Text(
                displayName,
                modifier = Modifier
                    .padding(start = 32.dp, end = 32.dp, bottom = 54.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .clickable { context.copyTextToClipboard("filename", displayName) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x8A4A4A4A))
                .clickable(
                    interactionSource = controlInteractionSource,
                    indication = null,
                    onClick = {},
                )
                .padding(start = 14.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                PlaybackTimeFormatter.formatMillis(safePosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            CompactSeekBar(
                currentPosition = safePosition,
                duration = safeDuration,
                onSeek = onSeek,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )
            Text(
                PlaybackTimeFormatter.formatMillis(duration),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "i",
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showInfo = !showInfo }
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            PlaybackSpeedOptions.values.forEach { speed ->
                val selected = playbackSpeed == speed
                val speedBackground by animateColorAsState(
                    targetValue = if (selected) Color(0x55FFFFFF) else Color.Transparent,
                    animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
                    label = "playback_speed_selection",
                )
                Text(
                    "${speed}x",
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(speedBackground)
                        .clickable { onPlaybackSpeedChange(speed) }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun CompactSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val progress = if (duration > 0L) {
        currentPosition.toFloat() / duration.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val thumbHalfPx = with(density) { 4.dp.toPx() }

    fun seekFromX(x: Float) {
        if (widthPx <= 0) return
        val fraction = (x / widthPx).coerceIn(0f, 1f)
        onSeek((duration * fraction).toLong())
    }

    Box(
        modifier = modifier
            .height(18.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(duration, widthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    seekFromX(down.position.x)
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                seekFromX(change.position.x)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x66FFFFFF)),
        )
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White),
        )
        Box(
            Modifier
                .offset {
                    IntOffset(
                        x = (widthPx * progress - thumbHalfPx).roundToInt(),
                        y = 0,
                    )
                }
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White),
        )
    }
}

@Composable
private fun InfoButton(
    displayName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showInfo by remember(displayName) { mutableStateOf(false) }
    Box(modifier = modifier.padding(16.dp)) {
        AnimatedVisibility(
            visible = showInfo,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideInVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { it / 2 },
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) +
                slideOutVertically(animationSpec = tween(ViewerUiSpec.OVERLAY_TRANSITION_DURATION_MILLIS)) { it / 2 },
        ) {
            Text(
                displayName,
                modifier = Modifier
                    .padding(bottom = 42.dp)
                    .background(Color(0x99000000), RoundedCornerShape(6.dp))
                    .clickable { context.copyTextToClipboard("filename", displayName) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
        }
        Text(
            "i",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x99000000))
                .clickable { showInfo = !showInfo }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun requiredPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        }

        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun Context.hasMediaPermission(): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        requiredPermissions() + Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    } else {
        requiredPermissions()
    }
    return permissions.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}

private fun VideoSize.displayAspectRatio(): Float? {
    if (width <= 0 || height <= 0) return null
    return width.toFloat() * pixelWidthHeightRatio / height.toFloat()
}

private fun Long?.formatDuration(): String {
    val totalSeconds = (this ?: 0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun Long.formatBytes(): String {
    if (this <= 0L) return "-"
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "%.1f %s".format(value, units[unitIndex])
    }
}

private fun Context.copyTextToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, "文件名已复制", Toast.LENGTH_SHORT).show()
}
