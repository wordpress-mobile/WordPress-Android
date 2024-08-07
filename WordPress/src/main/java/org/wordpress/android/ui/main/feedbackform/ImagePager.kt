package org.wordpress.android.ui.main.feedbackform

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePager(
    imageFiles: List<File>
) {
    val pagerState = rememberPagerState(pageCount = {
        imageFiles.size
    })
    HorizontalPager(state = pagerState) { index ->
        Text(
            text = "Page: $index",
            modifier = Modifier.fillMaxWidth()
        )
        ImageFile(imageFiles[index])
    }
}

@Composable
private fun ImageFile(file: File) {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    Row {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.height(MAX_SIZE.dp)
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
    val attachment1 = File("/tmp/attachment.jpg")
    val attachment2 = File("/tmp/attachment.mp4")
    val attachments = listOf(attachment1, attachment2)
    ImagePager(
        imageFiles = attachments
    )
}

private const val MAX_SIZE = 64
