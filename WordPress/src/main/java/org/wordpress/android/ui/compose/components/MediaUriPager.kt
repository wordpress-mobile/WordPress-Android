package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import org.wordpress.android.R

/**
 * A simple pager to show a carousel from a list of local media URIs. This was designed
 * to show feedback form attachments but should be suitable for other use cases.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaUriPager(
    mediaUris: List<Uri>,
    modifier: Modifier = Modifier,
    showButton: Boolean = true,
    onButtonClick: (Uri) -> Unit = {},
) {
    if (mediaUris.isEmpty()) {
        return
    }
    val pagerState = rememberPagerState(
        pageCount = { mediaUris.size }
    )
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        pageSize = PageSize.Fixed(IMAGE_SIZE.dp),
        modifier = Modifier.then(modifier)
    ) { index ->
        val uri = mediaUris[index]
        Box(
            modifier = Modifier.height(IMAGE_SIZE.dp),
        ) {
            MediaUriImage(uri)
            if (showButton) {
                ImageButton(
                    uri = uri,
                    itemNumber = index + 1,
                    onButtonClick = onButtonClick
                )
            }
        }
    }
}

@Composable
private fun MediaUriImage(uri: Uri) {
    val context = LocalContext.current
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType?.startsWith("video/") == true) {
        val imageLoader = ImageLoader.Builder(LocalContext.current)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
                    .size(IMAGE_SIZE.dp)
            )
            Image(
                imageVector = ImageVector.vectorResource(id = org.wordpress.android.editor.R.drawable.ic_overlay_video),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .placeholder(R.color.placeholder)
                .error(org.wordpress.android.editor.R.drawable.ic_image_failed_grey_a_40_48dp)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
                .size(IMAGE_SIZE.dp)
        )
    }
}

@Composable
private fun BoxScope.ImageButton(
    uri: Uri,
    itemNumber: Int,
    onButtonClick: (Uri) -> Unit = {},
) {
    IconButton(
        onClick = { onButtonClick(uri) },
        modifier = Modifier
            .absoluteOffset(x = (-2).dp, y = (-2).dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(2.dp)
            )
            .size(24.dp)
            .align(Alignment.BottomEnd),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = stringResource(
                R.string.media_pager_remove_item_content_description,
                itemNumber
            )
        )
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MediaPagerPreview() {
    val attachment1 = Uri.parse("/tmp/attachment.jpg")
    val attachment2 = Uri.parse("/tmp/attachment.mp4")
    MediaUriPager(
        mediaUris = listOf(attachment1, attachment2)
    )
}

private const val IMAGE_SIZE = 128
