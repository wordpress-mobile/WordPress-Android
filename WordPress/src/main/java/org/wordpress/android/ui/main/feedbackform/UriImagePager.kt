package org.wordpress.android.ui.main.feedbackform

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    showDeleteButton: Boolean = true,
    onDeleteClick: (Uri) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        pageCount = { imageUris.size }
    )
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
            UriImage(uri)
            if (showDeleteButton) {
                DeleteButton(uri, onDeleteClick)
            }
        }
    }
}

@Composable
private fun UriImage(uri: Uri) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .crossfade(true)
            .placeholder(R.color.placeholder)
            .error(R.drawable.ic_warning)
            .build(),
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
}

@Composable
private fun DeleteButton(
    uri: Uri,
    onDeleteClick: (Uri) -> Unit = {},
) {
    IconButton(
        onClick = { onDeleteClick(uri) },
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
