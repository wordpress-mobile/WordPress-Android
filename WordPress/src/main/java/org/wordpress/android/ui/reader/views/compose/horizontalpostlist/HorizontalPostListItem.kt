package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun HorizontalPostListItem(
    siteName: String,
    siteImageUrl: String,
    postDateLine: String,
    postTitle: String,
    postExcerpt: String,
    postImageUrl: String,
    postNumberOfLikesText: String,
    postNumberOfCommentsText: String,
    onSiteImageClick: () -> Unit,
    onPostImageClick: () -> Unit,
) {
    val primaryElementColor = AppColor.Black.copy(
        alpha = 0.87F
    )
    val secondaryElementColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.6F
    )
    Column(modifier = Modifier.width(240.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Site image
            SiteImage(
                modifier = Modifier.padding(
                    end = Margin.Small.value,
                ),
                imageUrl = siteImageUrl,
                onClick = { onSiteImageClick() },
            )
            // Site name
            Text(
                modifier = Modifier.weight(1F),
                text = siteName,
                style = MaterialTheme.typography.labelLarge,
                color = primaryElementColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // "•" separator
            Text(
                modifier = Modifier.padding(
                    horizontal = Margin.Small.value
                ),
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
            // Time since it was posted
            Text(
                text = postDateLine,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
        }
        // Post title
        Text(
            modifier = Modifier.padding(top = Margin.Medium.value),
            text = postTitle,
            style = MaterialTheme.typography.titleMedium,
            color = AppColor.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // Post excerpt
        Text(
            modifier = Modifier.padding(
                top = Margin.Small.value,
                bottom = Margin.Small.value,
            ),
            text = postExcerpt,
            style = MaterialTheme.typography.bodySmall,
            color = primaryElementColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // Post image
        PostImage(
            imageUrl = postImageUrl,
            onClick = onPostImageClick,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Number of likes
            Text(
                text = postNumberOfLikesText,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
            // "•" separator
            Text(
                modifier = Modifier.padding(
                    horizontal = Margin.Small.value
                ),
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
            // Number of comments
            Text(
                text = postNumberOfCommentsText,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
        }
    }
}

@Composable
fun SiteImage(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .error(R.drawable.bg_oval_placeholder_image_32dp)
            .crossfade(true)
            .build(),
        contentDescription = null
    )
}

@Composable
fun PostImage(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier
            .width(240.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(corner = CornerSize(8.dp)))
            .clickable { onClick() },
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            // TODO RenanLukas: placeholder
            // .error(R.drawable.bg_oval_placeholder_image_32dp)
            .crossfade(true)
            .build(),
        contentDescription = null
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            HorizontalPostListItem(
                siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed" +
                        " urna fermentum posuere. Vivamus in pretium nisl.",
                siteImageUrl = "",
                postDateLine = "1h",
                postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien " +
                        "sed urna fermentum posuere. Vivamus in pretium nisl.",
                postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien" +
                        " sed urna fermentum posuere. Vivamus in pretium nisl.",
                postImageUrl = "",
                postNumberOfLikesText = "15 likes",
                postNumberOfCommentsText = "4 comments",
                onSiteImageClick = {},
                onPostImageClick = {},
            )
        }
    }
}
