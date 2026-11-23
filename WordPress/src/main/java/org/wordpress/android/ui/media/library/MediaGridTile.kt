package org.wordpress.android.ui.media.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.WPMediaUtils
import java.io.File
import java.util.Locale

/**
 * One tile in the media library grid. [selectionIndex] is the item's 1-based position in the selection, or null
 * when it isn't selected; [showSelectionBadge] is true while a selection is active and the item can be selected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Suppress("LongParameterList")
fun MediaGridTile(
    item: MediaGridItemUiState,
    thumbnailSource: MediaThumbnailSource,
    thumbnailWidthPx: Int,
    selectionIndex: Int?,
    showSelectionBadge: Boolean,
    canLoadVideoFrame: suspend (String) -> Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRetryClick: () -> Unit,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectionIndex != null
    val scale by animateFloatAsState(if (isSelected) SCALE_SELECTED else 1f, label = "tileScale")
    val description = stringResource(R.string.media_grid_item_image_desc, item.fileName)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(TILE_ASPECT_RATIO)
            .padding(TILE_PADDING)
            .semantics { selected = isSelected }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                // read in the layer so the selection animation only redraws the tile
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            when (item.type) {
                MediaGridItemType.IMAGE -> MediaImage(
                    model = item.localFilePath?.let { File(it) }
                        ?: item.bestImageUrl(thumbnailSource, thumbnailWidthPx, thumbnailHeightPx(thumbnailWidthPx))
                )
                MediaGridItemType.VIDEO -> VideoThumbnail(item, canLoadVideoFrame)
                MediaGridItemType.OTHER -> FileTile(item)
            }

            if (item.uploadState != MediaUploadState.UPLOADED) {
                UploadStateOverlay(item.uploadState, onRetryClick, onTrashClick)
            } else if (item.type == MediaGridItemType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.media_grid_item_play_video_desc),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(PLAY_ICON_SIZE)
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA), CircleShape)
                        .padding(PLAY_ICON_PADDING)
                )
            }
        }

        if (showSelectionBadge) {
            SelectionBadge(
                selectionIndex = selectionIndex,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(BADGE_MARGIN)
            )
        }
    }
}

@Composable
private fun MediaImage(model: Any?) {
    val context = LocalContext.current
    val request = remember(model) { ImageRequest.Builder(context).data(model).crossfade(true).build() }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * A server-supplied thumbnail when there is one, else a frame from the local file, else a frame from the remote
 * video if it's small enough to download.
 */
@Composable
private fun VideoThumbnail(item: MediaGridItemUiState, canLoadVideoFrame: suspend (String) -> Boolean) {
    val thumbnailUrl = item.thumbnailUrl?.takeIf { it.isNotEmpty() && !MediaUtils.isVideo(it) }
    val remoteUrl = item.url?.takeIf { thumbnailUrl == null && item.localFilePath == null && it.isNotEmpty() }
    val canLoadRemoteFrame by produceState(initialValue = false, remoteUrl) {
        value = remoteUrl != null && canLoadVideoFrame(remoteUrl)
    }
    val model: Any? = thumbnailUrl
        ?: item.localFilePath?.let { File(it) }
        ?: remoteUrl?.takeIf { canLoadRemoteFrame }
    if (model != null) {
        MediaImage(model)
    }
}

@Composable
private fun FileTile(item: MediaGridItemUiState) {
    val iconResId = remember(item.fileName, item.mimeType) { WPMediaUtils.getPlaceholder(item.fileName, item.mimeType) }
    val extension = remember(item.mimeType) {
        MediaUtils.getExtensionForMimeType(item.mimeType).uppercase(Locale.ROOT)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FILE_TILE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(FILE_ICON_SIZE)
        )
        if (extension.isNotEmpty()) {
            Spacer(modifier = Modifier.height(FILE_TEXT_GAP))
            Text(
                text = extension,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UploadStateOverlay(state: MediaUploadState, onRetryClick: () -> Unit, onTrashClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // failed uploads can be retried or deleted, queued items can be deleted
        if (state == MediaUploadState.FAILED || state == MediaUploadState.QUEUED) {
            Row {
                if (state == MediaUploadState.FAILED) {
                    OverlayIconButton(
                        onClick = onRetryClick,
                        contentDescription = stringResource(R.string.media_grid_item_retry_desc)
                    ) { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                }
                OverlayIconButton(
                    onClick = onTrashClick,
                    contentDescription = stringResource(R.string.media_grid_item_trash_desc)
                ) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) }
            }
        }
        if (state == MediaUploadState.UPLOADING || state == MediaUploadState.DELETING) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = PROGRESS_STROKE,
                modifier = Modifier
                    .size(PROGRESS_SIZE)
                    .padding(bottom = FILE_TEXT_GAP)
            )
        }
        Text(
            text = stringResource(state.labelResId()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OverlayIconButton(onClick: () -> Unit, contentDescription: String, icon: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            modifier = Modifier
                .size(OVERLAY_BUTTON_SIZE)
                .background(Color.Black.copy(alpha = SCRIM_ALPHA), CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
    }
}

@Composable
private fun SelectionBadge(selectionIndex: Int?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = if (selectionIndex != null) colors.primary else Color.Black.copy(alpha = SCRIM_ALPHA)
    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .background(background, CircleShape)
            .border(BADGE_BORDER, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selectionIndex != null) {
            Text(
                text = selectionIndex.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimary
            )
        }
    }
}

private fun MediaUploadState.labelResId(): Int = when (this) {
    MediaUploadState.QUEUED -> R.string.media_upload_state_queued
    MediaUploadState.UPLOADING -> R.string.media_upload_state_uploading
    MediaUploadState.DELETING -> R.string.media_upload_state_deleting
    MediaUploadState.DELETED -> R.string.media_upload_state_deleted
    MediaUploadState.FAILED -> R.string.media_upload_state_failed
    MediaUploadState.UPLOADED -> R.string.media_upload_state_uploaded
}

/** Height of a tile for the given width */
private fun thumbnailHeightPx(widthPx: Int): Int = (widthPx / TILE_ASPECT_RATIO).toInt()

private const val TILE_ASPECT_RATIO = 4f / 3f
private const val SCALE_SELECTED = 0.8f
private const val SCRIM_ALPHA = 0.5f
private val TILE_PADDING = 2.dp
private val FILE_TILE_PADDING = 8.dp
private val FILE_ICON_SIZE = 24.dp
private val FILE_TEXT_GAP = 4.dp
private val PLAY_ICON_SIZE = 32.dp
private val PLAY_ICON_PADDING = 4.dp
private val PROGRESS_SIZE = 28.dp
private val PROGRESS_STROKE = 2.dp
private val OVERLAY_BUTTON_SIZE = 36.dp
private val BADGE_SIZE = 24.dp
private val BADGE_BORDER = 1.5.dp
private val BADGE_MARGIN = 8.dp
