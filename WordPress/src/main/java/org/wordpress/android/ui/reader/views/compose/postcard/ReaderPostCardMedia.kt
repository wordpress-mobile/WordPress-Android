package org.wordpress.android.ui.reader.views.compose.postcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderThumbnailTable
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption
import org.wordpress.android.ui.reader.ReaderConstants.THUMBNAIL_STRIP_IMG_COUNT
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.GalleryThumbnailStripData
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils.VideoThumbnailUrlListener
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiDimen.UIDimenDPInt
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import java.util.EnumSet

private const val FEATURED_IMAGE_ASPECT_RATIO = 16f / 9f

/**
 * The featured image (with the video overlay for video posts) or, for gallery posts, the thumbnail strip.
 */
@Composable
fun ReaderPostCardMedia(uiState: ReaderPostUiState) {
    if (uiState.featuredImageVisibility) {
        FeaturedImage(uiState)
    }
    uiState.thumbnailStripSection?.let { ThumbnailStrip(it) }
}

@Composable
private fun FeaturedImage(uiState: ReaderPostUiState) {
    // A video post shows its thumbnail once resolved, and only the placeholder if there isn't one
    val imageUrl = uiState.fullVideoUrl.let { videoUrl ->
        if (videoUrl != null) rememberVideoThumbnail(uiState.postId, videoUrl) else uiState.featuredImageUrl
    }
    // Portrait images are fitted to the height on a background colour rather than center-cropped
    var isPortrait by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .aspectRatio(FEATURED_IMAGE_ASPECT_RATIO)
            .clip(RoundedCornerShape(uiState.featuredImageCornerRadius.toDp()))
            .background(colorResource(R.color.reader_featured_image_background)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .placeholder(R.drawable.bg_rectangle_placeholder_radius_4dp)
                .error(R.drawable.bg_rectangle_placeholder_radius_4dp)
                .fallback(R.color.placeholder)
                // Fixed so that switching contentScale for portrait images doesn't restart the load
                .scale(Scale.FILL)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (isPortrait) ContentScale.Fit else ContentScale.Crop,
            onSuccess = { success ->
                val drawable = success.result.drawable
                isPortrait = drawable.intrinsicHeight > drawable.intrinsicWidth
            },
        )
        if (uiState.videoOverlayVisibility) {
            Icon(
                painter = painterResource(R.drawable.ic_play_white_black_24dp),
                contentDescription = stringResource(R.string.reader_cardview_post_play_video_desc),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(56.dp)
                    .clickable { uiState.onVideoOverlayClicked(uiState.postId, uiState.blogId) },
            )
        }
    }
}

/**
 * Resolves a video's thumbnail through [ReaderVideoUtils], caching what it finds just as the
 * View-based card did. Null until resolved, or when there's no thumbnail and the placeholder shows.
 *
 * This runs during composition rather than in an effect because YouTube and cached thumbnails resolve
 * synchronously, so the first frame already has them; only Vimeo's arrives later.
 */
@Composable
private fun rememberVideoThumbnail(postId: Long, videoUrl: String): String? {
    val thumbnail = remember(videoUrl) {
        mutableStateOf<String?>(null).also { state ->
            ReaderVideoUtils.retrieveVideoThumbnailUrl(videoUrl, object : VideoThumbnailUrlListener {
                override fun showThumbnail(thumbnailUrl: String) {
                    state.value = thumbnailUrl
                }

                override fun showPlaceholder() {
                    state.value = null
                }

                override fun cacheThumbnailUrl(thumbnailUrl: String) {
                    ReaderThumbnailTable.addThumbnail(postId, videoUrl, thumbnailUrl)
                }
            })
        }
    }
    return thumbnail.value
}

/**
 * A row of gallery thumbnails; tapping one opens it in the photo viewer. Only shown when the post has
 * at least [THUMBNAIL_STRIP_IMG_COUNT] suitable images.
 */
@Composable
private fun ThumbnailStrip(data: GalleryThumbnailStripData) {
    if (data.images.size < THUMBNAIL_STRIP_IMG_COUNT) return
    val context = LocalContext.current
    val resources = LocalResources.current
    val height = dimensionResource(R.dimen.reader_thumbnail_strip_image_height)
    val heightPx = resources.getDimensionPixelSize(R.dimen.reader_thumbnail_strip_image_height)
    val widthPx = remember(resources) {
        val margins = resources.getDimensionPixelSize(R.dimen.reader_card_content_padding) * 2
        (DisplayUtils.getWindowPixelWidth(context) - margins) / THUMBNAIL_STRIP_IMG_COUNT
    }
    val photoViewerOptions = remember(data.isPrivate) {
        EnumSet.of(PhotoViewerOption.IS_GALLERY_IMAGE).apply {
            if (data.isPrivate) add(PhotoViewerOption.IS_PRIVATE_IMAGE)
        }
    }

    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        data.images.take(THUMBNAIL_STRIP_IMG_COUNT).forEach { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(PhotonUtils.getPhotonImageUrl(imageUrl, widthPx, heightPx))
                    .placeholder(R.color.placeholder)
                    .error(R.color.placeholder)
                    .build(),
                contentDescription = stringResource(R.string.reader_gallery_images_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clickable {
                        ReaderActivityLauncher.showReaderPhotoViewer(
                            context, imageUrl, data.content, null, photoViewerOptions, 0, 0
                        )
                    },
            )
        }
    }
}

@Composable
private fun UiDimen.toDp() = when (this) {
    is UIDimenRes -> dimensionResource(dimenRes)
    is UIDimenDPInt -> dimensionDP.dp
}
