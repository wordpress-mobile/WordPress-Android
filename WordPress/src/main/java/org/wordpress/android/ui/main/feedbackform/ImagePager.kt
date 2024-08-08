package org.wordpress.android.ui.main.feedbackform

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePager(
    imageUris: List<Uri>,
    modifier: Modifier = Modifier
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
        UriImage(imageUris[index])
    }
}

@Composable
private fun UriImage(uri: Uri) {
    Row(
        modifier = Modifier
            .height(IMAGE_SIZE.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
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
    val attachment1 = Uri.parse("/tmp/attachment.jpg")
    val attachment2 = Uri.parse("/tmp/attachment.mp4")
    val attachments = listOf(attachment1, attachment2)
    ImagePager(
        imageUris = attachments
    )
}

private const val IMAGE_SIZE = 128
