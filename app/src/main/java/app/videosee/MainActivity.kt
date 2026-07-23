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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem.fromUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.effect.ConvolutionFunction1D
import androidx.media3.effect.SeparableConvolution
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.effect.SingleColorLut
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
import app.videosee.ui.VideoSeekGesture
import app.videosee.ui.VideoSnapshotFileName
import app.videosee.ui.ViewerUiSpec
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.io.File
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoSeeRoute()
        }
    }
}

@Composable
private fun VideoSeeTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val palette = when (theme) {
        AppTheme.Midnight -> darkColorScheme(
            background = Color(0xFF101827), surface = Color(0xFF172033), surfaceVariant = Color(0xFF24314A),
            primary = Color(0xFF7DD3FC), onBackground = Color(0xFFEAF2FF), onSurface = Color(0xFFEAF2FF), onSurfaceVariant = Color(0xFFAAB8D0),
        )
        AppTheme.Graphite -> darkColorScheme(
            background = Color(0xFF171717), surface = Color(0xFF202020), surfaceVariant = Color(0xFF303030),
            primary = Color(0xFFD4D4D4), onBackground = Color(0xFFF4F4F5), onSurface = Color(0xFFF4F4F5), onSurfaceVariant = Color(0xFFA1A1AA),
        )
        AppTheme.Forest -> darkColorScheme(
            background = Color(0xFF0E1B17), surface = Color(0xFF14251F), surfaceVariant = Color(0xFF203A30),
            primary = Color(0xFF6EE7B7), onBackground = Color(0xFFE7F8EF), onSurface = Color(0xFFE7F8EF), onSurfaceVariant = Color(0xFFA8C7B7),
        )
        AppTheme.Snow -> lightColorScheme(
            background = Color(0xFFFAFAFA), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF1F5F9),
            primary = Color(0xFF2563EB), onBackground = Color(0xFF172033), onSurface = Color(0xFF172033), onSurfaceVariant = Color(0xFF64748B),
        )
        AppTheme.Mist -> lightColorScheme(
            background = Color(0xFFF9F7FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF0EBFA),
            primary = Color(0xFF7C3AED), onBackground = Color(0xFF251B35), onSurface = Color(0xFF251B35), onSurfaceVariant = Color(0xFF776B8B),
        )
        AppTheme.Sand -> lightColorScheme(
            background = Color(0xFFFFFBF5), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF7EEDF),
            primary = Color(0xFFB45309), onBackground = Color(0xFF302015), onSurface = Color(0xFF302015), onSurfaceVariant = Color(0xFF816B58),
        )
    }
    MaterialTheme(
        colorScheme = palette,
        content = content,
    )
}

@Composable
private fun VideoSeeRoute(viewModel: VideoSeeViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    var selectedMediaUrisForDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var multiMediaSession by remember { mutableStateOf<MultiMediaSession?>(null) }
    var isMultiMediaFullscreen by remember { mutableStateOf(false) }
    var temporarySinglePaneSlot by remember { mutableStateOf<MultiMediaSlot?>(null) }
    var videoRotationLockState by remember { mutableStateOf(VideoRotationLockState()) }
    val playbackAdjustmentStore = remember(context) { PlaybackAdjustmentStore(context) }
    var toneCurve by remember(playbackAdjustmentStore) {
        mutableStateOf(playbackAdjustmentStore.loadToneCurve())
    }
    var isToneCurvePanelVisible by remember { mutableStateOf(false) }
    var colorAdjustments by remember(playbackAdjustmentStore) {
        mutableStateOf(playbackAdjustmentStore.loadColorAdjustments())
    }
    var isColorAdjustmentPanelVisible by remember { mutableStateOf(false) }
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

    CompositionLocalProvider(
        LocalToneCurve provides toneCurve,
        LocalColorAdjustments provides colorAdjustments,
        LocalToneCurveEditing provides (isToneCurvePanelVisible || isColorAdjustmentPanelVisible),
    ) {
    VideoSeeTheme(theme = state.appTheme) {
    VideoSeeApp(
        state = state,
        onRequestPermission = { launcher.launch(requiredPermissions()) },
        onSelectFolder = viewModel::selectFolder,
        onSelectAuthor = viewModel::selectAuthor,
        onSelectTag = viewModel::selectTag,
        onSelectFavoriteFolder = viewModel::selectFavoriteFolder,
        onSelectBrowserMode = viewModel::selectBrowserMode,
        onCollectionSearchQueryChange = viewModel::updateCollectionSearchQuery,
        onSelectCollectionSortField = viewModel::selectCollectionSortField,
        onToggleCollectionSortDirection = viewModel::toggleCollectionSortDirection,
        onSetAuthorFavoriteLevel = viewModel::setAuthorFavoriteLevel,
        onSelectMediaSortField = viewModel::selectMediaSortField,
        onToggleMediaSortDirection = viewModel::toggleMediaSortDirection,
        onSetMediaFavoriteLevel = viewModel::setMediaFavoriteLevel,
        onEnsureVideoThumbnail = viewModel::ensureVideoThumbnail,
        onOpenItem = { selectedItem ->
            val session = multiMediaSession
            val pendingSlot = session?.pendingSlot
            if (pendingSlot != null) {
                multiMediaSession = session.withPane(
                    slot = pendingSlot,
                    pane = session.pane(pendingSlot)?.copy(
                        item = selectedItem,
                        sourceItems = state.selectedItems,
                        playbackPositionMillis = 0L,
                        playWhenReady = true,
                    ) ?: MultiMediaPaneSession(
                        item = selectedItem,
                        sourceItems = state.selectedItems,
                    ),
                ).copy(pendingSlot = null)
                isMultiMediaFullscreen = true
            } else {
                viewModel.openViewer(selectedItem)
            }
        },
        onCloseViewer = viewModel::closeViewer,
        onNext = viewModel::showNext,
        onPrevious = viewModel::showPrevious,
        onFirst = viewModel::showFirst,
        onLast = viewModel::showLast,
        onTogglePlaybackMode = viewModel::togglePlaybackMode,
        onAddVideoSegment = viewModel::addVideoSegment,
        onDeleteVideoSegment = viewModel::deleteVideoSegment,
        onRenameVideoSegment = viewModel::renameVideoSegment,
        onOpenAuthorSearch = viewModel::openAuthorSearch,
        onRefresh = viewModel::refresh,
        onOpenSettings = viewModel::openSettingsPane,
        onOpenTagSettings = viewModel::openTagSettingsPane,
        onOpenSync = viewModel::openSyncPane,
        onOpenBackup = viewModel::openBackupPane,
        onSelectAppTheme = viewModel::selectAppTheme,
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
        onCreateFavoriteFolder = viewModel::createFavoriteFolder,
        onRenameFavoriteFolder = viewModel::renameFavoriteFolder,
        onSetDefaultFavoriteFolder = viewModel::setDefaultFavoriteFolder,
        onToggleMediaInDefaultFavoriteFolder = viewModel::toggleMediaInDefaultFavoriteFolder,
        onGridReturnTargetHandled = viewModel::clearGridReturnTarget,
        multiMediaSession = multiMediaSession,
        isMultiMediaFullscreen = isMultiMediaFullscreen,
        temporarySinglePaneSlot = temporarySinglePaneSlot,
        onEnterMultiMediaMode = { currentItem ->
            temporarySinglePaneSlot = null
            multiMediaSession = MultiMediaSession(
                topLeft = MultiMediaPaneSession(
                    item = currentItem,
                    sourceItems = state.selectedItems,
                ),
            )
            isMultiMediaFullscreen = true
        },
        onPickMultiMediaSlotFromBrowser = { slot ->
            multiMediaSession = multiMediaSession?.copy(pendingSlot = slot)
            isMultiMediaFullscreen = false
            viewModel.closeViewer()
        },
        onSearchMultiMediaAuthor = { slot, authorId ->
            multiMediaSession = multiMediaSession?.copy(pendingSlot = slot)
            viewModel.openAuthorSearch(authorId)
            isMultiMediaFullscreen = false
            viewModel.closeViewer()
        },
        onExpandMultiMediaSlot = { slot, playbackSnapshots ->
            multiMediaSession = multiMediaSession
                ?.withPanePlaybackSnapshots(playbackSnapshots)
                ?.expandRightSlot(slot)
        },
        onRemoveMultiMediaSlot = { slot, playbackSnapshots ->
            val updatedSession = multiMediaSession
                ?.withPanePlaybackSnapshots(playbackSnapshots)
                ?.removePane(slot)
            if (updatedSession == null || updatedSession.isEmpty()) {
                multiMediaSession = null
                isMultiMediaFullscreen = false
                viewModel.closeViewer()
            } else {
                multiMediaSession = updatedSession
            }
        },
        onExitMultiMediaMode = {
            multiMediaSession = null
            isMultiMediaFullscreen = false
            temporarySinglePaneSlot = null
        },
        onOpenMultiMediaMode = {
            isMultiMediaFullscreen = true
            multiMediaSession?.firstPane()?.item?.let(viewModel::openViewer)
        },
        onUpdateMultiMedia = { slot, item ->
            multiMediaSession = multiMediaSession?.let { session ->
                val currentPane = session.pane(slot) ?: return@let session
                session.withPane(
                    slot,
                    currentPane.copy(
                        item = item,
                        playbackPositionMillis = 0L,
                        playWhenReady = true,
                        scale = currentPane.scale.takeIf { it < 1f } ?: 1f,
                    ),
                )
            }
        },
        onUpdateMultiMediaPlayback = { slot, positionMillis, playWhenReady, scale ->
            multiMediaSession = multiMediaSession?.withPanePlaybackState(
                slot = slot,
                positionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale,
            )
        },
        onOpenTemporarySingleMedia = { slot, positionMillis, playWhenReady, scale ->
            multiMediaSession = multiMediaSession?.withPanePlaybackState(
                slot = slot,
                positionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale,
            )
            temporarySinglePaneSlot = slot
            isMultiMediaFullscreen = false
        },
        onReturnToMultiMedia = {
            temporarySinglePaneSlot = null
            isMultiMediaFullscreen = true
        },
        onUpdateTemporaryMultiMediaPlayback = { slot, itemUri, positionMillis, playWhenReady ->
            multiMediaSession = multiMediaSession?.let { session ->
                if (session.pane(slot)?.item?.uri != itemUri) session else {
                    session.withPanePlaybackState(slot, positionMillis, playWhenReady)
                }
            }
        },
        onUpdateTemporaryMultiMediaScale = { slot, itemUri, scale ->
            multiMediaSession = multiMediaSession?.let { session ->
                if (session.pane(slot)?.item?.uri != itemUri) session else {
                    session.withPaneScale(slot, scale)
                }
            }
        },
        toneCurve = toneCurve,
        isToneCurvePanelVisible = isToneCurvePanelVisible,
        colorAdjustments = colorAdjustments,
        isColorAdjustmentPanelVisible = isColorAdjustmentPanelVisible,
        onOpenToneCurve = {
            isColorAdjustmentPanelVisible = false
            isToneCurvePanelVisible = true
        },
        onToneCurveChange = {
            toneCurve = it
            playbackAdjustmentStore.saveToneCurve(it)
        },
        onDismissToneCurve = { isToneCurvePanelVisible = false },
        onOpenColorAdjustments = {
            isToneCurvePanelVisible = false
            isColorAdjustmentPanelVisible = true
        },
        onColorAdjustmentsChange = {
            colorAdjustments = it
            playbackAdjustmentStore.saveColorAdjustments(it)
        },
        onDismissColorAdjustments = { isColorAdjustmentPanelVisible = false },
        videoRotationLockState = videoRotationLockState,
        onLockVideoRotation = { degrees ->
            videoRotationLockState = VideoRotationLockState(
                defaultDegrees = normalizeRotationDegrees(degrees),
                isLocked = true,
            )
        },
        onUnlockVideoRotation = {
            videoRotationLockState = videoRotationLockState.copy(isLocked = false)
        },
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
    }
}

@Composable
private fun VideoSeeApp(
    state: VideoSeeUiState,
    onRequestPermission: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectAuthor: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onSelectFavoriteFolder: (String) -> Unit,
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
    onTogglePlaybackMode: () -> Unit,
    onAddVideoSegment: (String, Long, Long) -> Unit,
    onDeleteVideoSegment: (String, VideoSegment) -> Unit,
    onRenameVideoSegment: (String, VideoSegment, String) -> Unit,
    onOpenAuthorSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTagSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onSelectAppTheme: (AppTheme) -> Unit,
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
    onCreateFavoriteFolder: () -> Unit,
    onRenameFavoriteFolder: (String, String) -> Unit,
    onSetDefaultFavoriteFolder: (String) -> Unit,
    onToggleMediaInDefaultFavoriteFolder: (String) -> Unit,
    onGridReturnTargetHandled: (String) -> Unit,
    multiMediaSession: MultiMediaSession?,
    isMultiMediaFullscreen: Boolean,
    temporarySinglePaneSlot: MultiMediaSlot?,
    onEnterMultiMediaMode: (MediaItem) -> Unit,
    onPickMultiMediaSlotFromBrowser: (MultiMediaSlot) -> Unit,
    onSearchMultiMediaAuthor: (MultiMediaSlot, String) -> Unit,
    onExpandMultiMediaSlot: (MultiMediaSlot, Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>) -> Unit,
    onRemoveMultiMediaSlot: (MultiMediaSlot, Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>) -> Unit,
    onExitMultiMediaMode: () -> Unit,
    onOpenMultiMediaMode: () -> Unit,
    onUpdateMultiMedia: (MultiMediaSlot, MediaItem) -> Unit,
    onUpdateMultiMediaPlayback: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onOpenTemporarySingleMedia: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onReturnToMultiMedia: () -> Unit,
    onUpdateTemporaryMultiMediaPlayback: (MultiMediaSlot, String, Long, Boolean) -> Unit,
    onUpdateTemporaryMultiMediaScale: (MultiMediaSlot, String, Float) -> Unit,
    toneCurve: ToneCurve,
    isToneCurvePanelVisible: Boolean,
    colorAdjustments: ColorAdjustments,
    isColorAdjustmentPanelVisible: Boolean,
    onOpenToneCurve: () -> Unit,
    onToneCurveChange: (ToneCurve) -> Unit,
    onDismissToneCurve: () -> Unit,
    onOpenColorAdjustments: () -> Unit,
    onColorAdjustmentsChange: (ColorAdjustments) -> Unit,
    onDismissColorAdjustments: () -> Unit,
    videoRotationLockState: VideoRotationLockState,
    onLockVideoRotation: (Int) -> Unit,
    onUnlockVideoRotation: () -> Unit,
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
                    onSelectFavoriteFolder = onSelectFavoriteFolder,
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
                    onOpenSettings = onOpenSettings,
                    onOpenTagSettings = onOpenTagSettings,
                    onOpenSync = onOpenSync,
                    onOpenBackup = onOpenBackup,
                    onSelectAppTheme = onSelectAppTheme,
                    onSyncHostChange = onSyncHostChange,
                    onSyncPortChange = onSyncPortChange,
                    onSyncTokenChange = onSyncTokenChange,
                    onSyncDeviceIdChange = onSyncDeviceIdChange,
                    onLoadSyncPendingFiles = onLoadSyncPendingFiles,
                    onDownloadSyncFile = onDownloadSyncFile,
                    onDownloadAllSyncFiles = onDownloadAllSyncFiles,
                    onAddTag = onAddTag,
                    onDeleteTag = onDeleteTag,
                    onCreateFavoriteFolder = onCreateFavoriteFolder,
                    onRenameFavoriteFolder = onRenameFavoriteFolder,
                    onSetDefaultFavoriteFolder = onSetDefaultFavoriteFolder,
                    onGridReturnTargetHandled = onGridReturnTargetHandled,
                    selectedMediaUrisForDelete = selectedMediaUrisForDelete,
                    onSelectedMediaUrisForDeleteChange = onSelectedMediaUrisForDeleteChange,
                    onDeleteMediaUris = onDeleteMediaUris,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                )
            }
        }

        state.viewerItem?.uri?.takeIf { multiMediaSession == null }?.let { uri ->
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
                playbackMode = state.playbackMode,
                onTogglePlaybackMode = onTogglePlaybackMode,
                videoSegments = state.videoSegmentsByUri[item.uri].orEmpty(),
                onAddVideoSegment = { start, end -> onAddVideoSegment(item.uri, start, end) },
                onDeleteVideoSegment = { segment -> onDeleteVideoSegment(item.uri, segment) },
                onRenameVideoSegment = { segment, name -> onRenameVideoSegment(item.uri, segment, name) },
                onOpenAuthorSearch = onOpenAuthorSearch,
                onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
                isInDefaultFavoriteFolder = state.defaultFavoriteFolderId
                    ?.let { item.uri in state.favoriteFolderMediaUris[it].orEmpty() } == true,
                onToggleDefaultFavoriteFolder = { onToggleMediaInDefaultFavoriteFolder(item.uri) },
                allTags = state.tags,
                selectedTags = state.mediaTags[item.uri].orEmpty(),
                onToggleMediaTag = { tagName -> onToggleMediaTag(item.uri, tagName) },
                onDeleteCurrentMedia = { onDeleteViewerMedia(item.uri, viewerDeleteTargetUri) },
                onEnterMultiVideoMode = { onEnterMultiMediaMode(item) },
                onOpenToneCurve = onOpenToneCurve,
                onToneCurveChange = onToneCurveChange,
                onOpenColorAdjustments = onOpenColorAdjustments,
                onColorAdjustmentsChange = onColorAdjustmentsChange,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
            )
        }
        val temporarySinglePane = temporarySinglePaneSlot?.let { slot ->
            multiMediaSession?.pane(slot)?.let { pane -> slot to pane }
        }
        temporarySinglePane?.let { (slot, pane) ->
            val temporaryViewerItem = pane.item.copy(
                favoriteLevel = state.mediaFavoriteLevels[pane.item.uri] ?: 0,
            )
            val sourceItems = pane.sourceItems
            val currentIndex = sourceItems.indexOfFirst { it.uri == pane.item.uri }
            MediaViewer(
                item = temporaryViewerItem,
                hasPrevious = currentIndex > 0,
                hasNext = currentIndex >= 0 && currentIndex < sourceItems.lastIndex,
                onClose = onReturnToMultiMedia,
                onNext = {
                    sourceItems.getOrNull(currentIndex + 1)?.let { nextItem ->
                        onUpdateMultiMedia(slot, nextItem)
                    }
                },
                onPrevious = {
                    sourceItems.getOrNull(currentIndex - 1)?.let { previousItem ->
                        onUpdateMultiMedia(slot, previousItem)
                    }
                },
                onFirst = {
                    sourceItems.firstOrNull()?.let { firstItem ->
                        onUpdateMultiMedia(slot, firstItem)
                    }
                },
                onLast = {
                    sourceItems.lastOrNull()?.let { lastItem ->
                        onUpdateMultiMedia(slot, lastItem)
                    }
                },
                playbackMode = state.playbackMode,
                onTogglePlaybackMode = onTogglePlaybackMode,
                videoSegments = state.videoSegmentsByUri[temporaryViewerItem.uri].orEmpty(),
                onAddVideoSegment = { start, end -> onAddVideoSegment(temporaryViewerItem.uri, start, end) },
                onDeleteVideoSegment = { segment -> onDeleteVideoSegment(temporaryViewerItem.uri, segment) },
                onRenameVideoSegment = { segment, name ->
                    onRenameVideoSegment(temporaryViewerItem.uri, segment, name)
                },
                onOpenAuthorSearch = onOpenAuthorSearch,
                onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
                isInDefaultFavoriteFolder = state.defaultFavoriteFolderId
                    ?.let { temporaryViewerItem.uri in state.favoriteFolderMediaUris[it].orEmpty() } == true,
                onToggleDefaultFavoriteFolder = {
                    onToggleMediaInDefaultFavoriteFolder(temporaryViewerItem.uri)
                },
                allTags = state.tags,
                selectedTags = state.mediaTags[temporaryViewerItem.uri].orEmpty(),
                onToggleMediaTag = { tagName -> onToggleMediaTag(temporaryViewerItem.uri, tagName) },
                onDeleteCurrentMedia = {},
                onEnterMultiVideoMode = {},
                onReturnToMultiVideo = onReturnToMultiMedia,
                showDeleteAction = false,
                initialPlaybackPositionMillis = pane.playbackPositionMillis,
                initialPlayWhenReady = pane.playWhenReady,
                initialScale = pane.scale,
                onVideoPlaybackStateChange = { position, shouldPlay ->
                    onUpdateTemporaryMultiMediaPlayback(slot, temporaryViewerItem.uri, position, shouldPlay)
                },
                onViewerScaleChange = { scale ->
                    onUpdateTemporaryMultiMediaScale(slot, temporaryViewerItem.uri, scale)
                },
                onOpenToneCurve = onOpenToneCurve,
                onToneCurveChange = onToneCurveChange,
                onOpenColorAdjustments = onOpenColorAdjustments,
                onColorAdjustmentsChange = onColorAdjustmentsChange,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
            )
        }
        multiMediaSession?.takeIf { isMultiMediaFullscreen && temporarySinglePane == null }?.let { session ->
            MultiMediaViewer(
                session = session,
                videoSegmentsByUri = state.videoSegmentsByUri,
                onPickMediaSlot = onPickMultiMediaSlotFromBrowser,
                onSearchAuthor = onSearchMultiMediaAuthor,
                onExpandMediaSlot = onExpandMultiMediaSlot,
                onRemoveMediaSlot = onRemoveMultiMediaSlot,
                onPaneItemChange = onUpdateMultiMedia,
                onPanePlaybackStateChange = onUpdateMultiMediaPlayback,
                onOpenTemporarySingleMedia = onOpenTemporarySingleMedia,
                onOpenToneCurve = onOpenToneCurve,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                onExit = onExitMultiMediaMode,
            )
        }
        multiMediaSession?.takeIf { !isMultiMediaFullscreen && temporarySinglePane == null }?.let { session ->
            FloatingMultiMediaWindow(
                session = session,
                onPaneItemChange = onUpdateMultiMedia,
                videoRotationLockState = videoRotationLockState,
                onOpen = onOpenMultiMediaMode,
                onClose = onExitMultiMediaMode,
            )
        }
        if (isToneCurvePanelVisible) {
            ToneCurvePanel(
                toneCurve = toneCurve,
                onToneCurveChange = onToneCurveChange,
                onDismiss = onDismissToneCurve,
            )
        }
        if (isColorAdjustmentPanelVisible) {
            ColorAdjustmentPanel(
                colorAdjustments = colorAdjustments,
                onColorAdjustmentsChange = onColorAdjustmentsChange,
                onDismiss = onDismissColorAdjustments,
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
    onSelectFavoriteFolder: (String) -> Unit,
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
    onOpenSettings: () -> Unit,
    onOpenTagSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenBackup: () -> Unit,
    onSelectAppTheme: (AppTheme) -> Unit,
    onSyncHostChange: (String) -> Unit,
    onSyncPortChange: (String) -> Unit,
    onSyncTokenChange: (String) -> Unit,
    onSyncDeviceIdChange: (String) -> Unit,
    onLoadSyncPendingFiles: () -> Unit,
    onDownloadSyncFile: (SyncPendingFile) -> Unit,
    onDownloadAllSyncFiles: () -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onCreateFavoriteFolder: () -> Unit,
    onRenameFavoriteFolder: (String, String) -> Unit,
    onSetDefaultFavoriteFolder: (String) -> Unit,
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
        BrowserMode.FavoriteFolder -> onSelectFavoriteFolder
    }

    Row(Modifier.fillMaxSize()) {
        FolderRail(
            collections = collections,
            selectedCollectionIds = selectedCollectionIds,
            browserMode = state.browserMode,
            hasAuthors = state.authors.isNotEmpty(),
            hasTags = state.tags.isNotEmpty(),
            favoriteFolders = state.favoriteFolders,
            defaultFavoriteFolderId = state.defaultFavoriteFolderId,
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
            onCreateFavoriteFolder = onCreateFavoriteFolder,
            onRenameFavoriteFolder = onRenameFavoriteFolder,
            onSetDefaultFavoriteFolder = onSetDefaultFavoriteFolder,
            onSelectCollection = onSelectCollection,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.width(214.dp).fillMaxHeight(),
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Crossfade(
            targetState = state.rightPaneMode to selectedCollectionId,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            animationSpec = tween(ViewerUiSpec.FOLDER_SWITCH_DURATION_MILLIS),
            label = "media_collection_switch",
        ) { (rightPaneMode, collectionId) ->
            when (rightPaneMode) {
                RightPaneMode.Settings -> SettingsScreen(
                    selectedTheme = state.appTheme,
                    onSelectTheme = onSelectAppTheme,
                    onOpenBackup = onOpenBackup,
                    onOpenTagSettings = onOpenTagSettings,
                    onOpenSync = onOpenSync,
                    modifier = Modifier.fillMaxSize(),
                )
                RightPaneMode.Tags -> TagSettingsScreen(
                    tags = state.tags,
                    onAddTag = onAddTag,
                    onDeleteTag = onDeleteTag,
                    modifier = Modifier.fillMaxSize(),
                )
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
                                isRecentPlayback = selectedCollection?.id == "recent-playback",
                                onSelectSortField = onSelectMediaSortField,
                                onToggleSortDirection = onToggleMediaSortDirection,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (selectedMediaUrisForDelete.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CancelSelectionButton(
                                        onClick = { onSelectedMediaUrisForDeleteChange(emptySet()) },
                                    )
                                    DeleteSelectedButton(
                                        count = selectedMediaUrisForDelete.size,
                                        onClick = { onDeleteMediaUris(selectedMediaUrisForDelete) },
                                    )
                                }
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
    isRecentPlayback: Boolean,
    onSelectSortField: (MediaSortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("媒体", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        CompactDropdown(
            label = "排序 · ${sortField.label(isRecentPlayback)}",
            options = listOf(
                "名称" to { onSelectSortField(MediaSortField.Name) },
                (if (isRecentPlayback) "播放时间" else "时间") to { onSelectSortField(MediaSortField.ModifiedTime) },
                "爱心" to { onSelectSortField(MediaSortField.FavoriteLevel) },
            ),
        )
        SortDirectionButton(sortDirection = sortDirection, onClick = onToggleSortDirection)
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
private fun CancelSelectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "全部取消",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF34445F))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(8.dp))
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
    Row(modifier = modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompactDropdown(
            label = "排序 · ${sortField.label()}",
            options = buildList {
                add("名称" to { onSelectSortField(CollectionSortField.Name) })
                add("数量" to { onSelectSortField(CollectionSortField.Count) })
                add("时间" to { onSelectSortField(CollectionSortField.ModifiedTime) })
                if (browserMode == BrowserMode.Author) add("爱心" to { onSelectSortField(CollectionSortField.FavoriteLevel) })
            },
        )
        SortDirectionButton(
            sortDirection = sortDirection,
            onClick = onToggleSortDirection,
            compact = true,
        )
    }
}

@Composable
private fun CompactDropdown(label: String, options: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            "$label ▾",
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }.padding(horizontal = 9.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (name, action) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { action(); expanded = false })
            }
        }
    }
}

private fun MediaSortField.label(isRecentPlayback: Boolean = false) = when (this) {
    MediaSortField.Name -> "名称"
    MediaSortField.ModifiedTime -> if (isRecentPlayback) "播放时间" else "时间"
    MediaSortField.FavoriteLevel -> "爱心"
}

private fun CollectionSortField.label() = when (this) {
    CollectionSortField.Name -> "名称"
    CollectionSortField.Count -> "数量"
    CollectionSortField.ModifiedTime -> "时间"
    CollectionSortField.FavoriteLevel -> "爱心"
}

@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun SortDirectionButton(
    sortDirection: SortDirection,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val isAscending = sortDirection == SortDirection.Ascending
    Box(
        modifier = Modifier
            .size(if (compact) 32.dp else 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = if (isAscending) "升序" else "降序",
            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FolderRail(
    collections: List<MediaFolder>,
    selectedCollectionIds: Set<String>,
    browserMode: BrowserMode,
    hasAuthors: Boolean,
    hasTags: Boolean,
    favoriteFolders: List<FavoriteFolder>,
    defaultFavoriteFolderId: String?,
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
    onCreateFavoriteFolder: () -> Unit,
    onRenameFavoriteFolder: (String, String) -> Unit,
    onSetDefaultFavoriteFolder: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        AppRailHeader(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        )
        RailDivider()
        FolderToolbar(
            browserMode = browserMode,
            hasAuthors = hasAuthors,
            hasTags = hasTags,
            onSelectBrowserMode = onSelectBrowserMode,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
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
        if (browserMode == BrowserMode.FavoriteFolder) {
            FavoriteFolderListHeader(onCreateFavoriteFolder = onCreateFavoriteFolder)
            RailDivider()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(collections, key = { it.id }) { collection ->
                val selected = collection.id in selectedCollectionIds
                val rowBackground by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
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
                            if (browserMode == BrowserMode.FavoriteFolder) {
                                val folderId = collection.id.removePrefix("favorite-folder:")
                                Spacer(Modifier.weight(1f))
                                if (favoriteFolders.any { it.id == folderId }) {
                                    FavoriteFolderRowActions(
                                        folderName = collection.name,
                                        isDefault = folderId == defaultFavoriteFolderId,
                                        onSetDefault = { onSetDefaultFavoriteFolder(folderId) },
                                        onRename = { name -> onRenameFavoriteFolder(folderId, name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteFolderListHeader(onCreateFavoriteFolder: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "收藏夹",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onCreateFavoriteFolder() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "新建收藏夹",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FavoriteFolderRowActions(
    folderName: String,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onRename: (String) -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var draftName by remember(folderName) { mutableStateOf(folderName) }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable { if (!isDefault) onSetDefault() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isDefault) "默认收藏夹" else "设为默认收藏夹",
                modifier = Modifier.size(18.dp),
                tint = if (isDefault) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .clickable { showRenameDialog = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "重命名收藏夹",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名收藏夹") },
            text = {
                TextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    label = { Text("收藏夹名称") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRename(draftName)
                    showRenameDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                Text(
                    "取消",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showRenameDialog = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun CollectionSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fieldHeight = if (compact) 34.dp else 48.dp
    val fieldShape = RoundedCornerShape(if (compact) 8.dp else 4.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(fieldHeight),
        singleLine = true,
        textStyle = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
            .copy(color = MaterialTheme.colorScheme.onSurface),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (compact) 0.dp else 10.dp,
                        vertical = if (compact) 0.dp else 5.dp,
                    )
                    .clip(fieldShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        shape = fieldShape,
                    )
                    .padding(start = if (compact) 10.dp else 12.dp, end = 6.dp),
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
                            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
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
                        color = MaterialTheme.colorScheme.onSurface,
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
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun AppRailHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("VideoSee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        RailIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "刷新",
            enabled = !isRefreshing,
            onClick = onRefresh,
        )
        Spacer(Modifier.weight(1f))
        RailIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "设置",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun RailIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val iconColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = iconColor,
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
private fun DefaultFavoriteFolderButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(1.dp, Color(0x99E9C55D), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        RoundedStarIcon(
            selected = isFavorite,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun RoundedStarIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.46f
        val innerRadius = outerRadius * 0.48f
        val vertices = List(10) { index ->
            val angle = Math.toRadians((-90.0 + index * 36.0))
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            Offset(
                x = center.x + (kotlin.math.cos(angle) * radius).toFloat(),
                y = center.y + (kotlin.math.sin(angle) * radius).toFloat(),
            )
        }
        val cornerFraction = 0.18f
        fun between(start: Offset, end: Offset, fraction: Float): Offset = Offset(
            x = start.x + (end.x - start.x) * fraction,
            y = start.y + (end.y - start.y) * fraction,
        )
        val path = Path().apply {
            val firstPrevious = vertices.last()
            val first = vertices.first()
            moveTo(
                between(firstPrevious, first, 1f - cornerFraction).x,
                between(firstPrevious, first, 1f - cornerFraction).y,
            )
            vertices.indices.forEach { index ->
                val previous = vertices[(index + vertices.lastIndex) % vertices.size]
                val current = vertices[index]
                val next = vertices[(index + 1) % vertices.size]
                val before = between(previous, current, 1f - cornerFraction)
                val after = between(current, next, cornerFraction)
                lineTo(before.x, before.y)
                quadraticTo(current.x, current.y, after.x, after.y)
            }
            close()
        }
        if (selected) {
            drawPath(path, color = Color(0xFFE9C55D), style = Fill)
        } else {
            drawPath(path, color = Color.Transparent, style = Fill, blendMode = BlendMode.Clear)
            drawPath(
                path,
                color = Color.White,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
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
private fun ViewerRotateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 25.dp,
    lockSize: Dp = 18.dp,
    isLockActive: Boolean = false,
    onLockClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.size(size),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0x664AA8FF))
                .border(1.dp, Color(0xAA9FD3FF), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                contentDescription = "旋转媒体",
                modifier = Modifier.size(iconSize),
                tint = Color(0xFFE3F2FF),
            )
        }
        if (onLockClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(lockSize)
                    .clip(CircleShape)
                    .background(if (isLockActive) Color(0x99D93025) else Color(0x664AA8FF))
                    .border(
                        1.dp,
                        if (isLockActive) Color(0xBBFFAAA5) else Color(0xAA9FD3FF),
                        CircleShape,
                    )
                    .clickable { onLockClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isLockActive) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = if (isLockActive) "取消锁定默认旋转角度" else "锁定默认旋转角度",
                    modifier = Modifier.size(lockSize * 0.62f),
                    tint = Color.White,
                )
            }
        }
    }
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
                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
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
    onSelectBrowserMode: (BrowserMode) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactDropdown(
            label = browserMode.label(),
            options = buildList {
                add("文件夹" to { onSelectBrowserMode(BrowserMode.Folder) })
                if (hasAuthors) add("作者" to { onSelectBrowserMode(BrowserMode.Author) })
                if (hasTags) add("标签" to { onSelectBrowserMode(BrowserMode.Tag) })
                add("收藏夹" to { onSelectBrowserMode(BrowserMode.FavoriteFolder) })
            },
        )
        CollectionSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun BrowserMode.label() = when (this) {
    BrowserMode.Folder -> "文件夹"
    BrowserMode.Author -> "作者"
    BrowserMode.Tag -> "标签"
    BrowserMode.FavoriteFolder -> "收藏夹"
}

@Composable
private fun BrowserModeButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
            selected -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.onSurface
        },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsScreen(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenTagSettings: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SettingsSection("外观") {
                Text("主题色", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTheme.entries.take(3).forEach { theme -> ThemeTile(theme, selectedTheme == theme) { onSelectTheme(theme) } }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTheme.entries.drop(3).forEach { theme -> ThemeTile(theme, selectedTheme == theme) { onSelectTheme(theme) } }
                }
            }
        }
        item {
            SettingsSection("数据与同步") {
                SettingsAction("数据备份", "导入或导出爱心、标签、收藏夹和精彩片段", onOpenBackup)
                Spacer(Modifier.height(8.dp))
                SettingsAction("标签设置", "新建、整理或删除标签", onOpenTagSettings)
                Spacer(Modifier.height(8.dp))
                SettingsAction("下载", "连接手机同步服务并下载文件", onOpenSync)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ThemeTile(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val color = when (theme) {
        AppTheme.Midnight -> Color(0xFF172033); AppTheme.Graphite -> Color(0xFF303030); AppTheme.Forest -> Color(0xFF203A30)
        AppTheme.Snow -> Color(0xFFF1F5F9); AppTheme.Mist -> Color(0xFFF0EBFA); AppTheme.Sand -> Color(0xFFF7EEDF)
    }
    Column(
        modifier = Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).background(color)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(10.dp),
    ) {
        Text(if (theme.isDark) "深色" else "浅色", color = if (theme.isDark) Color.White else Color(0xFF302015), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Text(theme.label, color = if (theme.isDark) Color.White else Color(0xFF302015), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsAction(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BackupScreen(
    onExportBackup: (String) -> Unit,
    onImportBackup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
    ) {
        Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BackupActionRow(
                title = "导出备份",
                description = "导出爱心、标签、收藏夹和精彩片段",
                onClick = { onExportBackup("all") },
            )
            BackupActionRow(
                title = "导入备份",
                description = "导入爱心、标签、收藏夹和精彩片段",
                onClick = { onImportBackup("all") },
            )
        }
    }
}

@Composable
private fun BackupActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TagSettingsScreen(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTagName by remember { mutableStateOf("") }
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background).padding(14.dp)) {
        Text("标签设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = newTagName, onValueChange = { newTagName = it }, label = { Text("新标签") }, singleLine = true, modifier = Modifier.width(220.dp))
            SortChip("添加", selected = true, onClick = { onAddTag(newTagName); newTagName = "" })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags, key = { it }) { tagName ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
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
            .background(MaterialTheme.colorScheme.background)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
    LaunchedEffect(gridState, items, columnCount) {
        var previousFirstVisibleIndex = -1
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                null
            } else {
                visibleItems.minOf { it.index } to visibleItems.maxOf { it.index }
            }
        }.collectLatest { visibleRange ->
            val (firstVisibleIndex, lastVisibleIndex) = visibleRange ?: return@collectLatest
            // Wait for a fling to settle. Visible cells keep their own higher-priority requests.
            delay(360)
            val prefetchCount = columnCount * 2
            val movingForward = previousFirstVisibleIndex < 0 || firstVisibleIndex >= previousFirstVisibleIndex
            val prefetchRange = if (movingForward) {
                (lastVisibleIndex + 1).coerceAtMost(items.size)..(lastVisibleIndex + prefetchCount).coerceAtMost(items.lastIndex)
            } else {
                (firstVisibleIndex - prefetchCount).coerceAtLeast(0)..(firstVisibleIndex - 1).coerceAtLeast(0)
            }
            if (!prefetchRange.isEmpty()) {
                prefetchRange.forEach { index ->
                    items[index]
                        .takeIf { it.mediaType == MediaType.Video }
                        ?.let(onEnsureVideoThumbnail)
                }
            }
            previousFirstVisibleIndex = firstVisibleIndex
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(Color(0x99000000))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.mediaType == MediaType.Video) {
                            Text(
                                text = "▶ ${item.durationMillis.formatDuration()}",
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 50.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        AuthorFavoritePicker(
                            favoriteLevel = item.favoriteLevel,
                            onFavoriteLevelChange = { level ->
                                onSetMediaFavoriteLevel(item.uri, level)
                            },
                            heartSize = 18.dp,
                            touchTargetSize = 20.dp,
                            spacing = 1.dp,
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
    playbackMode: PlaybackMode,
    onTogglePlaybackMode: () -> Unit,
    videoSegments: List<VideoSegment>,
    onAddVideoSegment: (Long, Long) -> Unit,
    onDeleteVideoSegment: (VideoSegment) -> Unit,
    onRenameVideoSegment: (VideoSegment, String) -> Unit,
    onOpenAuthorSearch: (String) -> Unit,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    isInDefaultFavoriteFolder: Boolean,
    onToggleDefaultFavoriteFolder: () -> Unit,
    allTags: List<String>,
    selectedTags: Set<String>,
    onToggleMediaTag: (String) -> Unit,
    onDeleteCurrentMedia: () -> Unit,
    onEnterMultiVideoMode: () -> Unit,
    onOpenToneCurve: () -> Unit,
    onToneCurveChange: (ToneCurve) -> Unit,
    onOpenColorAdjustments: () -> Unit,
    onColorAdjustmentsChange: (ColorAdjustments) -> Unit,
    videoRotationLockState: VideoRotationLockState,
    onLockVideoRotation: (Int) -> Unit,
    onUnlockVideoRotation: () -> Unit,
    showDeleteAction: Boolean = true,
    onReturnToMultiVideo: (() -> Unit)? = null,
    initialPlaybackPositionMillis: Long = 0L,
    initialPlayWhenReady: Boolean = true,
    initialScale: Float = 1f,
    onVideoPlaybackStateChange: ((Long, Boolean) -> Unit)? = null,
    onViewerScaleChange: ((Float) -> Unit)? = null,
) {
    BackHandler(onBack = onReturnToMultiVideo ?: onClose)
    var scale by remember { mutableFloatStateOf(initialScale) }
    var hasInitializedViewerItem by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var armedBoundaryIntent by remember { mutableStateOf<SwipeIntent?>(null) }
    var boundaryFeedbackText by remember { mutableStateOf<String?>(null) }
    var videoRotationDegrees by remember(item.uri) {
        mutableStateOf(if (videoRotationLockState.isLocked) videoRotationLockState.defaultDegrees else 0)
    }
    var hasVideoRotationStarted by remember(item.uri) {
        mutableStateOf(videoRotationDegrees != 0)
    }

    LaunchedEffect(item.uri) {
        if (!hasInitializedViewerItem) {
            scale = initialScale
            hasInitializedViewerItem = true
        } else if (scale >= 1f) {
            scale = 1f
        }
        offsetX = 0f
        offsetY = 0f
    }

    LaunchedEffect(item.uri, videoRotationLockState.defaultDegrees, videoRotationLockState.isLocked) {
        if (videoRotationLockState.isLocked && item.mediaType.supportsManualRotation) {
            videoRotationDegrees = videoRotationLockState.defaultDegrees
            hasVideoRotationStarted = videoRotationDegrees != 0
        }
    }

    LaunchedEffect(scale) {
        onViewerScaleChange?.invoke(scale)
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
            .pointerInput(item.uri, hasPrevious, hasNext) {
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
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val nextScale = (scale * zoom).coerceIn(MIN_VIEWER_SCALE, MAX_VIEWER_SCALE)
                            scale = nextScale
                            if (nextScale != 1f) {
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
                                    change?.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val intent = if (usedMultiTouch) null else SwipeIntent.fromVerticalDrag(totalSwipe)
                    when (intent) {
                        SwipeIntent.Next -> {
                            if (hasNext) {
                                armedBoundaryIntent = null
                                onNext()
                            } else if (armedBoundaryIntent == SwipeIntent.Next) {
                                boundaryFeedbackText = null
                                armedBoundaryIntent = null
                                onFirst()
                            } else {
                                boundaryFeedbackText = "已经是最后一张了"
                                armedBoundaryIntent = SwipeIntent.Next
                            }
                        }

                        SwipeIntent.Previous -> {
                            if (hasPrevious) {
                                armedBoundaryIntent = null
                                onPrevious()
                            } else if (armedBoundaryIntent == SwipeIntent.Previous) {
                                boundaryFeedbackText = null
                                armedBoundaryIntent = null
                                onLast()
                            } else {
                                boundaryFeedbackText = "已经是第一张了"
                                armedBoundaryIntent = SwipeIntent.Previous
                            }
                        }

                        null -> {
                            armedBoundaryIntent = null
                        }
                    }
                }
            },
    ) {
        val mediaDisplayScale = scale * DEFAULT_SINGLE_MEDIA_DISPLAY_SCALE
        MediaSurface(
            item = item,
            scale = mediaDisplayScale,
            offsetX = offsetX,
            offsetY = offsetY,
            activeVideo = true,
            onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
            playbackMode = playbackMode,
            onTogglePlaybackMode = onTogglePlaybackMode,
            videoSegments = videoSegments,
            onAddVideoSegment = onAddVideoSegment,
            onDeleteVideoSegment = onDeleteVideoSegment,
            onRenameVideoSegment = onRenameVideoSegment,
            onOpenAuthorSearch = onOpenAuthorSearch,
            isInDefaultFavoriteFolder = isInDefaultFavoriteFolder,
            onToggleDefaultFavoriteFolder = onToggleDefaultFavoriteFolder,
            initialPlaybackPositionMillis = initialPlaybackPositionMillis,
            initialPlayWhenReady = initialPlayWhenReady,
            onPlaybackStateChange = onVideoPlaybackStateChange,
            showFileName = scale <= 1f,
            videoRotationDegrees = videoRotationDegrees,
        )

        val actionBottomAnchor = maxHeight / 4
        val showSingleVideoQuickPresets = item.mediaType == MediaType.Video && onReturnToMultiVideo == null
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(8f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (onReturnToMultiVideo != null) {
                ReturnToMultiVideoButton(onClick = onReturnToMultiVideo)
            } else {
                MultiVideoModeButton(onClick = onEnterMultiVideoMode)
            }
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToneCurveButton(onClick = onOpenToneCurve)
                    if (showSingleVideoQuickPresets) {
                        Spacer(Modifier.width(6.dp))
                        ToneCurveQuickPresetBar(
                            toneCurve = LocalToneCurve.current,
                            onToneCurveChange = onToneCurveChange,
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                        )
                    }
                }
                if (showSingleVideoQuickPresets) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorAdjustmentButton(onClick = onOpenColorAdjustments)
                        Spacer(Modifier.width(6.dp))
                        ColorAdjustmentQuickPresetBar(
                            colorAdjustments = LocalColorAdjustments.current,
                            onColorAdjustmentsChange = onColorAdjustmentsChange,
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                        )
                    }
                }
            }
        }
        if (item.mediaType == MediaType.Image) {
            ViewerFavoritePicker(
                favoriteLevel = item.favoriteLevel,
                onFavoriteLevelChange = { level ->
                    onSetMediaFavoriteLevel(item.uri, level)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = actionBottomAnchor - 64.dp),
            )
            InfoButton(
                displayName = item.displayName,
                onOpenAuthorSearch = onOpenAuthorSearch,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        if (item.mediaType == MediaType.Image) {
            DefaultFavoriteFolderButton(
                isFavorite = isInDefaultFavoriteFolder,
                onClick = onToggleDefaultFavoriteFolder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = actionBottomAnchor - 140.dp),
            )
        }
        if (showDeleteAction) {
            if (item.mediaType.supportsManualRotation) {
                ViewerRotateButton(
                    onClick = {
                        if (videoRotationLockState.isLocked) {
                            onUnlockVideoRotation()
                        }
                        videoRotationDegrees = nextVideoRotationDegrees(
                            currentDegrees = videoRotationDegrees,
                            hasStarted = hasVideoRotationStarted,
                        )
                        hasVideoRotationStarted = true
                    },
                    isLockActive = videoRotationLockState.isLocked,
                    onLockClick = {
                        if (videoRotationLockState.isLocked) {
                            onUnlockVideoRotation()
                        } else {
                            onLockVideoRotation(videoRotationDegrees)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = actionBottomAnchor + 110.dp),
                )
            }
            ViewerDeleteButton(
                onClick = onDeleteCurrentMedia,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = actionBottomAnchor + 54.dp),
            )
        }
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

@Composable
private fun MultiVideoModeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xB3151A22))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(15.dp)) {
            val gap = 2.dp.toPx()
            val dotSize = (size.minDimension - gap * 2f) / 3f
            val color = Color.White
            repeat(3) { row ->
                repeat(3) { column ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(
                            x = column * (dotSize + gap),
                            y = row * (dotSize + gap),
                        ),
                        size = androidx.compose.ui.geometry.Size(dotSize, dotSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReturnToMultiVideoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "返回多视频模式",
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xA9284B77))
            .border(1.dp, Color(0x886FB7FF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ToneCurveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "曲线",
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xAA242B38))
            .border(1.dp, Color(0x667DD3FC), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ColorAdjustmentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "颜色",
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xAA242B38))
            .border(1.dp, Color(0x667DD3FC), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ToneCurveQuickPresetBar(
    toneCurve: ToneCurve,
    onToneCurveChange: (ToneCurve) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val presetStore = remember(context) { ToneCurvePresetStore(context) }
    var presets by remember(presetStore) { mutableStateOf(presetStore.load()) }

    DisposableEffect(presetStore) {
        val stopObserving = presetStore.observe {
            presets = presetStore.load()
        }
        onDispose(stopObserving)
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            ToneCurveQuickPresetButton(
                label = "默认",
                selected = toneCurve.isIdentity,
                onClick = { onToneCurveChange(ToneCurve()) },
            )
        }
        items(presets, key = { it.id }) { preset ->
            ToneCurveQuickPresetButton(
                label = preset.name,
                selected = preset.curve.isCloseTo(toneCurve),
                onClick = { onToneCurveChange(preset.curve) },
            )
        }
    }
}

@Composable
private fun ColorAdjustmentQuickPresetBar(
    colorAdjustments: ColorAdjustments,
    onColorAdjustmentsChange: (ColorAdjustments) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val presetStore = remember(context) { ColorAdjustmentPresetStore(context) }
    var presets by remember(presetStore) { mutableStateOf(presetStore.load()) }

    DisposableEffect(presetStore) {
        val stopObserving = presetStore.observe {
            presets = presetStore.load()
        }
        onDispose(stopObserving)
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            ToneCurveQuickPresetButton(
                label = "默认",
                selected = colorAdjustments.isIdentity,
                onClick = { onColorAdjustmentsChange(ColorAdjustments()) },
            )
        }
        items(presets, key = { it.id }) { preset ->
            ToneCurveQuickPresetButton(
                label = preset.name,
                selected = preset.adjustments.isCloseTo(colorAdjustments),
                onClick = { onColorAdjustmentsChange(preset.adjustments) },
            )
        }
    }
}

@Composable
private fun ToneCurveQuickPresetButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    val background = if (selected) Color(0x883B82B6) else Color.Transparent
    val border = if (selected) Color(0xFF90D8FF) else Color(0xDDEAF2FF)
    Box(
        modifier = Modifier
            .height(24.dp)
            .widthIn(min = 34.dp, max = 72.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ToneCurvePanel(
    toneCurve: ToneCurve,
    onToneCurveChange: (ToneCurve) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val presetStore = remember(context) { ToneCurvePresetStore(context) }
    var presets by remember { mutableStateOf(presetStore.load()) }
    var expandedPresetId by remember { mutableStateOf<String?>(null) }
    val initialActivePresetId = remember { presets.firstOrNull { it.curve.isCloseTo(toneCurve) }?.id }
    var activePresetId by remember { mutableStateOf<String?>(initialActivePresetId) }
    var defaultPresetCurve by remember {
        mutableStateOf(if (initialActivePresetId == null) toneCurve else ToneCurve())
    }
    var renamingPresetId by remember { mutableStateOf<String?>(null) }
    var presetNameDraft by remember { mutableStateOf("") }
    var isPresetNameDialogVisible by remember { mutableStateOf(false) }
    var activePoint by remember { mutableStateOf<Int?>(null) }
    val latestToneCurve by rememberUpdatedState(toneCurve)
    val latestActivePresetId by rememberUpdatedState(activePresetId)
    val latestPresets by rememberUpdatedState(presets)
    val panelHeight = 446.dp
    val panelHeightPx = with(LocalDensity.current) { panelHeight.toPx() }
    val updateToneCurve: (ToneCurve) -> Unit = { nextCurve ->
        onToneCurveChange(nextCurve)
        val currentPresetId = activePresetId
        if (currentPresetId == null) {
            defaultPresetCurve = nextCurve
        } else {
            val nextPresets = presets.map { preset ->
                if (preset.id == currentPresetId) preset.copy(curve = nextCurve) else preset
            }
            presets = nextPresets
            presetStore.save(nextPresets)
        }
    }
    val latestToneCurveChange by rememberUpdatedState<(ToneCurve) -> Unit> { nextCurve ->
        onToneCurveChange(nextCurve)
        val currentPresetId = latestActivePresetId
        if (currentPresetId == null) {
            defaultPresetCurve = nextCurve
        } else {
            val nextPresets = latestPresets.map { preset ->
                if (preset.id == currentPresetId) preset.copy(curve = nextCurve) else preset
            }
            presets = nextPresets
            presetStore.save(nextPresets)
        }
    }
    val nodeInset = with(LocalDensity.current) { 12.dp.toPx() }
    val curveColor = Color(0xFFF4F7FB)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(40f)
            .pointerInput(panelHeightPx) {
                detectTapGestures { offset ->
                    if (offset.y < size.height - panelHeightPx) {
                        onDismiss()
                    }
                }
            }
            .background(Color(0x22000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .background(Color(0xF4272B31))
                .border(1.dp, Color(0x334F5968))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("曲线 · RGB", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "重置",
                        modifier = Modifier.clickable { updateToneCurve(ToneCurve()) },
                        color = Color(0xFFB9C3D2),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "完成",
                        modifier = Modifier.clickable {
                            onDismiss()
                        },
                        color = Color(0xFF8FD7FF),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "可输入百分比，或在数值框内上下滑动微调；5 个点均可拖动",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFF9EA9B9),
                style = MaterialTheme.typography.bodySmall,
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    ToneCurvePresetAddButton(
                        onClick = {
                            renamingPresetId = null
                            presetNameDraft = ""
                            isPresetNameDialogVisible = true
                        },
                    )
                }
                item {
                    ToneCurveDefaultPresetButton(
                        selected = activePresetId == null,
                        onClick = {
                            expandedPresetId = null
                            activePresetId = null
                            onToneCurveChange(defaultPresetCurve)
                        },
                    )
                }
                items(presets, key = { it.id }) { preset ->
                    ToneCurvePresetButton(
                        preset = preset,
                        selected = activePresetId == preset.id,
                        showActions = expandedPresetId == preset.id,
                        onApply = {
                            expandedPresetId = null
                            activePresetId = preset.id
                            onToneCurveChange(preset.curve)
                        },
                        onLongClick = {
                            expandedPresetId = if (expandedPresetId == preset.id) null else preset.id
                        },
                        onRename = {
                            expandedPresetId = null
                            renamingPresetId = preset.id
                            presetNameDraft = preset.name
                            isPresetNameDialogVisible = true
                        },
                        onDelete = {
                            val next = presets.filterNot { it.id == preset.id }
                            presets = next
                            presetStore.save(next)
                            if (activePresetId == preset.id) {
                                activePresetId = null
                                onToneCurveChange(defaultPresetCurve)
                            }
                            if (renamingPresetId == preset.id) renamingPresetId = null
                            expandedPresetId = null
                        },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val labels = listOf("黑场", "暗部", "中调", "亮部", "白场")
                toneCurve.pointValues.forEachIndexed { index, value ->
                    ToneCurveValueEditor(
                        label = labels[index],
                        value = value,
                        onValueChange = { nextValue ->
                            updateToneCurve(toneCurve.withPoint(index, nextValue))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 14.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val curve = latestToneCurve
                                val graphWidth = (size.width - nodeInset * 2f).coerceAtLeast(1f)
                                val graphHeight = (size.height - nodeInset * 2f).coerceAtLeast(1f)
                                activePoint = curve.pointValues.indices.minByOrNull { index ->
                                    val x = nodeInset + graphWidth * ToneCurve.POINT_X[index]
                                    val y = size.height - nodeInset - graphHeight * curve.pointValues[index]
                                    val dx = x - offset.x
                                    val dy = y - offset.y
                                    dx * dx + dy * dy
                                }
                            },
                            onDragEnd = { activePoint = null },
                            onDragCancel = { activePoint = null },
                        ) { change, _ ->
                            val point = activePoint ?: return@detectDragGestures
                            val graphHeight = (size.height - nodeInset * 2f).coerceAtLeast(1f)
                            val nextValue = ((size.height - nodeInset - change.position.y) / graphHeight)
                                .coerceIn(0f, 1f)
                            latestToneCurveChange(latestToneCurve.withPoint(point, nextValue))
                            change.consume()
                        }
                    },
            ) {
                val graphLeft = nodeInset
                val graphTop = nodeInset
                val graphRight = size.width - nodeInset
                val graphBottom = size.height - nodeInset
                val graphWidth = (graphRight - graphLeft).coerceAtLeast(1f)
                val graphHeight = (graphBottom - graphTop).coerceAtLeast(1f)
                val x = ToneCurve.POINT_X.map { graphLeft + graphWidth * it }.toFloatArray()
                val y = toneCurve.pointValues
                repeat(5) { index ->
                    val horizontalProgress = index / 4f
                    val verticalProgress = index / 4f
                    drawLine(
                        Color(0x333E4B5C),
                        Offset(graphLeft + graphWidth * horizontalProgress, graphTop),
                        Offset(graphLeft + graphWidth * horizontalProgress, graphBottom),
                        1.dp.toPx(),
                    )
                    drawLine(
                        Color(0x333E4B5C),
                        Offset(graphLeft, graphTop + graphHeight * verticalProgress),
                        Offset(graphRight, graphTop + graphHeight * verticalProgress),
                        1.dp.toPx(),
                    )
                }
                val path = Path().apply {
                    moveTo(x.first(), graphBottom - graphHeight * y.first())
                    for (index in 1 until x.size) {
                        lineTo(x[index], graphBottom - graphHeight * y[index])
                    }
                }
                drawPath(path, curveColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                for (index in y.indices) {
                    drawCircle(
                        color = Color(0xFF15191F),
                        radius = 9.dp.toPx(),
                        center = Offset(x[index], graphBottom - graphHeight * y[index]),
                    )
                    drawCircle(
                        color = curveColor,
                        radius = 5.dp.toPx(),
                        center = Offset(x[index], graphBottom - graphHeight * y[index]),
                    )
                }
            }
        }
        if (isPresetNameDialogVisible) {
            val renamingPreset = presets.firstOrNull { it.id == renamingPresetId }
            AlertDialog(
                onDismissRequest = {
                    isPresetNameDialogVisible = false
                    renamingPresetId = null
                },
                title = { Text(if (renamingPreset == null) "保存曲线预设" else "重命名曲线预设") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (renamingPreset == null) {
                                "给这一组曲线参数起个名字，之后可一键套用。"
                            } else {
                                "只修改这个参数组的名称，不改变当前曲线参数。"
                            },
                        )
                        TextField(
                            value = presetNameDraft,
                            onValueChange = { presetNameDraft = it },
                            singleLine = true,
                            label = { Text("预设名称") },
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = presetNameDraft.trim().ifBlank {
                                if (renamingPreset == null) "曲线预设 ${presets.size + 1}" else renamingPreset.name
                            }
                            val next = if (renamingPreset == null) {
                                val id = UUID.randomUUID().toString()
                                activePresetId = id
                                presets + ToneCurvePreset(
                                    id = id,
                                    name = name,
                                    curve = toneCurve,
                                )
                            } else {
                                presets.map { preset ->
                                    if (preset.id == renamingPreset.id) {
                                        preset.copy(name = name)
                                    } else {
                                        preset
                                    }
                                }
                            }
                            presets = next
                            presetStore.save(next)
                            isPresetNameDialogVisible = false
                            renamingPresetId = null
                        },
                    ) { Text(if (renamingPreset == null) "保存" else "重命名") }
                },
                dismissButton = {
                    Text(
                        "取消",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isPresetNameDialogVisible = false
                                renamingPresetId = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorAdjustmentPanel(
    colorAdjustments: ColorAdjustments,
    onColorAdjustmentsChange: (ColorAdjustments) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val presetStore = remember(context) { ColorAdjustmentPresetStore(context) }
    var presets by remember { mutableStateOf(presetStore.load()) }
    var expandedPresetId by remember { mutableStateOf<String?>(null) }
    val initialActivePresetId = remember {
        presets.firstOrNull { it.adjustments.isCloseTo(colorAdjustments) }?.id
    }
    var activePresetId by remember { mutableStateOf<String?>(initialActivePresetId) }
    var defaultPresetAdjustments by remember {
        mutableStateOf(if (initialActivePresetId == null) colorAdjustments else ColorAdjustments())
    }
    var renamingPresetId by remember { mutableStateOf<String?>(null) }
    var presetNameDraft by remember { mutableStateOf("") }
    var isPresetNameDialogVisible by remember { mutableStateOf(false) }
    val latestActivePresetId by rememberUpdatedState(activePresetId)
    val latestPresets by rememberUpdatedState(presets)
    val panelHeight = 446.dp
    val panelHeightPx = with(LocalDensity.current) { panelHeight.toPx() }
    val updateAdjustments: (ColorAdjustments) -> Unit = { nextAdjustments ->
        onColorAdjustmentsChange(nextAdjustments)
        val currentPresetId = activePresetId
        if (currentPresetId == null) {
            defaultPresetAdjustments = nextAdjustments
        } else {
            val nextPresets = presets.map { preset ->
                if (preset.id == currentPresetId) preset.copy(adjustments = nextAdjustments) else preset
            }
            presets = nextPresets
            presetStore.save(nextPresets)
        }
    }
    val latestAdjustmentChange by rememberUpdatedState<(ColorAdjustments) -> Unit> { nextAdjustments ->
        onColorAdjustmentsChange(nextAdjustments)
        val currentPresetId = latestActivePresetId
        if (currentPresetId == null) {
            defaultPresetAdjustments = nextAdjustments
        } else {
            val nextPresets = latestPresets.map { preset ->
                if (preset.id == currentPresetId) preset.copy(adjustments = nextAdjustments) else preset
            }
            presets = nextPresets
            presetStore.save(nextPresets)
        }
    }
    val controls = listOf(
        ColorAdjustmentControl("锐度", 0f..1f, colorAdjustments.sharpness) {
            colorAdjustments.copy(sharpness = it)
        },
        ColorAdjustmentControl("对比度", -1f..1f, colorAdjustments.contrast) {
            colorAdjustments.copy(contrast = it)
        },
        ColorAdjustmentControl("自然饱和度", -1f..1f, colorAdjustments.vibrance) {
            colorAdjustments.copy(vibrance = it)
        },
        ColorAdjustmentControl("红色系", -1f..1f, colorAdjustments.redAccent) {
            colorAdjustments.copy(redAccent = it)
        },
        ColorAdjustmentControl("蓝色系", -1f..1f, colorAdjustments.blueAccent) {
            colorAdjustments.copy(blueAccent = it)
        },
        ColorAdjustmentControl("紫色系", -1f..1f, colorAdjustments.purpleAccent) {
            colorAdjustments.copy(purpleAccent = it)
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(40f)
            .pointerInput(panelHeightPx) {
                detectTapGestures { offset ->
                    if (offset.y < size.height - panelHeightPx) {
                        onDismiss()
                    }
                }
            }
            .background(Color(0x22000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .background(Color(0xF4272B31))
                .border(1.dp, Color(0x334F5968))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("颜色", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "重置",
                        modifier = Modifier.clickable { updateAdjustments(ColorAdjustments()) },
                        color = Color(0xFFB9C3D2),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "完成",
                        modifier = Modifier.clickable { onDismiss() },
                        color = Color(0xFF8FD7FF),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "正值增强，负值减淡；红/蓝/紫只命中对应色系",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFF9EA9B9),
                style = MaterialTheme.typography.bodySmall,
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    ToneCurvePresetAddButton(
                        onClick = {
                            renamingPresetId = null
                            presetNameDraft = ""
                            isPresetNameDialogVisible = true
                        },
                    )
                }
                item {
                    ToneCurveDefaultPresetButton(
                        selected = activePresetId == null,
                        onClick = {
                            expandedPresetId = null
                            activePresetId = null
                            onColorAdjustmentsChange(defaultPresetAdjustments)
                        },
                    )
                }
                items(presets, key = { it.id }) { preset ->
                    ColorAdjustmentPresetButton(
                        name = preset.name,
                        selected = activePresetId == preset.id,
                        showActions = expandedPresetId == preset.id,
                        onApply = {
                            expandedPresetId = null
                            activePresetId = preset.id
                            onColorAdjustmentsChange(preset.adjustments)
                        },
                        onLongClick = {
                            expandedPresetId = if (expandedPresetId == preset.id) null else preset.id
                        },
                        onRename = {
                            expandedPresetId = null
                            renamingPresetId = preset.id
                            presetNameDraft = preset.name
                            isPresetNameDialogVisible = true
                        },
                        onDelete = {
                            val next = presets.filterNot { it.id == preset.id }
                            presets = next
                            presetStore.save(next)
                            if (activePresetId == preset.id) {
                                activePresetId = null
                                onColorAdjustmentsChange(defaultPresetAdjustments)
                            }
                            if (renamingPresetId == preset.id) renamingPresetId = null
                            expandedPresetId = null
                        },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                controls.forEach { control ->
                    ColorAdjustmentValueEditor(
                        label = control.label,
                        value = control.value,
                        range = control.range,
                        onValueChange = { nextValue ->
                            latestAdjustmentChange(control.copyValue(nextValue))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (isPresetNameDialogVisible) {
            val renamingPreset = presets.firstOrNull { it.id == renamingPresetId }
            AlertDialog(
                onDismissRequest = {
                    isPresetNameDialogVisible = false
                    renamingPresetId = null
                },
                title = { Text(if (renamingPreset == null) "保存颜色预设" else "重命名颜色预设") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (renamingPreset == null) {
                                "给这一组颜色参数起个名字，之后可一键套用。"
                            } else {
                                "只修改这个参数组的名称，不改变当前颜色参数。"
                            },
                        )
                        TextField(
                            value = presetNameDraft,
                            onValueChange = { presetNameDraft = it },
                            singleLine = true,
                            label = { Text("预设名称") },
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = presetNameDraft.trim().ifBlank {
                                if (renamingPreset == null) "颜色预设 ${presets.size + 1}" else renamingPreset.name
                            }
                            val next = if (renamingPreset == null) {
                                val id = UUID.randomUUID().toString()
                                activePresetId = id
                                presets + ColorAdjustmentPreset(
                                    id = id,
                                    name = name,
                                    adjustments = colorAdjustments,
                                )
                            } else {
                                presets.map { preset ->
                                    if (preset.id == renamingPreset.id) preset.copy(name = name) else preset
                                }
                            }
                            presets = next
                            presetStore.save(next)
                            isPresetNameDialogVisible = false
                            renamingPresetId = null
                        },
                    ) { Text(if (renamingPreset == null) "保存" else "重命名") }
                },
                dismissButton = {
                    Text(
                        "取消",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isPresetNameDialogVisible = false
                                renamingPresetId = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                },
            )
        }
    }
}

private data class ColorAdjustmentControl(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val value: Float,
    val copyValue: (Float) -> ColorAdjustments,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorAdjustmentPresetButton(
    name: String,
    selected: Boolean,
    showActions: Boolean,
    onApply: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (selected) Color(0xFF274B63) else Color(0xFF1B222C)
    val border = if (selected) Color(0xCC8FD7FF) else Color(0x334F5968)
    val textColor = if (selected) Color(0xFFE7F7FF) else Color(0xFFEAF2FF)
    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 56.dp, max = 156.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(background)
                .border(1.dp, border, RoundedCornerShape(9.dp))
                .combinedClickable(onClick = onApply, onLongClick = onLongClick)
                .padding(horizontal = if (showActions) 18.dp else 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showActions) {
            ToneCurvePresetActionButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "重命名预设",
                tint = Color(0xFFA9D8FF),
                background = Color(0x553A7CA8),
                modifier = Modifier.align(Alignment.TopStart),
                onClick = onRename,
            )
            ToneCurvePresetActionButton(
                icon = Icons.Rounded.Close,
                contentDescription = "删除预设",
                tint = Color(0xFFFFB0B0),
                background = Color(0x55A84343),
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ColorAdjustmentValueEditor(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val knobSize = 30.dp
    val valueRange = (range.endInclusive - range.start).coerceAtLeast(0.001f)
    val fraction = ((value - range.start) / valueRange).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFB7C2D1),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF151D28)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatColorAdjustmentValue(value),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            ParameterStepButtons(
                onIncrement = {
                    onValueChange((value + PARAMETER_STEP).coerceIn(range.start, range.endInclusive))
                },
                onDecrement = {
                    onValueChange((value - PARAMETER_STEP).coerceIn(range.start, range.endInclusive))
                },
                modifier = Modifier.height(44.dp),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .padding(top = 10.dp)
                .height(214.dp)
                .fillMaxWidth()
                .pointerInput(range) {
                    fun valueAt(y: Float): Float {
                        val knobPx = with(density) { knobSize.toPx() }
                        val usableHeight = (size.height - knobPx).coerceAtLeast(1f)
                        val knobTop = (y - knobPx / 2f).coerceIn(0f, usableHeight)
                        val nextFraction = 1f - knobTop / usableHeight
                        return (range.start + valueRange * nextFraction)
                            .coerceIn(range.start, range.endInclusive)
                    }
                    detectDragGestures(
                        onDragStart = { offset -> onValueChange(valueAt(offset.y)) },
                    ) { change, _ ->
                        onValueChange(valueAt(change.position.y))
                        change.consume()
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            val knobOffsetY = with(density) {
                ((1f - fraction) * (maxHeight.toPx() - knobSize.toPx()))
                    .roundToInt()
                    .coerceAtLeast(0)
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .padding(vertical = knobSize / 2)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF121923)),
            )
            if (range.start < 0f && range.endInclusive > 0f) {
                val zeroFraction = ((0f - range.start) / valueRange).coerceIn(0f, 1f)
                val zeroOffsetY = with(density) {
                    ((1f - zeroFraction) * (maxHeight.toPx() - 1.dp.toPx()))
                        .roundToInt()
                        .coerceAtLeast(0)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, zeroOffsetY) }
                        .width(26.dp)
                        .height(1.dp)
                        .background(Color(0x66788894)),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, knobOffsetY) }
                    .size(knobSize)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF8FD7FF))
                    .border(2.dp, Color(0xFFEAF7FF), RoundedCornerShape(15.dp)),
            )
        }
    }
}

private fun formatColorAdjustmentValue(value: Float): String {
    return (value * 100f).roundToInt().toString()
}

@Composable
private fun ToneCurvePresetAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF253446))
            .border(1.dp, Color(0x668FD7FF), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "保存当前曲线",
            tint = Color(0xFFBDE6FF),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ToneCurveDefaultPresetButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) Color(0xFF274B63) else Color(0xFF1B222C)
    val border = if (selected) Color(0xCC8FD7FF) else Color(0x334F5968)
    val textColor = if (selected) Color(0xFFE7F7FF) else Color(0xFFEAF2FF)
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "默认",
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToneCurvePresetButton(
    preset: ToneCurvePreset,
    selected: Boolean,
    showActions: Boolean,
    onApply: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (selected) Color(0xFF274B63) else Color(0xFF1B222C)
    val border = if (selected) Color(0xCC8FD7FF) else Color(0x334F5968)
    val textColor = if (selected) Color(0xFFE7F7FF) else Color(0xFFEAF2FF)
    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 56.dp, max = 156.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(background)
                .border(1.dp, border, RoundedCornerShape(9.dp))
                .combinedClickable(onClick = onApply, onLongClick = onLongClick)
                .padding(horizontal = if (showActions) 18.dp else 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                preset.name,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showActions) {
            ToneCurvePresetActionButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "重命名预设",
                tint = Color(0xFFA9D8FF),
                background = Color(0x553A7CA8),
                modifier = Modifier.align(Alignment.TopStart),
                onClick = onRename,
            )
            ToneCurvePresetActionButton(
                icon = Icons.Rounded.Close,
                contentDescription = "删除预设",
                tint = Color(0xFFFFB0B0),
                background = Color(0x55A83A3A),
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ToneCurvePresetActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(17.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun ToneCurveValueEditor(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(formatToneCurveValue(value)) }
    var isFocused by remember { mutableStateOf(false) }
    var dragStartValue by remember { mutableFloatStateOf(0f) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val latestValue by rememberUpdatedState(value)
    val latestValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = formatToneCurveValue(value)
    }

    Column(modifier = modifier) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF9EA9B9),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    text = input
                    input.toFloatOrNull()?.let { percent ->
                        onValueChange((percent / 100f).coerceIn(0f, 1f))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            text.toFloatOrNull()?.let { percent ->
                                onValueChange((percent / 100f).coerceIn(0f, 1f))
                            }
                            text = formatToneCurveValue(value)
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragStartValue = latestValue
                                dragDistance = 0f
                            },
                        ) { change, dragAmount ->
                            dragDistance += dragAmount
                            val nextValue = (dragStartValue - dragDistance / 500f).coerceIn(0f, 1f)
                            latestValueChange(nextValue)
                            text = formatToneCurveValue(nextValue)
                            change.consume()
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B222C))
                            .border(1.dp, if (isFocused) Color(0xFF82D4FF) else Color(0x334F5968), RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        innerTextField()
                    }
                },
            )
            ParameterStepButtons(
                onIncrement = {
                    val nextValue = (value + PARAMETER_STEP).coerceIn(0f, 1f)
                    onValueChange(nextValue)
                    text = formatToneCurveValue(nextValue)
                },
                onDecrement = {
                    val nextValue = (value - PARAMETER_STEP).coerceIn(0f, 1f)
                    onValueChange(nextValue)
                    text = formatToneCurveValue(nextValue)
                },
                modifier = Modifier.height(44.dp),
            )
        }
    }
}

@Composable
private fun ParameterStepButtons(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(34.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ParameterStepButton(
            icon = Icons.Rounded.ArrowUpward,
            contentDescription = "增加 1",
            onClick = onIncrement,
            modifier = Modifier.weight(1f),
        )
        ParameterStepButton(
            icon = Icons.Rounded.ArrowDownward,
            contentDescription = "减少 1",
            onClick = onDecrement,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ParameterStepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF182333))
            .border(1.dp, Color(0x668FD7FF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFBDE6FF),
            modifier = Modifier.size(14.dp),
        )
    }
}

private const val PARAMETER_STEP = 0.01f

private fun formatToneCurveValue(value: Float): String {
    val percent = (value.coerceIn(0f, 1f) * 100f)
    return if (kotlin.math.abs(percent - percent.roundToInt()) < 0.05f) {
        percent.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
}

@Composable
private fun MultiMediaViewer(
    session: MultiMediaSession,
    videoSegmentsByUri: Map<String, List<VideoSegment>>,
    onPickMediaSlot: (MultiMediaSlot) -> Unit,
    onSearchAuthor: (MultiMediaSlot, String) -> Unit,
    onExpandMediaSlot: (MultiMediaSlot, Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>) -> Unit,
    onRemoveMediaSlot: (MultiMediaSlot, Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>) -> Unit,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    onPanePlaybackStateChange: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onOpenTemporarySingleMedia: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onOpenToneCurve: () -> Unit,
    videoRotationLockState: VideoRotationLockState,
    onLockVideoRotation: (Int) -> Unit,
    onUnlockVideoRotation: () -> Unit,
    onExit: () -> Unit,
) {
    var topPaneFraction by remember { mutableFloatStateOf(0.5f) }
    var topColumnFraction by remember { mutableFloatStateOf(0.5f) }
    var bottomColumnFraction by remember { mutableFloatStateOf(0.5f) }
    var playbackSnapshots by remember { mutableStateOf<Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>>(emptyMap()) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        Column(Modifier.fillMaxSize()) {
            MultiMediaRow(
                leftSlot = MultiMediaSlot.TopLeft,
                rightSlot = MultiMediaSlot.TopRight,
                leftPane = session.topLeft,
                rightPane = session.topRight,
                isRightExpanded = session.topRightExpanded,
                leftFraction = topColumnFraction,
                onLeftFractionChange = { topColumnFraction = it },
                onPickMediaSlot = onPickMediaSlot,
                onSearchAuthor = onSearchAuthor,
                onExpandMediaSlot = { slot ->
                    topColumnFraction = 0.5f
                    onExpandMediaSlot(slot, playbackSnapshots)
                },
                onRemoveMediaSlot = { slot -> onRemoveMediaSlot(slot, playbackSnapshots) },
                onPaneItemChange = onPaneItemChange,
                onPanePlaybackStateChange = onPanePlaybackStateChange,
                onPanePlaybackSnapshot = { slot, positionMillis, playWhenReady, scale ->
                    playbackSnapshots = playbackSnapshots + (
                        slot to MultiMediaPlaybackSnapshot(positionMillis, playWhenReady, scale)
                    )
                },
                onOpenTemporarySingleMedia = onOpenTemporarySingleMedia,
                videoSegmentsByUri = videoSegmentsByUri,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                modifier = Modifier.weight(topPaneFraction),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color(0xFF10151E))
                    .pointerInput(viewportHeightPx) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            topPaneFraction = (topPaneFraction + dragAmount / viewportHeightPx)
                                .coerceIn(0.2f, 0.8f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0x99FFFFFF)),
                )
            }
            MultiMediaRow(
                leftSlot = MultiMediaSlot.BottomLeft,
                rightSlot = MultiMediaSlot.BottomRight,
                leftPane = session.bottomLeft,
                rightPane = session.bottomRight,
                isRightExpanded = session.bottomRightExpanded,
                leftFraction = bottomColumnFraction,
                onLeftFractionChange = { bottomColumnFraction = it },
                onPickMediaSlot = onPickMediaSlot,
                onSearchAuthor = onSearchAuthor,
                onExpandMediaSlot = { slot ->
                    bottomColumnFraction = 0.5f
                    onExpandMediaSlot(slot, playbackSnapshots)
                },
                onRemoveMediaSlot = { slot -> onRemoveMediaSlot(slot, playbackSnapshots) },
                onPaneItemChange = onPaneItemChange,
                onPanePlaybackStateChange = onPanePlaybackStateChange,
                onPanePlaybackSnapshot = { slot, positionMillis, playWhenReady, scale ->
                    playbackSnapshots = playbackSnapshots + (
                        slot to MultiMediaPlaybackSnapshot(positionMillis, playWhenReady, scale)
                    )
                },
                onOpenTemporarySingleMedia = onOpenTemporarySingleMedia,
                videoSegmentsByUri = videoSegmentsByUri,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                modifier = Modifier.weight(1f - topPaneFraction),
            )
        }
        Text(
            "单屏",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xB3151A22))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                .clickable { onExit() }
                .padding(horizontal = 11.dp, vertical = 7.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        ToneCurveButton(
            onClick = onOpenToneCurve,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 76.dp, top = 14.dp),
        )
    }
}

@Composable
private fun MultiMediaRow(
    leftSlot: MultiMediaSlot,
    rightSlot: MultiMediaSlot,
    leftPane: MultiMediaPaneSession?,
    rightPane: MultiMediaPaneSession?,
    isRightExpanded: Boolean,
    leftFraction: Float,
    onLeftFractionChange: (Float) -> Unit,
    onPickMediaSlot: (MultiMediaSlot) -> Unit,
    onSearchAuthor: (MultiMediaSlot, String) -> Unit,
    onExpandMediaSlot: (MultiMediaSlot) -> Unit,
    onRemoveMediaSlot: (MultiMediaSlot) -> Unit,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    onPanePlaybackStateChange: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onPanePlaybackSnapshot: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    onOpenTemporarySingleMedia: (MultiMediaSlot, Long, Boolean, Float) -> Unit,
    videoSegmentsByUri: Map<String, List<VideoSegment>>,
    videoRotationLockState: VideoRotationLockState,
    onLockVideoRotation: (Int) -> Unit,
    onUnlockVideoRotation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isRightExpanded) {
        Box(modifier = modifier.fillMaxWidth()) {
            MultiMediaPaneOrEmpty(
                pane = leftPane,
                slot = leftSlot,
                onPickMediaSlot = onPickMediaSlot,
                onPaneItemChange = onPaneItemChange,
                onSearchAuthor = { authorId -> onSearchAuthor(leftSlot, authorId) },
                onPlaybackStateChange = { position, shouldPlay, scale ->
                    onPanePlaybackStateChange(leftSlot, position, shouldPlay, scale)
                },
                onPlaybackSnapshot = { position, shouldPlay, scale ->
                    onPanePlaybackSnapshot(leftSlot, position, shouldPlay, scale)
                },
                onOpenTemporarySingle = { position, shouldPlay, scale ->
                    onOpenTemporarySingleMedia(leftSlot, position, shouldPlay, scale)
                },
                onSelectNewMedia = { onPickMediaSlot(leftSlot) },
                onRemove = { onRemoveMediaSlot(leftSlot) },
                videoSegmentsByUri = videoSegmentsByUri,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                modifier = Modifier.fillMaxSize(),
            )
            MultiMediaRowAddButton(
                onClick = { onExpandMediaSlot(rightSlot) },
                modifier = Modifier
                    .align(Alignment.CenterEnd),
            )
        }
        return
    }

    val density = LocalDensity.current
    val latestLeftFraction by rememberUpdatedState(leftFraction)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        Row(Modifier.fillMaxSize()) {
            MultiMediaPaneOrEmpty(
                pane = leftPane,
                slot = leftSlot,
                onPickMediaSlot = onPickMediaSlot,
                onPaneItemChange = onPaneItemChange,
                onSearchAuthor = { authorId -> onSearchAuthor(leftSlot, authorId) },
                onPlaybackStateChange = { position, shouldPlay, scale ->
                    onPanePlaybackStateChange(leftSlot, position, shouldPlay, scale)
                },
                onPlaybackSnapshot = { position, shouldPlay, scale ->
                    onPanePlaybackSnapshot(leftSlot, position, shouldPlay, scale)
                },
                onOpenTemporarySingle = { position, shouldPlay, scale ->
                    onOpenTemporarySingleMedia(leftSlot, position, shouldPlay, scale)
                },
                onSelectNewMedia = { onPickMediaSlot(leftSlot) },
                onRemove = { onRemoveMediaSlot(leftSlot) },
                videoSegmentsByUri = videoSegmentsByUri,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                modifier = Modifier
                    .weight(leftFraction)
                    .fillMaxHeight(),
            )
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF10151E))
                    .pointerInput(viewportWidthPx) {
                        var draggedFraction = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { draggedFraction = latestLeftFraction },
                        ) { change, dragAmount ->
                            change.consume()
                            draggedFraction = (draggedFraction + dragAmount / viewportWidthPx)
                                .coerceIn(0.2f, 0.8f)
                            onLeftFractionChange(draggedFraction)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0x99FFFFFF)),
                )
            }
            MultiMediaPaneOrEmpty(
                pane = rightPane,
                slot = rightSlot,
                onPickMediaSlot = onPickMediaSlot,
                onPaneItemChange = onPaneItemChange,
                onSearchAuthor = { authorId -> onSearchAuthor(rightSlot, authorId) },
                onPlaybackStateChange = { position, shouldPlay, scale ->
                    onPanePlaybackStateChange(rightSlot, position, shouldPlay, scale)
                },
                onPlaybackSnapshot = { position, shouldPlay, scale ->
                    onPanePlaybackSnapshot(rightSlot, position, shouldPlay, scale)
                },
                onOpenTemporarySingle = { position, shouldPlay, scale ->
                    onOpenTemporarySingleMedia(rightSlot, position, shouldPlay, scale)
                },
                onSelectNewMedia = { onPickMediaSlot(rightSlot) },
                onRemove = { onRemoveMediaSlot(rightSlot) },
                videoSegmentsByUri = videoSegmentsByUri,
                videoRotationLockState = videoRotationLockState,
                onLockVideoRotation = onLockVideoRotation,
                onUnlockVideoRotation = onUnlockVideoRotation,
                modifier = Modifier
                    .weight(1f - leftFraction)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun MultiMediaRowAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leftHalfCircle = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
        bottomStart = 24.dp,
    )
    Box(
        modifier = modifier
            .width(34.dp)
            .height(48.dp)
            .clip(leftHalfCircle)
            .background(Color(0x66263247))
            .border(1.dp, Color(0x66FFFFFF), leftHalfCircle)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "+",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MultiMediaPaneOrEmpty(
    pane: MultiMediaPaneSession?,
    slot: MultiMediaSlot,
    onPickMediaSlot: (MultiMediaSlot) -> Unit,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    onSearchAuthor: (String) -> Unit,
    onPlaybackStateChange: (Long, Boolean, Float) -> Unit,
    onPlaybackSnapshot: (Long, Boolean, Float) -> Unit,
    onOpenTemporarySingle: (Long, Boolean, Float) -> Unit,
    onSelectNewMedia: () -> Unit,
    onRemove: () -> Unit,
    videoSegmentsByUri: Map<String, List<VideoSegment>>,
    videoRotationLockState: VideoRotationLockState,
    onLockVideoRotation: (Int) -> Unit,
    onUnlockVideoRotation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    pane?.let { selectedPane ->
        MultiMediaPane(
            item = selectedPane.item,
            sourceItems = selectedPane.sourceItems,
            videoSegments = videoSegmentsByUri[selectedPane.item.uri].orEmpty(),
            initialPlaybackPositionMillis = selectedPane.playbackPositionMillis,
            initialPlayWhenReady = selectedPane.playWhenReady,
            initialScale = selectedPane.scale,
            onSwitchItem = { onPaneItemChange(slot, it) },
            onSearchAuthor = onSearchAuthor,
            onPlaybackStateChange = onPlaybackStateChange,
            onPlaybackSnapshot = onPlaybackSnapshot,
            onOpenTemporarySingle = onOpenTemporarySingle,
            onSelectNewMedia = onSelectNewMedia,
            onRemove = onRemove,
            videoRotationLockState = videoRotationLockState,
            onLockVideoRotation = onLockVideoRotation,
            onUnlockVideoRotation = onUnlockVideoRotation,
            modifier = modifier,
        )
    } ?: MultiMediaEmptyPane(
        onClick = { onPickMediaSlot(slot) },
        modifier = modifier,
    )
}

private enum class MultiMediaSlot {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

private data class MultiMediaSession(
    val topLeft: MultiMediaPaneSession? = null,
    val topRight: MultiMediaPaneSession? = null,
    val bottomLeft: MultiMediaPaneSession? = null,
    val bottomRight: MultiMediaPaneSession? = null,
    val topRightExpanded: Boolean = false,
    val bottomRightExpanded: Boolean = false,
    val pendingSlot: MultiMediaSlot? = null,
) {
    fun pane(slot: MultiMediaSlot): MultiMediaPaneSession? = when (slot) {
        MultiMediaSlot.TopLeft -> topLeft
        MultiMediaSlot.TopRight -> topRight
        MultiMediaSlot.BottomLeft -> bottomLeft
        MultiMediaSlot.BottomRight -> bottomRight
    }

    fun withPane(slot: MultiMediaSlot, pane: MultiMediaPaneSession): MultiMediaSession = when (slot) {
        MultiMediaSlot.TopLeft -> copy(topLeft = pane)
        MultiMediaSlot.TopRight -> copy(topRight = pane, topRightExpanded = true)
        MultiMediaSlot.BottomLeft -> copy(bottomLeft = pane)
        MultiMediaSlot.BottomRight -> copy(bottomRight = pane, bottomRightExpanded = true)
    }

    fun expandRightSlot(slot: MultiMediaSlot): MultiMediaSession = when (slot) {
        MultiMediaSlot.TopRight -> copy(topRightExpanded = true)
        MultiMediaSlot.BottomRight -> copy(bottomRightExpanded = true)
        else -> this
    }

    fun withPanePlaybackState(
        slot: MultiMediaSlot,
        positionMillis: Long,
        playWhenReady: Boolean,
        scale: Float? = null,
    ): MultiMediaSession = when (slot) {
        MultiMediaSlot.TopLeft -> topLeft?.let { pane ->
            copy(topLeft = pane.copy(
                playbackPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale ?: pane.scale,
            ))
        }
        MultiMediaSlot.TopRight -> topRight?.let { pane ->
            copy(topRight = pane.copy(
                playbackPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale ?: pane.scale,
            ))
        }
        MultiMediaSlot.BottomLeft -> bottomLeft?.let { pane ->
            copy(bottomLeft = pane.copy(
                playbackPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale ?: pane.scale,
            ))
        }
        MultiMediaSlot.BottomRight -> bottomRight?.let { pane ->
            copy(bottomRight = pane.copy(
                playbackPositionMillis = positionMillis,
                playWhenReady = playWhenReady,
                scale = scale ?: pane.scale,
            ))
        }
    } ?: this

    fun withPaneScale(slot: MultiMediaSlot, scale: Float): MultiMediaSession = when (slot) {
        MultiMediaSlot.TopLeft -> topLeft?.let { pane -> copy(topLeft = pane.copy(scale = scale)) }
        MultiMediaSlot.TopRight -> topRight?.let { pane -> copy(topRight = pane.copy(scale = scale)) }
        MultiMediaSlot.BottomLeft -> bottomLeft?.let { pane -> copy(bottomLeft = pane.copy(scale = scale)) }
        MultiMediaSlot.BottomRight -> bottomRight?.let { pane -> copy(bottomRight = pane.copy(scale = scale)) }
    } ?: this

    fun withPanePlaybackSnapshots(
        snapshots: Map<MultiMediaSlot, MultiMediaPlaybackSnapshot>,
    ): MultiMediaSession = snapshots.entries.fold(this) { session, (slot, snapshot) ->
        session.withPanePlaybackState(
            slot,
            snapshot.positionMillis,
            snapshot.playWhenReady,
            snapshot.scale,
        )
    }

    fun removePane(slot: MultiMediaSlot): MultiMediaSession = when (slot) {
        MultiMediaSlot.TopLeft -> topRight?.let { pane ->
            copy(topLeft = pane, topRight = null, topRightExpanded = false)
        } ?: copy(topLeft = null, topRightExpanded = false)
        MultiMediaSlot.TopRight -> copy(topRight = null, topRightExpanded = false)
        MultiMediaSlot.BottomLeft -> bottomRight?.let { pane ->
            copy(bottomLeft = pane, bottomRight = null, bottomRightExpanded = false)
        } ?: copy(bottomLeft = null, bottomRightExpanded = false)
        MultiMediaSlot.BottomRight -> copy(bottomRight = null, bottomRightExpanded = false)
    }

    fun firstPane(): MultiMediaPaneSession? =
        listOfNotNull(topLeft, topRight, bottomLeft, bottomRight).firstOrNull()

    fun isEmpty(): Boolean = firstPane() == null
}

private data class MultiMediaPaneSession(
    val item: MediaItem,
    val sourceItems: List<MediaItem>,
    val playbackPositionMillis: Long = 0L,
    val playWhenReady: Boolean = true,
    val scale: Float = 1f,
)

private data class MultiMediaPlaybackSnapshot(
    val positionMillis: Long,
    val playWhenReady: Boolean,
    val scale: Float,
)

@Composable
private fun FloatingMultiMediaWindow(
    session: MultiMediaSession,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    videoRotationLockState: VideoRotationLockState,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHorizontalOffset = -(
            with(density) { maxWidth.toPx() } - with(density) { 220.dp.toPx() + 14.dp.toPx() }
        ).coerceAtLeast(0f)
        val maxVerticalOffset = -(
            with(density) { maxHeight.toPx() } - with(density) { 280.dp.toPx() + 14.dp.toPx() }
        ).coerceAtLeast(0f)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                .width(220.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xF20A0E14))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)),
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    if (session.pendingSlot != null) "请从列表选择媒体" else "多视频",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(start = 10.dp, end = 30.dp, top = 9.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FloatingMultiMediaRow(
                    leftPane = session.topLeft,
                    rightPane = session.topRight,
                    isRightExpanded = session.topRightExpanded,
                    leftSlot = MultiMediaSlot.TopLeft,
                    rightSlot = MultiMediaSlot.TopRight,
                    onPaneItemChange = onPaneItemChange,
                    videoRotationLockState = videoRotationLockState,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x99FFFFFF)))
                FloatingMultiMediaRow(
                    leftPane = session.bottomLeft,
                    rightPane = session.bottomRight,
                    isRightExpanded = session.bottomRightExpanded,
                    leftSlot = MultiMediaSlot.BottomLeft,
                    rightSlot = MultiMediaSlot.BottomRight,
                    onPaneItemChange = onPaneItemChange,
                    videoRotationLockState = videoRotationLockState,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxHorizontalOffset, maxVerticalOffset) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var wasDragged = false
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        val delta = change.positionChange()
                                        if (delta.x != 0f || delta.y != 0f) {
                                            wasDragged = true
                                            dragOffsetX = (dragOffsetX + delta.x)
                                                .coerceIn(maxHorizontalOffset, 0f)
                                            dragOffsetY = (dragOffsetY + delta.y)
                                                .coerceIn(maxVerticalOffset, 0f)
                                            change.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            if (!wasDragged) onOpen()
                        }
                    },
            )
            Text(
                "×",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xAA000000))
                    .clickable { onClose() }
                    .padding(bottom = 2.dp),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun FloatingMultiMediaRow(
    leftPane: MultiMediaPaneSession?,
    rightPane: MultiMediaPaneSession?,
    isRightExpanded: Boolean,
    leftSlot: MultiMediaSlot,
    rightSlot: MultiMediaSlot,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    videoRotationLockState: VideoRotationLockState,
    modifier: Modifier = Modifier,
) {
    if (!isRightExpanded) {
        FloatingMultiMediaPreview(
            pane = leftPane,
            slot = leftSlot,
            onPaneItemChange = onPaneItemChange,
            videoRotationLockState = videoRotationLockState,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    Row(modifier.fillMaxWidth()) {
        FloatingMultiMediaPreview(
            pane = leftPane,
            slot = leftSlot,
            onPaneItemChange = onPaneItemChange,
            videoRotationLockState = videoRotationLockState,
            modifier = Modifier.weight(0.68f),
        )
        Box(Modifier.width(3.dp).fillMaxHeight().background(Color(0x99FFFFFF)))
        FloatingMultiMediaPreview(
            pane = rightPane,
            slot = rightSlot,
            onPaneItemChange = onPaneItemChange,
            videoRotationLockState = videoRotationLockState,
            modifier = Modifier.weight(0.32f),
        )
    }
}

@Composable
private fun FloatingMultiMediaPreview(
    pane: MultiMediaPaneSession?,
    slot: MultiMediaSlot,
    onPaneItemChange: (MultiMediaSlot, MediaItem) -> Unit,
    videoRotationLockState: VideoRotationLockState,
    modifier: Modifier = Modifier,
) {
    pane?.let { selectedPane ->
        MultiMediaPane(
            item = selectedPane.item,
            sourceItems = selectedPane.sourceItems,
            onSwitchItem = { onPaneItemChange(slot, it) },
            showControls = false,
            videoRotationLockState = videoRotationLockState,
            modifier = modifier,
        )
    } ?: Box(
        modifier = modifier.background(Color(0xFF10151E)),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", color = Color(0xCCFFFFFF), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun MultiMediaEmptyPane(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F15))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "+",
                color = Color(0xCCFFFFFF),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                "选择媒体",
                color = Color(0xCCFFFFFF),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MultiMediaPane(
    item: MediaItem,
    sourceItems: List<MediaItem>,
    videoSegments: List<VideoSegment> = emptyList(),
    initialPlaybackPositionMillis: Long = 0L,
    initialPlayWhenReady: Boolean = true,
    initialScale: Float = 1f,
    onSwitchItem: (MediaItem) -> Unit,
    onSearchAuthor: ((String) -> Unit)? = null,
    onPlaybackStateChange: ((Long, Boolean, Float) -> Unit)? = null,
    onPlaybackSnapshot: ((Long, Boolean, Float) -> Unit)? = null,
    onOpenTemporarySingle: ((Long, Boolean, Float) -> Unit)? = null,
    onSelectNewMedia: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    videoRotationLockState: VideoRotationLockState = VideoRotationLockState(),
    onLockVideoRotation: (Int) -> Unit = {},
    onUnlockVideoRotation: () -> Unit = {},
    showControls: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val toneCurve = LocalToneCurve.current
    val colorAdjustments = LocalColorAdjustments.current
    val isToneCurveEditing = LocalToneCurveEditing.current
    val fallbackDurationMillis = item.durationMillis?.takeIf { it > 0L } ?: 0L
    var scale by remember(item.uri) { mutableFloatStateOf(initialScale) }
    var offsetX by remember(item.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.uri) { mutableFloatStateOf(0f) }
    var videoAspectRatio by remember(item.uri) { mutableStateOf(item.displayAspectRatio) }
    var horizontalSeekBasePosition by remember(item.uri) { mutableStateOf(0L) }
    var resumeAfterHorizontalSeek by remember(item.uri) { mutableStateOf(false) }
    var controlsVisible by remember(item.uri) { mutableStateOf(false) }
    var currentPosition by remember(item.uri) { mutableStateOf(0L) }
    var duration by remember(item.uri) { mutableStateOf(fallbackDurationMillis) }
    var playbackSpeed by remember(item.uri) { mutableFloatStateOf(1f) }
    var isScrubbing by remember(item.uri) { mutableStateOf(false) }
    var resumeAfterScrubbing by remember(item.uri) { mutableStateOf(false) }
    var activeSegment by remember(item.uri) { mutableStateOf<VideoSegment?>(null) }
    var isMuted by remember(item.uri) { mutableStateOf(false) }
    var videoRotationDegrees by remember(item.uri) {
        mutableStateOf(if (videoRotationLockState.isLocked) videoRotationLockState.defaultDegrees else 0)
    }
    var hasVideoRotationStarted by remember(item.uri) {
        mutableStateOf(videoRotationDegrees != 0)
    }
    var controlsAutoHideToken by remember(item.uri) { mutableStateOf(0) }
    var suppressNextControlsTap by remember(item.uri) { mutableStateOf(false) }
    var controlsTapGuardToken by remember(item.uri) { mutableStateOf(0) }
    val authorId = remember(item.displayName) { item.displayName.substringBefore('_').trim() }
    val videoEffectsKey = remember(toneCurve, colorAdjustments) {
        VideoEffectsKey(toneCurve, colorAdjustments)
    }
    val initialVideoEffects = remember(videoEffectsKey) {
        buildVideoEffects(toneCurve, colorAdjustments)
    }
    val player = if (item.mediaType == MediaType.Video) {
        remember(item.uri) {
            ExoPlayer.Builder(context).build().apply {
                if (initialVideoEffects.isNotEmpty()) {
                    setVideoEffects(initialVideoEffects)
                }
                setMediaItem(fromUri(Uri.parse(item.uri)))
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                seekTo(initialPlaybackPositionMillis.coerceAtLeast(0L))
                playWhenReady = initialPlayWhenReady
            }
        }
    } else {
        null
    }
    var appliedVideoEffectsKey by remember(player) { mutableStateOf<VideoEffectsKey?>(videoEffectsKey) }
    var resumePlaybackAfterAdjustment by remember(player) { mutableStateOf(false) }
    PausePlayerWhenAppStops(player)

    if (player != null) {
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    videoAspectRatio = videoSize.displayAspectRatio() ?: item.displayAspectRatio
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    duration = player.effectiveDurationMillis(fallbackDurationMillis)
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                }
            }
            player.addListener(listener)
            onDispose {
                onPlaybackStateChange?.invoke(
                    player.currentPosition.coerceAtLeast(0L),
                    player.playWhenReady,
                    scale,
                )
                player.removeListener(listener)
                player.release()
            }
        }
    }

    LaunchedEffect(player) {
        player ?: return@LaunchedEffect
        while (true) {
            duration = player.effectiveDurationMillis(fallbackDurationMillis)
            if (!isScrubbing) {
                currentPosition = player.currentPosition.coerceAtLeast(0L)
            }
            onPlaybackSnapshot?.invoke(
                player.currentPosition.coerceAtLeast(0L),
                player.playWhenReady,
                scale,
            )
            if (showControls && !isScrubbing) {
                activeSegment?.let { segment ->
                    if (player.currentPosition < segment.startMillis ||
                        player.currentPosition >= segment.endMillis
                    ) {
                        player.seekTo(segment.startMillis)
                        currentPosition = segment.startMillis
                    }
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(activeSegment, player, showControls) {
        if (!showControls) return@LaunchedEffect
        activeSegment?.let { segment ->
            player?.seekTo(segment.startMillis)
            currentPosition = segment.startMillis
            player?.play()
        }
    }

    LaunchedEffect(videoSegments) {
        if (activeSegment != null && activeSegment !in videoSegments) {
            activeSegment = null
        }
    }

    LaunchedEffect(player, playbackSpeed) {
        player?.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(player, isToneCurveEditing) {
        player?.let { activePlayer ->
            if (isToneCurveEditing) {
                resumePlaybackAfterAdjustment = activePlayer.playWhenReady
                activePlayer.pause()
            } else if (resumePlaybackAfterAdjustment) {
                activePlayer.play()
            }
            if (!isToneCurveEditing) {
                resumePlaybackAfterAdjustment = false
            }
        }
    }

    LaunchedEffect(player, toneCurve, colorAdjustments, isToneCurveEditing) {
        player?.let { activePlayer ->
            val nextKey = videoEffectsKey
            if (appliedVideoEffectsKey == nextKey) return@LaunchedEffect
            if (appliedVideoEffectsKey == null && nextKey.isIdentity) {
                appliedVideoEffectsKey = nextKey
                return@LaunchedEffect
            }
            if (isToneCurveEditing) {
                delay(VIDEO_EFFECT_EDITING_DEBOUNCE_MILLIS)
            }
            val nextEffects = buildVideoEffectsInBackground(nextKey)
            val resumePlayback = !isToneCurveEditing && (
                activePlayer.playWhenReady || resumePlaybackAfterAdjustment
            )
            if (resumePlayback) {
                resumePlaybackAfterAdjustment = true
            }
            val positionMillis = activePlayer.currentPosition.coerceAtLeast(0L)
            activePlayer.applyVideoEffectsWithPipelineReset(
                mediaUri = item.uri,
                positionMillis = positionMillis,
                effects = nextEffects,
                resumePlayback = resumePlayback,
            )
            appliedVideoEffectsKey = nextKey
            if (resumePlayback) {
                resumePlaybackAfterAdjustment = false
            }
        }
    }

    LaunchedEffect(player, isMuted) {
        player?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(item.uri, videoRotationLockState.defaultDegrees, videoRotationLockState.isLocked) {
        if (videoRotationLockState.isLocked && item.mediaType.supportsManualRotation) {
            videoRotationDegrees = videoRotationLockState.defaultDegrees
            hasVideoRotationStarted = videoRotationDegrees != 0
        }
    }

    LaunchedEffect(controlsAutoHideToken) {
        if (controlsAutoHideToken == 0) return@LaunchedEffect
        delay(CONTROLS_AFTER_SCRUB_VISIBLE_MILLIS)
        controlsVisible = false
    }

    LaunchedEffect(controlsTapGuardToken) {
        if (controlsTapGuardToken == 0) return@LaunchedEffect
        suppressNextControlsTap = true
        delay(CONTROLS_SCRUB_TAP_GUARD_MILLIS)
        suppressNextControlsTap = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black)
            .pointerInput(item.uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            val nextScale = (scale * event.calculateZoom()).coerceIn(0.5f, 3f)
                            val pan = event.calculatePan()
                            scale = nextScale
                            if (nextScale != 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(item.uri, player) {
                detectVideoGestures(
                    onTap = {
                        if (player != null) {
                            if (suppressNextControlsTap) {
                                suppressNextControlsTap = false
                                controlsVisible = true
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        }
                    },
                    onDoubleTap = {
                        player?.let { activePlayer ->
                            if (activePlayer.playWhenReady) activePlayer.pause() else activePlayer.play()
                        }
                    },
                    onHorizontalSwipeStart = {
                        player?.let { activePlayer ->
                            horizontalSeekBasePosition = activePlayer.currentPosition.coerceAtLeast(0L)
                            resumeAfterHorizontalSeek = activePlayer.playWhenReady
                            activePlayer.pause()
                        }
                    },
                    onHorizontalSwipe = { seekOffset ->
                        player?.let { activePlayer ->
                            val duration = activePlayer.duration
                                .takeIf { it > 0L && it != C.TIME_UNSET }
                                ?: return@let
                            activePlayer.seekTo(
                                (horizontalSeekBasePosition + seekOffset).coerceIn(0L, duration),
                            )
                        }
                    },
                    onHorizontalSwipeEnd = {
                        player?.let { activePlayer ->
                            if (resumeAfterHorizontalSeek) activePlayer.play()
                            resumeAfterHorizontalSeek = false
                        }
                    },
                    onVerticalSwipe = { intent ->
                        val currentIndex = sourceItems.indexOfFirst { it.uri == item.uri }
                        val targetIndex = when (intent) {
                            SwipeIntent.Next -> currentIndex + 1
                            SwipeIntent.Previous -> currentIndex - 1
                        }
                        sourceItems.getOrNull(targetIndex)?.let(onSwitchItem)
                    },
                    durationMillis = {
                        player?.effectiveDurationMillis(fallbackDurationMillis) ?: fallbackDurationMillis
                    },
                )
            },
    ) {
        StableAspectMediaFrame(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            aspectRatioOverride = videoAspectRatio,
            rotationDegrees = videoRotationDegrees,
        ) {
            if (player != null) {
                AndroidView(
                    factory = { viewContext ->
                            (LayoutInflater.from(viewContext)
                                .inflate(R.layout.view_video_player, null, false) as PlayerView).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    update = { view ->
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        view.player = player
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = mediaImageModel(item),
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = toneCurve.toImageColorFilter(),
                )
            }
        }
        if (showControls) {
            MultiPaneVideoSegmentList(
                segments = videoSegments,
                activeSegment = activeSegment,
                onToggleSegment = { segment ->
                    activeSegment = if (activeSegment == segment) null else segment
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 12.dp),
            )
        }
        if (showControls && (
                onOpenTemporarySingle != null || onSearchAuthor != null || player != null ||
                    onSelectNewMedia != null || onRemove != null
            )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (onOpenTemporarySingle != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x504AA8FF))
                            .border(1.dp, Color(0x887CC4FF), RoundedCornerShape(50))
                            .clickable {
                                onOpenTemporarySingle.invoke(
                                    player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                                    player?.playWhenReady ?: false,
                                    scale,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "临时单视频播放",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFD6EAFF),
                        )
                    }
                }
                if (onSearchAuthor != null && authorId.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x504D46D8))
                            .border(1.dp, Color(0x888F89FF), RoundedCornerShape(50))
                            .clickable { onSearchAuthor(authorId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索作者 $authorId",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFE1DEFF),
                        )
                    }
                }
                if (item.mediaType.supportsManualRotation) {
                    ViewerRotateButton(
                        onClick = {
                            if (videoRotationLockState.isLocked) {
                                onUnlockVideoRotation()
                            }
                            videoRotationDegrees = nextVideoRotationDegrees(
                                currentDegrees = videoRotationDegrees,
                                hasStarted = hasVideoRotationStarted,
                            )
                            hasVideoRotationStarted = true
                        },
                        isLockActive = videoRotationLockState.isLocked,
                        onLockClick = {
                            if (videoRotationLockState.isLocked) {
                                onUnlockVideoRotation()
                            } else {
                                onLockVideoRotation(videoRotationDegrees)
                            }
                        },
                        size = 24.dp,
                        iconSize = 14.dp,
                        lockSize = 13.dp,
                    )
                }
                if (player != null) {
                    val muteBackground = if (isMuted) Color(0x66432127) else Color(0x5044A870)
                    val muteBorder = if (isMuted) Color(0x88FF9C9C) else Color(0x8890E0AC)
                    val muteTint = if (isMuted) Color(0xFFFFD0D0) else Color(0xFFD6FFE3)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(muteBackground)
                            .border(1.dp, muteBorder, RoundedCornerShape(50))
                            .clickable {
                                isMuted = !isMuted
                                player.volume = if (isMuted) 0f else 1f
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (isMuted) "取消静音" else "静音",
                            modifier = Modifier.size(14.dp),
                            tint = muteTint,
                        )
                    }
                }
                if (onSelectNewMedia != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x504AA8FF))
                            .border(1.dp, Color(0x887CC4FF), RoundedCornerShape(50))
                            .clickable { onSelectNewMedia() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "选择新媒体",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFD4E9FF),
                        )
                    }
                }
                if (onRemove != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x66F56B6B))
                            .border(1.dp, Color(0x88FFB0B0), RoundedCornerShape(50))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(10.dp)) {
                            val strokeWidth = 1.6.dp.toPx()
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = player != null && controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                slideInVertically(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) { it / 2 },
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                slideOutVertically(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) { it / 2 },
        ) {
            MultiPaneProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                onSeek = { position ->
                    player?.seekTo(position)
                    currentPosition = position
                },
                onScrubStart = {
                    player?.let { activePlayer ->
                        resumeAfterScrubbing = activePlayer.playWhenReady
                        isScrubbing = true
                        controlsVisible = true
                        activePlayer.pause()
                    }
                },
                onScrubEnd = {
                    player?.let { activePlayer ->
                        isScrubbing = false
                        if (resumeAfterScrubbing) activePlayer.play() else activePlayer.pause()
                    }
                    resumeAfterScrubbing = false
                    controlsVisible = true
                    controlsAutoHideToken += 1
                    controlsTapGuardToken += 1
                },
                onPlaybackSpeedChange = { playbackSpeed = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MultiPaneVideoSegmentList(
    segments: List<VideoSegment>,
    activeSegment: VideoSegment?,
    onToggleSegment: (VideoSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (segments.isEmpty()) return
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        segments.sortedBy { it.startMillis }.forEach { segment ->
            val isActive = activeSegment == segment
            Text(
                text = segment.name?.takeIf { it.isNotBlank() }
                    ?: "${segment.startMillis.formatDuration()}-${segment.endMillis.formatDuration()}",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) Color(0xFFE9C55D) else Color(0x99000000))
                    .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { onToggleSegment(segment) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                color = if (isActive) Color(0xFF3F2B00) else Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MultiPaneProgressBar(
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val safePosition = currentPosition.coerceIn(0L, safeDuration)
    val controlInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xB32A2A2A))
            .clickable(
                interactionSource = controlInteractionSource,
                indication = null,
                onClick = {},
            )
            .padding(start = 10.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
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
            onScrubStart = onScrubStart,
            onScrub = onSeek,
            onScrubEnd = onScrubEnd,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            PlaybackTimeFormatter.formatMillis(duration),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
        )
        PlaybackSpeedOptions.values.forEach { speed ->
            val selected = playbackSpeed == speed
            val speedBackground by animateColorAsState(
                targetValue = if (selected) Color(0x55FFFFFF) else Color.Transparent,
                animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
                label = "multi_pane_playback_speed_selection",
            )
            Text(
                "${speed}x",
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(speedBackground)
                    .clickable { onPlaybackSpeedChange(speed) }
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private const val VideoGestureDoubleTapTimeoutMillis = 280L
private const val MIN_VIEWER_SCALE = 0.45f
private const val MAX_VIEWER_SCALE = 5f
private const val DEFAULT_SINGLE_MEDIA_DISPLAY_SCALE = 0.86f
private const val CONTROLS_AFTER_SCRUB_VISIBLE_MILLIS = 10_000L
private const val CONTROLS_SCRUB_TAP_GUARD_MILLIS = 500L
private const val VIDEO_EFFECT_EDITING_DEBOUNCE_MILLIS = 120L
private const val VIDEO_EFFECT_RESUME_DELAY_MILLIS = 1_000L

private data class ToneCurve(
    val blacks: Float = 0f,
    val shadows: Float = 0.25f,
    val midtones: Float = 0.5f,
    val highlights: Float = 0.75f,
    val whites: Float = 1f,
) {
    val pointValues: FloatArray
        get() = floatArrayOf(blacks, shadows, midtones, highlights, whites)

    val isIdentity: Boolean
        get() = kotlin.math.abs(blacks) < 0.001f &&
            kotlin.math.abs(shadows - 0.25f) < 0.001f &&
            kotlin.math.abs(midtones - 0.5f) < 0.001f &&
            kotlin.math.abs(highlights - 0.75f) < 0.001f &&
            kotlin.math.abs(whites - 1f) < 0.001f

    fun withPoint(index: Int, value: Float): ToneCurve = when (index) {
        0 -> copy(blacks = value.coerceIn(0f, 1f))
        1 -> copy(shadows = value.coerceIn(0f, 1f))
        2 -> copy(midtones = value.coerceIn(0f, 1f))
        3 -> copy(highlights = value.coerceIn(0f, 1f))
        else -> copy(whites = value.coerceIn(0f, 1f))
    }

    fun isCloseTo(other: ToneCurve): Boolean {
        return kotlin.math.abs(blacks - other.blacks) < 0.001f &&
            kotlin.math.abs(shadows - other.shadows) < 0.001f &&
            kotlin.math.abs(midtones - other.midtones) < 0.001f &&
            kotlin.math.abs(highlights - other.highlights) < 0.001f &&
            kotlin.math.abs(whites - other.whites) < 0.001f
    }

    fun map(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        val values = pointValues
        val segment = (0..3).firstOrNull { index -> x <= POINT_X[index + 1] } ?: 3
        val progress = (x - POINT_X[segment]) / (POINT_X[segment + 1] - POINT_X[segment])
        return (values[segment] + (values[segment + 1] - values[segment]) * progress)
            .coerceIn(0f, 1f)
    }

    @OptIn(UnstableApi::class)
    fun toVideoEffect(): Effect {
        val cube = Array(VIDEO_LUT_SIZE) { red ->
            Array(VIDEO_LUT_SIZE) { green ->
                IntArray(VIDEO_LUT_SIZE) { blue ->
                    val r = mapLutNode(red)
                    val g = mapLutNode(green)
                    val b = mapLutNode(blue)
                    android.graphics.Color.rgb(r, g, b)
                }
            }
        }
        return SingleColorLut.createFromCube(cube)
    }

    fun toImageColorFilter(): ColorFilter? {
        if (isIdentity) return null
        val slope = ((map(0.75f) - map(0.25f)) / 0.5f).coerceIn(0f, 2.5f)
        val offset = ((map(0.5f) - 0.5f * slope) * 255f).coerceIn(-255f, 255f)
        return ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    slope, 0f, 0f, 0f, offset,
                    0f, slope, 0f, 0f, offset,
                    0f, 0f, slope, 0f, offset,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }

    private fun mapLutNode(index: Int): Int {
        return (map(index / (VIDEO_LUT_SIZE - 1f)) * 255f).roundToInt().coerceIn(0, 255)
    }

    companion object {
        val POINT_X = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        private const val VIDEO_LUT_SIZE = 32
    }
}

private data class ToneCurvePreset(
    val id: String,
    val name: String,
    val curve: ToneCurve,
)

private class ToneCurvePresetStore(context: Context) {
    private val preferences = context.getSharedPreferences("videosee_tone_curve_presets", Context.MODE_PRIVATE)

    fun load(): List<ToneCurvePreset> = runCatching {
        val array = JSONArray(preferences.getString(PRESETS_KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue
                add(
                    ToneCurvePreset(
                        id = id,
                        name = name,
                        curve = ToneCurve(
                            blacks = item.optDouble("blacks", 0.0).toFloat().coerceIn(0f, 1f),
                            shadows = item.optDouble("shadows", 0.25).toFloat().coerceIn(0f, 1f),
                            midtones = item.optDouble("midtones", 0.5).toFloat().coerceIn(0f, 1f),
                            highlights = item.optDouble("highlights", 0.75).toFloat().coerceIn(0f, 1f),
                            whites = item.optDouble("whites", 1.0).toFloat().coerceIn(0f, 1f),
                        ),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(presets: List<ToneCurvePreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("blacks", preset.curve.blacks)
                    put("shadows", preset.curve.shadows)
                    put("midtones", preset.curve.midtones)
                    put("highlights", preset.curve.highlights)
                    put("whites", preset.curve.whites)
                },
            )
        }
        preferences.edit().putString(PRESETS_KEY, array.toString()).apply()
    }

    fun observe(onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PRESETS_KEY) onChange()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val PRESETS_KEY = "presets"
    }
}

private class PlaybackAdjustmentStore(context: Context) {
    private val preferences = context.getSharedPreferences("videosee_playback_adjustments", Context.MODE_PRIVATE)

    fun loadToneCurve(): ToneCurve = runCatching {
        ToneCurve(
            blacks = preferences.getFloat(TONE_BLACKS_KEY, 0f).coerceIn(0f, 1f),
            shadows = preferences.getFloat(TONE_SHADOWS_KEY, 0.25f).coerceIn(0f, 1f),
            midtones = preferences.getFloat(TONE_MIDTONES_KEY, 0.5f).coerceIn(0f, 1f),
            highlights = preferences.getFloat(TONE_HIGHLIGHTS_KEY, 0.75f).coerceIn(0f, 1f),
            whites = preferences.getFloat(TONE_WHITES_KEY, 1f).coerceIn(0f, 1f),
        )
    }.getOrDefault(ToneCurve())

    fun saveToneCurve(toneCurve: ToneCurve) {
        preferences.edit()
            .putFloat(TONE_BLACKS_KEY, toneCurve.blacks)
            .putFloat(TONE_SHADOWS_KEY, toneCurve.shadows)
            .putFloat(TONE_MIDTONES_KEY, toneCurve.midtones)
            .putFloat(TONE_HIGHLIGHTS_KEY, toneCurve.highlights)
            .putFloat(TONE_WHITES_KEY, toneCurve.whites)
            .apply()
    }

    fun loadColorAdjustments(): ColorAdjustments = runCatching {
        ColorAdjustments(
            sharpness = preferences.getFloat(COLOR_SHARPNESS_KEY, 0f).coerceIn(0f, 1f),
            contrast = preferences.getFloat(COLOR_CONTRAST_KEY, 0f).coerceIn(-1f, 1f),
            vibrance = preferences.getFloat(COLOR_VIBRANCE_KEY, 0f).coerceIn(-1f, 1f),
            redAccent = preferences.getFloat(COLOR_RED_ACCENT_KEY, 0f).coerceIn(-1f, 1f),
            blueAccent = preferences.getFloat(COLOR_BLUE_ACCENT_KEY, 0f).coerceIn(-1f, 1f),
            purpleAccent = preferences.getFloat(COLOR_PURPLE_ACCENT_KEY, 0f).coerceIn(-1f, 1f),
        )
    }.getOrDefault(ColorAdjustments())

    fun saveColorAdjustments(colorAdjustments: ColorAdjustments) {
        preferences.edit()
            .putFloat(COLOR_SHARPNESS_KEY, colorAdjustments.sharpness)
            .putFloat(COLOR_CONTRAST_KEY, colorAdjustments.contrast)
            .putFloat(COLOR_VIBRANCE_KEY, colorAdjustments.vibrance)
            .putFloat(COLOR_RED_ACCENT_KEY, colorAdjustments.redAccent)
            .putFloat(COLOR_BLUE_ACCENT_KEY, colorAdjustments.blueAccent)
            .putFloat(COLOR_PURPLE_ACCENT_KEY, colorAdjustments.purpleAccent)
            .apply()
    }

    private companion object {
        const val TONE_BLACKS_KEY = "tone_blacks"
        const val TONE_SHADOWS_KEY = "tone_shadows"
        const val TONE_MIDTONES_KEY = "tone_midtones"
        const val TONE_HIGHLIGHTS_KEY = "tone_highlights"
        const val TONE_WHITES_KEY = "tone_whites"
        const val COLOR_SHARPNESS_KEY = "color_sharpness"
        const val COLOR_CONTRAST_KEY = "color_contrast"
        const val COLOR_VIBRANCE_KEY = "color_vibrance"
        const val COLOR_RED_ACCENT_KEY = "color_red_accent"
        const val COLOR_BLUE_ACCENT_KEY = "color_blue_accent"
        const val COLOR_PURPLE_ACCENT_KEY = "color_purple_accent"
    }
}

private data class ColorAdjustments(
    val sharpness: Float = 0f,
    val contrast: Float = 0f,
    val vibrance: Float = 0f,
    val redAccent: Float = 0f,
    val blueAccent: Float = 0f,
    val purpleAccent: Float = 0f,
) {
    val isIdentity: Boolean
        get() = kotlin.math.abs(sharpness) < 0.001f &&
            kotlin.math.abs(contrast) < 0.001f &&
            kotlin.math.abs(vibrance) < 0.001f &&
            kotlin.math.abs(redAccent) < 0.001f &&
            kotlin.math.abs(blueAccent) < 0.001f &&
            kotlin.math.abs(purpleAccent) < 0.001f

    val usesColorLut: Boolean
        get() = kotlin.math.abs(contrast) >= 0.001f ||
            kotlin.math.abs(vibrance) >= 0.001f ||
            kotlin.math.abs(redAccent) >= 0.001f ||
            kotlin.math.abs(blueAccent) >= 0.001f ||
            kotlin.math.abs(purpleAccent) >= 0.001f

    fun isCloseTo(other: ColorAdjustments): Boolean {
        return kotlin.math.abs(sharpness - other.sharpness) < 0.001f &&
            kotlin.math.abs(contrast - other.contrast) < 0.001f &&
            kotlin.math.abs(vibrance - other.vibrance) < 0.001f &&
            kotlin.math.abs(redAccent - other.redAccent) < 0.001f &&
            kotlin.math.abs(blueAccent - other.blueAccent) < 0.001f &&
            kotlin.math.abs(purpleAccent - other.purpleAccent) < 0.001f
    }

    fun toVideoEffects(): List<Effect> = buildList {
        if (sharpness > 0.001f) {
            add(SharpnessEffect(sharpness.coerceIn(0f, 1f)))
        }
        if (usesColorLut) {
            add(SingleColorLut.createFromCube(createColorAdjustmentCube()))
        }
    }

    private fun createColorAdjustmentCube(): Array<Array<IntArray>> {
        return Array(COLOR_LUT_SIZE) { red ->
            Array(COLOR_LUT_SIZE) { green ->
                IntArray(COLOR_LUT_SIZE) { blue ->
                    mapColor(
                        red / (COLOR_LUT_SIZE - 1f),
                        green / (COLOR_LUT_SIZE - 1f),
                        blue / (COLOR_LUT_SIZE - 1f),
                    )
                }
            }
        }
    }

    private fun mapColor(red: Float, green: Float, blue: Float): Int {
        val contrastScale = 1f + contrast.coerceIn(-1f, 1f) * 0.65f
        val adjustedRed = ((red - 0.5f) * contrastScale + 0.5f).coerceIn(0f, 1f)
        val adjustedGreen = ((green - 0.5f) * contrastScale + 0.5f).coerceIn(0f, 1f)
        val adjustedBlue = ((blue - 0.5f) * contrastScale + 0.5f).coerceIn(0f, 1f)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (adjustedRed * 255f).roundToInt().coerceIn(0, 255),
            (adjustedGreen * 255f).roundToInt().coerceIn(0, 255),
            (adjustedBlue * 255f).roundToInt().coerceIn(0, 255),
            hsv,
        )
        applyVibrance(hsv, vibrance.coerceIn(-1f, 1f))
        applyHueAccent(hsv, targetHue = 0f, widthDegrees = 42f, amount = redAccent.coerceIn(-1f, 1f))
        applyHueAccent(hsv, targetHue = 220f, widthDegrees = 48f, amount = blueAccent.coerceIn(-1f, 1f))
        applyHueAccent(hsv, targetHue = 285f, widthDegrees = 44f, amount = purpleAccent.coerceIn(-1f, 1f))
        return android.graphics.Color.HSVToColor(hsv)
    }

    private companion object {
        const val COLOR_LUT_SIZE = 32
    }
}

private class SharpnessEffect(
    private val amount: Float,
) : SeparableConvolution() {
    override fun getConvolution(presentationTimeUs: Long): ConvolutionFunction1D {
        val strength = amount.coerceIn(0f, 1f) * 0.28f
        return object : ConvolutionFunction1D {
            override fun domainStart(): Float = -1f
            override fun domainEnd(): Float = 1f
            override fun value(samplePosition: Float): Float {
                val distance = kotlin.math.abs(samplePosition)
                return when {
                    distance < 0.5f -> 1f + 2f * strength
                    distance <= 1f -> -strength
                    else -> 0f
                }
            }
        }
    }
}

private fun applyVibrance(hsv: FloatArray, amount: Float) {
    if (kotlin.math.abs(amount) < 0.001f) return
    val saturation = hsv[1]
    hsv[1] = if (amount > 0f) {
        (saturation + (1f - saturation) * amount * (1f - saturation) * 0.9f).coerceIn(0f, 1f)
    } else {
        (saturation * (1f + amount * 0.75f)).coerceIn(0f, 1f)
    }
}

private fun applyHueAccent(
    hsv: FloatArray,
    targetHue: Float,
    widthDegrees: Float,
    amount: Float,
) {
    if (kotlin.math.abs(amount) < 0.001f || hsv[1] < 0.04f) return
    val proximity = (1f - circularHueDistance(hsv[0], targetHue) / widthDegrees)
        .coerceIn(0f, 1f) * hsv[1]
    if (proximity <= 0f) return
    if (amount > 0f) {
        hsv[0] = shiftHueToward(hsv[0], targetHue, amount * proximity * 0.2f)
        hsv[1] = (hsv[1] + (1f - hsv[1]) * amount * proximity * 0.95f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * (1f + amount * proximity * 0.08f)).coerceIn(0f, 1f)
    } else {
        val fade = -amount
        hsv[1] = (hsv[1] * (1f - fade * proximity * 0.72f)).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * fade * proximity * 0.12f).coerceIn(0f, 1f)
    }
}

private fun circularHueDistance(hue: Float, targetHue: Float): Float {
    val delta = kotlin.math.abs(hue - targetHue)
    return kotlin.math.min(delta, 360f - delta)
}

private fun shiftHueToward(hue: Float, targetHue: Float, fraction: Float): Float {
    val delta = ((targetHue - hue + 540f) % 360f) - 180f
    return (hue + delta * fraction + 360f) % 360f
}

private data class ColorAdjustmentPreset(
    val id: String,
    val name: String,
    val adjustments: ColorAdjustments,
)

private class ColorAdjustmentPresetStore(context: Context) {
    private val preferences = context.getSharedPreferences("videosee_color_adjustment_presets", Context.MODE_PRIVATE)

    fun load(): List<ColorAdjustmentPreset> = runCatching {
        val array = JSONArray(preferences.getString(PRESETS_KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue
                add(
                    ColorAdjustmentPreset(
                        id = id,
                        name = name,
                        adjustments = ColorAdjustments(
                            sharpness = item.optDouble("sharpness", 0.0).toFloat().coerceIn(0f, 1f),
                            contrast = item.optDouble("contrast", 0.0).toFloat().coerceIn(-1f, 1f),
                            vibrance = item.optDouble("vibrance", 0.0).toFloat().coerceIn(-1f, 1f),
                            redAccent = item.optDouble("redAccent", 0.0).toFloat().coerceIn(-1f, 1f),
                            blueAccent = item.optDouble("blueAccent", 0.0).toFloat().coerceIn(-1f, 1f),
                            purpleAccent = item.optDouble("purpleAccent", 0.0).toFloat().coerceIn(-1f, 1f),
                        ),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(presets: List<ColorAdjustmentPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("sharpness", preset.adjustments.sharpness)
                    put("contrast", preset.adjustments.contrast)
                    put("vibrance", preset.adjustments.vibrance)
                    put("redAccent", preset.adjustments.redAccent)
                    put("blueAccent", preset.adjustments.blueAccent)
                    put("purpleAccent", preset.adjustments.purpleAccent)
                },
            )
        }
        preferences.edit().putString(PRESETS_KEY, array.toString()).apply()
    }

    fun observe(onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PRESETS_KEY) onChange()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val PRESETS_KEY = "presets"
    }
}

private val LocalToneCurve = staticCompositionLocalOf { ToneCurve() }
private val LocalColorAdjustments = staticCompositionLocalOf { ColorAdjustments() }
private val LocalToneCurveEditing = staticCompositionLocalOf { false }

private data class VideoEffectsKey(
    val toneCurve: ToneCurve,
    val colorAdjustments: ColorAdjustments,
) {
    val isIdentity: Boolean
        get() = toneCurve.isIdentity && colorAdjustments.isIdentity
}

@Composable
private fun PausePlayerWhenAppStops(
    player: Player?,
    onPaused: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPaused by rememberUpdatedState(onPaused)

    DisposableEffect(player, lifecycleOwner) {
        if (player == null) {
            return@DisposableEffect onDispose {}
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
                latestOnPaused()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private suspend fun buildVideoEffectsInBackground(key: VideoEffectsKey): List<Effect> {
    return withContext(Dispatchers.Default) {
        buildVideoEffects(key.toneCurve, key.colorAdjustments)
    }
}

private fun buildVideoEffects(toneCurve: ToneCurve, colorAdjustments: ColorAdjustments): List<Effect> {
    return buildList {
        if (!toneCurve.isIdentity) {
            add(toneCurve.toVideoEffect())
        }
        addAll(colorAdjustments.toVideoEffects())
    }
}

private suspend fun ExoPlayer.applyVideoEffectsWithPipelineReset(
    mediaUri: String,
    positionMillis: Long,
    effects: List<Effect>,
    resumePlayback: Boolean,
    onPauseStateChange: ((Boolean) -> Unit)? = null,
) {
    pause()
    playWhenReady = false
    onPauseStateChange?.invoke(true)
    stop()
    clearMediaItems()
    setVideoEffects(effects)
    setMediaItem(fromUri(Uri.parse(mediaUri)))
    prepare()
    seekTo(positionMillis.coerceAtLeast(0L))
    refreshToneCurveVideoFrame()
    if (resumePlayback) {
        delay(VIDEO_EFFECT_RESUME_DELAY_MILLIS)
        play()
        onPauseStateChange?.invoke(false)
    }
}

private fun Player.refreshToneCurveVideoFrame() {
    val position = currentPosition.coerceAtLeast(0L)
    val durationMillis = duration.takeIf { it > 0L && it != C.TIME_UNSET }
    val target = when {
        durationMillis == null -> position + 1L
        position < durationMillis - 1L -> position + 1L
        else -> (position - 1L).coerceAtLeast(0L)
    }
    seekTo(target)
}

private fun Player.effectiveDurationMillis(fallbackDurationMillis: Long = 0L): Long {
    return duration
        .takeIf { it > 0L && it != C.TIME_UNSET }
        ?: fallbackDurationMillis.coerceAtLeast(0L)
}

private data class VideoRotationLockState(
    val defaultDegrees: Int = 0,
    val isLocked: Boolean = false,
)

private fun nextVideoRotationDegrees(currentDegrees: Int, hasStarted: Boolean): Int {
    val increment = if (hasStarted) 90 else 180
    return normalizeRotationDegrees(currentDegrees + increment)
}

private fun normalizeRotationDegrees(degrees: Int): Int {
    return ((degrees % 360) + 360) % 360
}

private val MediaType.supportsManualRotation: Boolean
    get() = this == MediaType.Image || this == MediaType.Video

@Composable
private fun MediaSurface(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    activeVideo: Boolean,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    playbackMode: PlaybackMode,
    onTogglePlaybackMode: () -> Unit,
    videoSegments: List<VideoSegment>,
    onAddVideoSegment: (Long, Long) -> Unit,
    onDeleteVideoSegment: (VideoSegment) -> Unit,
    onRenameVideoSegment: (VideoSegment, String) -> Unit,
    onOpenAuthorSearch: (String) -> Unit,
    isInDefaultFavoriteFolder: Boolean,
    onToggleDefaultFavoriteFolder: () -> Unit,
    initialPlaybackPositionMillis: Long = 0L,
    initialPlayWhenReady: Boolean = true,
    onPlaybackStateChange: ((Long, Boolean) -> Unit)? = null,
    showFileName: Boolean = true,
    videoRotationDegrees: Int = 0,
) {
    when {
        item.mediaType == MediaType.Video && activeVideo -> VideoPlayer(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            onSetMediaFavoriteLevel = onSetMediaFavoriteLevel,
            playbackMode = playbackMode,
            onTogglePlaybackMode = onTogglePlaybackMode,
            videoSegments = videoSegments,
            onAddVideoSegment = onAddVideoSegment,
            onDeleteVideoSegment = onDeleteVideoSegment,
            onRenameVideoSegment = onRenameVideoSegment,
            onOpenAuthorSearch = onOpenAuthorSearch,
            isInDefaultFavoriteFolder = isInDefaultFavoriteFolder,
            onToggleDefaultFavoriteFolder = onToggleDefaultFavoriteFolder,
            initialPlaybackPositionMillis = initialPlaybackPositionMillis,
            initialPlayWhenReady = initialPlayWhenReady,
            onPlaybackStateChange = onPlaybackStateChange,
            showFileName = showFileName,
            rotationDegrees = videoRotationDegrees,
        )

        else -> StableAspectMediaFrame(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationDegrees = videoRotationDegrees,
        ) {
            AsyncImage(
                model = mediaImageModel(item),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = LocalToneCurve.current.toImageColorFilter(),
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
    rotationDegrees: Int = 0,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val aspectRatio = aspectRatioOverride ?: item.displayAspectRatio
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val isQuarterTurn = normalizedRotation == 90 || normalizedRotation == 270
        val displayAspectRatio = if (isQuarterTurn && aspectRatio != null) {
            1f / aspectRatio
        } else {
            aspectRatio
        }
        val containerRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else null
        val aspectModifier = when {
            displayAspectRatio == null || containerRatio == null -> Modifier.fillMaxSize()
            displayAspectRatio > containerRatio -> Modifier.fillMaxWidth().aspectRatio(displayAspectRatio)
            else -> Modifier.fillMaxHeight().aspectRatio(displayAspectRatio)
        }

        BoxWithConstraints(
            modifier = aspectModifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            },
            contentAlignment = Alignment.Center,
        ) {
            val contentModifier = if (isQuarterTurn && aspectRatio != null) {
                Modifier
                    .size(width = maxHeight, height = maxWidth)
                    .graphicsLayer { rotationZ = normalizedRotation.toFloat() }
            } else {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = normalizedRotation.toFloat() }
            }
            Box(modifier = contentModifier) {
                content()
            }
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

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onSetMediaFavoriteLevel: (String, Int) -> Unit,
    playbackMode: PlaybackMode,
    onTogglePlaybackMode: () -> Unit,
    videoSegments: List<VideoSegment>,
    onAddVideoSegment: (Long, Long) -> Unit,
    onDeleteVideoSegment: (VideoSegment) -> Unit,
    onRenameVideoSegment: (VideoSegment, String) -> Unit,
    onOpenAuthorSearch: (String) -> Unit,
    isInDefaultFavoriteFolder: Boolean,
    onToggleDefaultFavoriteFolder: () -> Unit,
    initialPlaybackPositionMillis: Long = 0L,
    initialPlayWhenReady: Boolean = true,
    onPlaybackStateChange: ((Long, Boolean) -> Unit)? = null,
    showFileName: Boolean = true,
    rotationDegrees: Int = 0,
) {
    val context = LocalContext.current
    val toneCurve = LocalToneCurve.current
    val colorAdjustments = LocalColorAdjustments.current
    val isToneCurveEditing = LocalToneCurveEditing.current
    val fallbackDurationMillis = item.durationMillis?.takeIf { it > 0L } ?: 0L
    val currentItem by rememberUpdatedState(item)
    val currentPlaybackStateChange by rememberUpdatedState(onPlaybackStateChange)
    val coroutineScope = rememberCoroutineScope()
    var isPaused by remember(item.uri) { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var currentPosition by remember(item.uri) {
        mutableStateOf(initialPlaybackPositionMillis.coerceAtLeast(0L))
    }
    var duration by remember(item.uri) { mutableStateOf(fallbackDurationMillis) }
    var controlsVisible by remember(item.uri) { mutableStateOf(ViewerUiSpec.CONTROLS_VISIBLE_BY_DEFAULT) }
    var playbackSpeed by remember(item.uri) { mutableFloatStateOf(1f) }
    var seekFeedbackText by remember(item.uri) { mutableStateOf<String?>(null) }
    var seekFeedbackDisplayText by remember(item.uri) { mutableStateOf("") }
    var horizontalSeekOffset by remember(item.uri) { mutableStateOf<Long?>(null) }
    var isSavingSnapshot by remember(item.uri) { mutableStateOf(false) }
    var decodedVideoAspectRatio by remember { mutableStateOf(item.displayAspectRatio) }
    var playerFrameAspectRatio by remember { mutableStateOf(item.displayAspectRatio) }
    var renderedVideoUri by remember { mutableStateOf(item.uri) }
    var segmentStartMillis by remember(item.uri) { mutableStateOf<Long?>(null) }
    var activeSegment by remember(item.uri) { mutableStateOf<VideoSegment?>(null) }
    var isScrubbing by remember(item.uri) { mutableStateOf(false) }
    var resumeAfterScrubbing by remember(item.uri) { mutableStateOf(false) }
    var horizontalSeekBasePosition by remember(item.uri) { mutableStateOf(0L) }
    var resumeAfterHorizontalSeek by remember(item.uri) { mutableStateOf(false) }
    var controlsAutoHideToken by remember(item.uri) { mutableStateOf(0) }
    var suppressNextControlsTap by remember(item.uri) { mutableStateOf(false) }
    var controlsTapGuardToken by remember(item.uri) { mutableStateOf(0) }
    // Keep one decoder and one TextureView for the whole viewer session. Recreating either
    // on every vertical swipe leaves the screen without a frame until the next video decodes.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    var appliedVideoEffectsKey by remember(player) { mutableStateOf<VideoEffectsKey?>(null) }
    var resumePlaybackAfterAdjustment by remember(player) { mutableStateOf(false) }
    PausePlayerWhenAppStops(player) {
        isPaused = true
    }

    LaunchedEffect(item.uri) {
        decodedVideoAspectRatio = item.displayAspectRatio
        playerFrameAspectRatio = item.displayAspectRatio
        renderedVideoUri = ""
    }

    LaunchedEffect(player, item.uri, initialPlaybackPositionMillis, initialPlayWhenReady) {
        player.pause()
        player.setMediaItem(fromUri(Uri.parse(item.uri)))
        player.prepare()
        player.seekTo(initialPlaybackPositionMillis.coerceAtLeast(0L))
        player.playWhenReady = initialPlayWhenReady
        isPaused = !initialPlayWhenReady
    }

    DisposableEffect(player, playerView) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val aspectRatio = videoSize.displayAspectRatio() ?: currentItem.displayAspectRatio
                decodedVideoAspectRatio = aspectRatio
                if (renderedVideoUri == currentItem.uri) {
                    playerFrameAspectRatio = aspectRatio
                }
                player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                playerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                playerView?.requestLayout()
                playerView?.invalidate()
            }

            override fun onRenderedFirstFrame() {
                renderedVideoUri = currentItem.uri
                playerFrameAspectRatio = decodedVideoAspectRatio
            }

            override fun onEvents(player: Player, events: Player.Events) {
                duration = player.effectiveDurationMillis(fallbackDurationMillis)
                currentPosition = player.currentPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, item.uri, fallbackDurationMillis) {
        while (true) {
            duration = player.effectiveDurationMillis(fallbackDurationMillis)
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            currentPlaybackStateChange?.invoke(currentPosition, player.playWhenReady)
            activeSegment?.takeIf { !isScrubbing }?.let { segment ->
                if (player.currentPosition < segment.startMillis || player.currentPosition >= segment.endMillis) {
                    player.seekTo(segment.startMillis)
                    currentPosition = segment.startMillis
                }
            }
            delay(250)
        }
    }

    LaunchedEffect(activeSegment, player) {
        activeSegment?.let { segment ->
            player.seekTo(segment.startMillis)
            currentPosition = segment.startMillis
            player.playWhenReady = true
            isPaused = false
        }
    }

    LaunchedEffect(videoSegments) {
        if (activeSegment != null && activeSegment !in videoSegments) {
            activeSegment = null
        }
    }

    DisposableEffect(player) {
        onDispose {
            currentPlaybackStateChange?.invoke(
                player.currentPosition.coerceAtLeast(0L),
                player.playWhenReady,
            )
            player.release()
        }
    }

    LaunchedEffect(player, playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(player, isToneCurveEditing) {
        if (isToneCurveEditing) {
            resumePlaybackAfterAdjustment = player.playWhenReady
            player.pause()
            isPaused = true
        } else if (resumePlaybackAfterAdjustment) {
            player.play()
            isPaused = false
        }
        if (!isToneCurveEditing) {
            resumePlaybackAfterAdjustment = false
        }
    }

    LaunchedEffect(player, item.uri, toneCurve, colorAdjustments, isToneCurveEditing) {
        val nextKey = VideoEffectsKey(toneCurve, colorAdjustments)
        if (appliedVideoEffectsKey == nextKey) return@LaunchedEffect
        if (appliedVideoEffectsKey == null && nextKey.isIdentity) {
            appliedVideoEffectsKey = nextKey
            return@LaunchedEffect
        }
        if (isToneCurveEditing) {
            delay(VIDEO_EFFECT_EDITING_DEBOUNCE_MILLIS)
        }
        val nextEffects = buildVideoEffectsInBackground(nextKey)
        val resumePlayback = !isToneCurveEditing && (
            player.playWhenReady || resumePlaybackAfterAdjustment
        )
        if (resumePlayback) {
            resumePlaybackAfterAdjustment = true
        }
        val positionMillis = player.currentPosition.coerceAtLeast(0L)
        player.applyVideoEffectsWithPipelineReset(
            mediaUri = item.uri,
            positionMillis = positionMillis,
            effects = nextEffects,
            resumePlayback = resumePlayback,
        ) { paused ->
            isPaused = paused
        }
        appliedVideoEffectsKey = nextKey
        if (resumePlayback) {
            resumePlaybackAfterAdjustment = false
        }
    }

    LaunchedEffect(controlsAutoHideToken) {
        if (controlsAutoHideToken == 0) return@LaunchedEffect
        delay(CONTROLS_AFTER_SCRUB_VISIBLE_MILLIS)
        controlsVisible = false
    }

    LaunchedEffect(controlsTapGuardToken) {
        if (controlsTapGuardToken == 0) return@LaunchedEffect
        suppressNextControlsTap = true
        delay(CONTROLS_SCRUB_TAP_GUARD_MILLIS)
        suppressNextControlsTap = false
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
            aspectRatioOverride = playerFrameAspectRatio,
            rotationDegrees = rotationDegrees,
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    // A decoded poster frame covers the initial load as well as devices where
                    // a TextureView has not retained a frame yet.
                    model = videoFrameImageModel(item),
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )

                AndroidView(
                    factory = { viewContext ->
                        (LayoutInflater.from(viewContext)
                            .inflate(R.layout.view_video_player, null, false) as PlayerView).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setKeepContentOnPlayerReset(true)
                            setEnableComposeSurfaceSyncWorkaround(true)
                            this.player = player
                            playerView = this
                        }
                    },
                    update = {
                        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                        it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        it.setKeepContentOnPlayerReset(true)
                        it.setEnableComposeSurfaceSyncWorkaround(true)
                        it.player = player
                        it.requestLayout()
                        it.invalidate()
                    },
                    modifier = Modifier.fillMaxSize(),
                )

            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(item.uri) {
                    detectVideoGestures(
                    onTap = {
                        if (suppressNextControlsTap) {
                            suppressNextControlsTap = false
                            controlsVisible = true
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    },
                        onDoubleTap = {
                            player.playWhenReady = !player.playWhenReady
                            isPaused = !player.playWhenReady
                            controlsVisible = true
                        },
                        onHorizontalSwipeStart = {
                            horizontalSeekBasePosition = player.currentPosition.coerceAtLeast(0L)
                            resumeAfterHorizontalSeek = player.playWhenReady
                            horizontalSeekOffset = null
                            player.pause()
                        },
                        onHorizontalSwipe = { seekOffset ->
                            val targetPosition = (horizontalSeekBasePosition + seekOffset)
                                .coerceIn(0L, duration.coerceAtLeast(fallbackDurationMillis))
                            player.seekTo(targetPosition)
                            currentPosition = targetPosition
                            horizontalSeekOffset = seekOffset
                            VideoSeekGesture.feedbackText(seekOffset).also { feedback ->
                                seekFeedbackText = feedback
                                seekFeedbackDisplayText = feedback
                            }
                        },
                        onHorizontalSwipeEnd = {
                            if (resumeAfterHorizontalSeek) {
                                player.play()
                                isPaused = false
                            }
                            resumeAfterHorizontalSeek = false
                        },
                        onVerticalSwipe = {},
                        durationMillis = { duration.coerceAtLeast(fallbackDurationMillis) },
                    )
                },
        )

        val actionBottomAnchor = maxHeight / 4
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
                .padding(end = 16.dp, bottom = actionBottomAnchor),
        )
        ViewerFavoritePicker(
            favoriteLevel = item.favoriteLevel,
            onFavoriteLevelChange = { level ->
                onSetMediaFavoriteLevel(item.uri, level)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = actionBottomAnchor - 64.dp),
        )
        DefaultFavoriteFolderButton(
            isFavorite = isInDefaultFavoriteFolder,
            onClick = onToggleDefaultFavoriteFolder,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = actionBottomAnchor - 140.dp),
        )
        SegmentMarkerButton(
            isSettingEnd = segmentStartMillis != null,
            onClick = {
                controlsVisible = true
                val start = segmentStartMillis
                if (start == null) {
                    segmentStartMillis = player.currentPosition.coerceAtLeast(0L)
                } else {
                    val end = player.currentPosition.coerceAtLeast(0L)
                    if (end > start) {
                        onAddVideoSegment(start, end)
                        segmentStartMillis = null
                    } else {
                        horizontalSeekOffset = null
                        seekFeedbackText = "结束时间需晚于起点"
                        seekFeedbackDisplayText = seekFeedbackText.orEmpty()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = actionBottomAnchor - 220.dp),
        )
        VideoSegmentList(
            segments = videoSegments,
            activeSegment = activeSegment,
            onToggleSegment = { segment ->
                activeSegment = if (activeSegment == segment) null else segment
            },
            onDeleteSegment = onDeleteVideoSegment,
            onRenameSegment = onRenameVideoSegment,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 68.dp, end = 16.dp),
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
            modifier = when {
                horizontalSeekOffset != null && horizontalSeekOffset!! < 0L -> Modifier
                    .align(Alignment.Center)
                    .offset(x = (-160).dp)
                horizontalSeekOffset != null -> Modifier
                    .align(Alignment.Center)
                    .offset(x = 160.dp)
                else -> Modifier.align(Alignment.Center)
            },
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
            if (horizontalSeekOffset != null) {
                val direction = if (horizontalSeekOffset!! < 0L) "<<<" else ">>>"
                Text(
                    text = "$direction  $seekFeedbackDisplayText",
                    modifier = Modifier
                        .background(Color(0xAA000000), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    seekFeedbackDisplayText,
                    modifier = Modifier
                        .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        AnimatedVisibility(
            visible = showFileName && !controlsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 64.dp, end = 64.dp, bottom = 20.dp),
            enter = fadeIn(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)),
            exit = fadeOut(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)),
        ) {
            Text(
                text = item.displayName,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                color = Color(0x70EAF2FF),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
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
                playbackMode = playbackMode,
                onOpenAuthorSearch = onOpenAuthorSearch,
                onSeek = { position ->
                    player.seekTo(position)
                    currentPosition = position
                },
                onScrubStart = {
                    resumeAfterScrubbing = player.playWhenReady
                    isScrubbing = true
                    controlsVisible = true
                    player.pause()
                },
                onScrubEnd = {
                    isScrubbing = false
                    if (resumeAfterScrubbing) {
                        player.play()
                        isPaused = false
                    } else {
                        player.pause()
                    }
                    resumeAfterScrubbing = false
                    controlsVisible = true
                    controlsAutoHideToken += 1
                    controlsTapGuardToken += 1
                },
                onPlaybackSpeedChange = { speed ->
                    playbackSpeed = speed
                },
                onTogglePlaybackMode = onTogglePlaybackMode,
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

@Composable
private fun SegmentMarkerButton(
    isSettingEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (isSettingEnd) "设置 B" else "设置 A",
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x99000000))
            .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoSegmentList(
    segments: List<VideoSegment>,
    activeSegment: VideoSegment?,
    onToggleSegment: (VideoSegment) -> Unit,
    onDeleteSegment: (VideoSegment) -> Unit,
    onRenameSegment: (VideoSegment, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (segments.isEmpty()) return
    var editingSegment by remember { mutableStateOf<VideoSegment?>(null) }
    var editingName by remember { mutableStateOf("") }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.sortedBy { it.startMillis }.forEach { segment ->
            var showDelete by remember(segment) { mutableStateOf(false) }
            Box {
                Text(
                    text = segment.name?.takeIf { it.isNotBlank() }
                        ?: "${segment.startMillis.formatDuration()}-${segment.endMillis.formatDuration()}",
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (activeSegment == segment) Color(0xFFE9C55D) else Color(0x99000000),
                        )
                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = { onToggleSegment(segment) },
                            onLongClick = { showDelete = true },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (activeSegment == segment) Color(0xFF3F2B00) else Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (showDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "编辑精彩片段名称",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-7).dp, y = (-7).dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF2867B2))
                            .clickable {
                                editingSegment = segment
                                editingName = segment.name.orEmpty()
                                showDelete = false
                            }
                            .padding(3.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = "×",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-7).dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFFD93025))
                            .clickable {
                                onDeleteSegment(segment)
                                showDelete = false
                            },
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    editingSegment?.let { segment ->
        AlertDialog(
            onDismissRequest = { editingSegment = null },
            title = { Text("编辑精彩片段名称") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("留空时将显示时间范围")
                    TextField(
                        value = editingName,
                        onValueChange = { editingName = it.take(40) },
                        singleLine = true,
                        label = { Text("片段名称") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameSegment(segment, editingName)
                        editingSegment = null
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                Button(onClick = { editingSegment = null }) { Text("取消") }
            },
        )
    }
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
    onHorizontalSwipeStart: () -> Unit,
    onHorizontalSwipe: (Long) -> Unit,
    onHorizontalSwipeEnd: () -> Unit,
    onVerticalSwipe: (SwipeIntent) -> Unit,
    durationMillis: () -> Long,
) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        var totalDragX = 0f
        var totalDragY = 0f
        var moved = false
        var horizontalSwipeStarted = false
        var lastSeekOffset: Long? = null

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
                    val seekOffset = if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
                        VideoSeekGesture.seekOffsetMillis(
                            totalDragX = totalDragX,
                            viewportWidthPx = size.width.toFloat(),
                            durationMillis = durationMillis(),
                        )
                    } else {
                        null
                    }
                    if (seekOffset != null && seekOffset != lastSeekOffset) {
                        if (!horizontalSwipeStarted) {
                            horizontalSwipeStarted = true
                            onHorizontalSwipeStart()
                        }
                        onHorizontalSwipe(seekOffset)
                        lastSeekOffset = seekOffset
                    }
                }
            }
        } while (event.changes.any { it.pressed })

        when {
            horizontalSwipeStarted -> onHorizontalSwipeEnd()
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
            else -> SwipeIntent.fromVerticalDrag(totalDragY)?.let(onVerticalSwipe)
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
    playbackMode: PlaybackMode,
    onOpenAuthorSearch: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onTogglePlaybackMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            FilenameInfoBubble(
                displayName = displayName,
                onOpenAuthorSearch = onOpenAuthorSearch,
                modifier = Modifier
                    .padding(start = 32.dp, end = 32.dp, bottom = 54.dp)
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
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (playbackMode == PlaybackMode.Shuffle) Color(0x44FFFFFF) else Color.Transparent)
                    .clickable { onTogglePlaybackMode() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playbackMode == PlaybackMode.Shuffle) Icons.Rounded.Shuffle else Icons.Rounded.Repeat,
                    contentDescription = if (playbackMode == PlaybackMode.Shuffle) "随机播放" else "顺序播放",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White,
                )
            }
            Text(
                PlaybackTimeFormatter.formatMillis(safePosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            CompactSeekBar(
                currentPosition = safePosition,
                duration = safeDuration,
                onSeek = onSeek,
                onScrubStart = onScrubStart,
                onScrub = onSeek,
                onScrubEnd = onScrubEnd,
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
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val progress = if (duration > 0L) {
        currentPosition.toFloat() / duration.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val thumbHalfPx = with(density) { 5.dp.toPx() }

    fun positionFromX(x: Float): Long? {
        if (widthPx <= 0) return null
        val fraction = (x / widthPx).coerceIn(0f, 1f)
        return (duration * fraction).toLong()
    }

    Box(
        modifier = modifier
            .height(18.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(duration, widthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrubStart()
                    positionFromX(down.position.x)?.let(onScrub)
                    down.consume()

                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                positionFromX(change.position.x)?.let(onScrub)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    onScrubEnd()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x66FFFFFF)),
        )
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
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
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White),
        )
    }
}


@Composable
private fun InfoButton(
    displayName: String,
    onOpenAuthorSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            FilenameInfoBubble(
                displayName = displayName,
                onOpenAuthorSearch = onOpenAuthorSearch,
                modifier = Modifier
                    .padding(bottom = 42.dp)
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

@Composable
private fun FilenameInfoBubble(
    displayName: String,
    onOpenAuthorSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val authorId = displayName.substringBefore('_').trim()
    Row(
        modifier = modifier
            .background(Color(0x99000000), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = authorId.isNotBlank()) { onOpenAuthorSearch(authorId) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "搜索作者 $authorId",
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )
        }
        Text(
            displayName,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clickable { context.copyTextToClipboard("filename", displayName) }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
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
