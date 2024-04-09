package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun HorizontalPostListItemSiteImage(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .error(R.drawable.bg_oval_placeholder_image_32dp)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemSiteImagePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            HorizontalPostListItemSiteImage(
                imageUrl = "https://picsum.photos/200/300",
                onClick = {},
            )
        }
    }
}
