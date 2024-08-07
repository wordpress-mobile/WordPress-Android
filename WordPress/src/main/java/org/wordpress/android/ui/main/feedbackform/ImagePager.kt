package org.wordpress.android.ui.main.feedbackform

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePager(
    imageFiles: List<File>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        pageCount = { imageFiles.size }
    )
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        pageSize = PageSize.Fixed(IMAGE_SIZE.dp),
        modifier = Modifier.then(modifier)
    ) { index ->
        ImageFile(imageFiles[index])
    }
}

@Composable
private fun ImageFile(file: File) {
    // TODO thumbnails
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    Row(
        modifier = Modifier
            .height(IMAGE_SIZE.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }
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
    val attachment1 = File("/tmp/attachment.jpg")
    val attachment2 = File("/tmp/attachment.mp4")
    val attachments = listOf(attachment1, attachment2)
    ImagePager(
        imageFiles = attachments
    )
}

private const val IMAGE_SIZE = 128
