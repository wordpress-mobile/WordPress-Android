package org.wordpress.android.ui.main.feedbackform

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UriImagePager(
    imageUris: List<Uri>,
    modifier: Modifier = Modifier,
    showButton: Boolean = true,
    onButtonClick: (Uri) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        pageCount = { imageUris.size }
    )
    val context = LocalContext.current
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        pageSize = PageSize.Fixed(IMAGE_SIZE.dp),
        modifier = Modifier.then(modifier)
    ) { index ->
        val uri = imageUris[index]
        Box(
            modifier = Modifier.height(IMAGE_SIZE.dp),
        ) {
            UriImage(uri, context)
            if (showButton) {
                ImageButton(uri, onButtonClick)
            }
        }
    }
}

@Composable
private fun UriImage(
    uri: Uri,
    context: Context
) {
    val mimeType = context.contentResolver.getType(uri)
    val isVideo =  mimeType?.startsWith("video/") == true
    // video thumbnails are not supported yet
    @DrawableRes val errorRes = if (isVideo) {
        org.wordpress.android.editor.R.drawable.ic_overlay_video
    } else {
        R.drawable.ic_warning
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .placeholder(R.color.placeholder)
            .error(errorRes)
            .build(),
        contentScale = ContentScale.FillHeight,
        contentDescription = null,
    )
}

@Composable
private fun BoxScope.ImageButton(
    uri: Uri,
    onButtonClick: (Uri) -> Unit = {},
) {
    IconButton(
        onClick = { onButtonClick(uri) },
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .size(24.dp)
            .align(Alignment.BottomEnd),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.remove),
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
private fun ImagePagerPreview() {
    val attachment = Uri.parse("/tmp/attachment.jpg")
    UriImagePager(
        imageUris = listOf(attachment, attachment)
    )
}

private const val IMAGE_SIZE = 128
