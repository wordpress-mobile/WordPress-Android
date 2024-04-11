package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.modifiers.conditionalThen
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
    postImageUrl: String?,
    postNumberOfLikesText: String,
    postNumberOfCommentsText: String,
    isPostLiked: Boolean,
    onSiteImageClick: () -> Unit,
    onPostImageClick: () -> Unit,
    onPostLikeClick: () -> Unit,
    onPostMoreMenuClick: () -> Unit,
) {
    val primaryElementColor = AppColor.Black.copy(
        alpha = 0.87F
    )
    val secondaryElementColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.6F
    )
    Column(modifier = Modifier
        .width(240.dp)
        .height(340.dp)) {
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
            modifier = Modifier
                .padding(
                    top = Margin.Small.value,
                    bottom = Margin.Medium.value,
                )
                .conditionalThen(
                    predicate = postImageUrl == null,
                    other = Modifier.height(180.dp)
                ),
            text = postExcerpt,
            style = MaterialTheme.typography.bodySmall,
            color = primaryElementColor,
            maxLines = if (postImageUrl != null) 2 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
        // Post image
        postImageUrl?.let {
            PostImage(
                imageUrl = it,
                onClick = onPostImageClick,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Margin.Medium.value),
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Like action
            TextButton(
                modifier = Modifier.defaultMinSize(minHeight = 24.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = { onPostLikeClick() },
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(
                        if (isPostLiked) {
                            R.drawable.ic_like_fill_new_24dp
                        } else {
                            R.drawable.ic_like_outline_new_24dp
                        }
                    ),
                    contentDescription = stringResource(
                        if (isPostLiked) {
                            R.string.mnu_comment_liked
                        } else {
                            R.string.reader_label_like
                        }
                    ),
                    tint = secondaryElementColor,
                )
                Text(
                    text = stringResource(R.string.reader_label_like),
                    color = secondaryElementColor,
                )
            }
            Spacer(Modifier.weight(1f))
            // More menu ("…")
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = {
                    onPostMoreMenuClick()
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_ellipsis_horizontal_squares),
                    contentDescription = stringResource(R.string.show_more_desc),
                    tint = secondaryElementColor,
                )
            }
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
            .height(150.dp)
            .clip(RoundedCornerShape(corner = CornerSize(8.dp)))
            .clickable { onClick() },
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = null
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemWithPostImagePreview() {
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
                postImageUrl = "postImageUrl",
                postNumberOfLikesText = "15 likes",
                postNumberOfCommentsText = "4 comments",
                isPostLiked = true,
                onSiteImageClick = {},
                onPostImageClick = {},
                onPostLikeClick = {},
                onPostMoreMenuClick = {},
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemWithoutPostImagePreview() {
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
                postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien " +
                        "sed urna fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, " +
                        "consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere. Vivamus" +
                        " in pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer " +
                        "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor " +
                        "sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum " +
                        "posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                        " Integer pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem " +
                        "ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna " +
                        "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur " +
                        "adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere. Vivamus in " +
                        "pretium nisl.",
                postImageUrl = null,
                postNumberOfLikesText = "15 likes",
                postNumberOfCommentsText = "4 comments",
                isPostLiked = true,
                onSiteImageClick = {},
                onPostImageClick = {},
                onPostLikeClick = {},
                onPostMoreMenuClick = {},
            )
        }
    }
}
