package app.videosee

import android.Manifest
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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import app.videosee.ui.PlaybackSpeedOptions
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
            background = Color(0xFF111317),
            surface = Color(0xFF171A20),
            surfaceVariant = Color(0xFF20242C),
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
    val state by viewModel.uiState.collectAsState()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionChanged(context.hasMediaPermission())
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
        onSelectBrowserMode = viewModel::selectBrowserMode,
        onSelectCollectionSortField = viewModel::selectCollectionSortField,
        onToggleCollectionSortDirection = viewModel::toggleCollectionSortDirection,
        onSelectMediaSortField = viewModel::selectMediaSortField,
        onToggleMediaSortDirection = viewModel::toggleMediaSortDirection,
        onOpenItem = viewModel::openViewer,
        onCloseViewer = viewModel::closeViewer,
        onNext = viewModel::showNext,
        onPrevious = viewModel::showPrevious,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun VideoSeeApp(
    state: VideoSeeUiState,
    onRequestPermission: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectAuthor: (String) -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSelectMediaSortField: (MediaSortField) -> Unit,
    onToggleMediaSortDirection: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onCloseViewer: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRefresh: () -> Unit,
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
                    onSelectBrowserMode = onSelectBrowserMode,
                    onSelectCollectionSortField = onSelectCollectionSortField,
                    onToggleCollectionSortDirection = onToggleCollectionSortDirection,
                    onSelectMediaSortField = onSelectMediaSortField,
                    onToggleMediaSortDirection = onToggleMediaSortDirection,
                    onOpenItem = onOpenItem,
                    onRefresh = onRefresh,
                )
            }
        }

        AnimatedContent(
            targetState = state.viewerItem,
            transitionSpec = {
                (fadeIn(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                    scaleIn(
                        animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS),
                        initialScale = 0.98f,
                    ))
                    .togetherWith(
                        fadeOut(animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS)) +
                            scaleOut(
                                animationSpec = tween(ViewerUiSpec.VIEWER_TRANSITION_DURATION_MILLIS),
                                targetScale = 0.98f,
                            ),
                    )
            },
            label = "viewer_transition",
        ) { item ->
            item?.let {
                val viewerIndex = state.selectedItems.indexOfFirst { media -> media.uri == it.uri }
                MediaViewer(
                    item = it,
                    hasPrevious = viewerIndex > 0,
                    hasNext = viewerIndex >= 0 && viewerIndex < state.selectedItems.lastIndex,
                    onClose = onCloseViewer,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )
            }
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
    onSelectBrowserMode: (BrowserMode) -> Unit,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSelectMediaSortField: (MediaSortField) -> Unit,
    onToggleMediaSortDirection: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onRefresh: () -> Unit,
) {
    val collections = state.visibleCollections
    val selectedCollection = state.selectedCollection
    val onSelectCollection = when (state.browserMode) {
        BrowserMode.Folder -> onSelectFolder
        BrowserMode.Author -> onSelectAuthor
    }

    Row(Modifier.fillMaxSize()) {
        FolderRail(
            collections = collections,
            selectedCollectionId = selectedCollection?.id,
            browserMode = state.browserMode,
            hasAuthors = state.authors.isNotEmpty(),
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            onSelectBrowserMode = onSelectBrowserMode,
            collectionSortField = state.collectionSortField,
            collectionSortDirection = state.collectionSortDirection,
            onSelectCollectionSortField = onSelectCollectionSortField,
            onToggleCollectionSortDirection = onToggleCollectionSortDirection,
            onSelectCollection = onSelectCollection,
            modifier = Modifier.width(172.dp).fillMaxHeight(),
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF252A33)),
        )
        Crossfade(
            targetState = selectedCollection,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            animationSpec = tween(ViewerUiSpec.FOLDER_SWITCH_DURATION_MILLIS),
            label = "media_collection_switch",
        ) { collection ->
            if (collection == null) {
                EmptyScreen("No media found")
            } else {
                Column(Modifier.fillMaxHeight()) {
                    MediaSortToolbar(
                        sortField = state.mediaSortField,
                        sortDirection = state.mediaSortDirection,
                        onSelectSortField = onSelectMediaSortField,
                        onToggleSortDirection = onToggleMediaSortDirection,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MediaGrid(
                        items = state.selectedItems,
                        onOpenItem = onOpenItem,
                        modifier = Modifier.weight(1f),
                    )
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
            text = sortDirection.label(),
            selected = true,
            onClick = onToggleSortDirection,
        )
    }
}

@Composable
private fun CollectionSortToolbar(
    sortField: CollectionSortField,
    sortDirection: SortDirection,
    onSelectSortField: (CollectionSortField) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(74.dp)
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SortChip(
                    text = sortDirection.label(),
                    selected = true,
                    onClick = onToggleSortDirection,
                    compact = true,
                )
            }
        }
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
        targetValue = if (selected) Color(0xFFFF8A00) else Color(0xFF20242C),
        animationSpec = tween(ViewerUiSpec.SELECTION_TRANSITION_DURATION_MILLIS),
        label = "sort_chip_background",
    )
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 6.dp),
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
    selectedCollectionId: String?,
    browserMode: BrowserMode,
    hasAuthors: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectBrowserMode: (BrowserMode) -> Unit,
    collectionSortField: CollectionSortField,
    collectionSortDirection: SortDirection,
    onSelectCollectionSortField: (CollectionSortField) -> Unit,
    onToggleCollectionSortDirection: () -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Color(0xFF15181D))) {
        FolderToolbar(
            browserMode = browserMode,
            hasAuthors = hasAuthors,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onSelectBrowserMode = onSelectBrowserMode,
            modifier = Modifier.fillMaxWidth(),
        )
        CollectionSortToolbar(
            sortField = collectionSortField,
            sortDirection = collectionSortDirection,
            onSelectSortField = onSelectCollectionSortField,
            onToggleSortDirection = onToggleCollectionSortDirection,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(collections, key = { it.id }) { collection ->
                val selected = collection.id == selectedCollectionId
                val rowBackground by animateColorAsState(
                    targetValue = if (selected) Color(0xFF25313A) else Color.Transparent,
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
                        model = mediaImageModel(collection.previewItem),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF252A33)),
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
                        Text(
                            "${collection.count}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderToolbar(
    browserMode: BrowserMode,
    hasAuthors: Boolean,
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
        Text(
            if (isRefreshing) "刷新" else "刷新",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF20242C))
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF20242C),
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
private fun MediaGrid(
    items: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(124.dp),
        modifier = modifier.padding(10.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.uri }) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF20242C))
                    .clickable { onOpenItem(item) },
            ) {
                AsyncImage(
                    model = mediaImageModel(item),
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
            }
        }
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
) {
    var scale by remember(item.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(item.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.uri) { mutableFloatStateOf(0f) }
    var swipeOffsetY by remember(item.uri) { mutableFloatStateOf(0f) }
    val settleOffsetY = remember(item.uri) { Animatable(0f) }
    var settleRequest by remember(item.uri) { mutableStateOf<SwipeSettleRequest?>(null) }
    var settleSerial by remember(item.uri) { mutableStateOf(0) }
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
            SwipeIntent.Next -> onNext()
            SwipeIntent.Previous -> onPrevious()
            null -> {
                settleRequest = null
                swipeOffsetY = 0f
                settleOffsetY.snapTo(0f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewportHeight = it.height.toFloat() }
            .pointerInput(item.uri, hasPrevious, hasNext, viewportHeight) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalSwipe = 0f
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
                                if (settleRequest != null) {
                                    settleRequest = null
                                }
                                change?.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val intent = if (usedMultiTouch) null else SwipeIntent.fromVerticalDrag(totalSwipe)
                    settleSerial += 1
                    when (intent) {
                        SwipeIntent.Next -> {
                            if (hasNext && viewportHeight > 0f) {
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = -viewportHeight,
                                    intent = SwipeIntent.Next,
                                )
                            } else {
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
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = viewportHeight,
                                    intent = SwipeIntent.Previous,
                                )
                            } else {
                                settleRequest = SwipeSettleRequest(
                                    serial = settleSerial,
                                    startOffsetY = 0f,
                                    targetOffsetY = 0f,
                                    intent = null,
                                )
                            }
                        }

                        null -> {
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
            )
        }

        if (item.mediaType == MediaType.Image) {
            InfoButton(
                displayName = item.displayName,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

private data class SwipeSettleRequest(
    val serial: Int,
    val startOffsetY: Float,
    val targetOffsetY: Float,
    val intent: SwipeIntent?,
)

private const val VideoGestureDoubleTapTimeoutMillis = 280L

@Composable
private fun MediaSurface(
    item: MediaItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    activeVideo: Boolean,
) {
    when {
        item.mediaType == MediaType.Video && activeVideo -> VideoPlayer(
            item = item,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
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
private fun mediaImageModel(item: MediaItem?): Any? {
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
                        modifier = Modifier
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
                }
            }
        }

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
        if (isSaving) "保存中" else "截图",
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x88000000))
            .clickable(enabled = !isSaving) { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
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
