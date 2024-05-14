package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

private const val CONTENT_TOTAL_LINES = 3


@Composable
fun ReaderTagsFeedPostListItem(
    item: TagsFeedPostItem,
) = with(item) {
    val baseColor = if (isSystemInDarkTheme()) AppColor.White else AppColor.Black
    val primaryElementColor = baseColor.copy(
        alpha = 0.87F
    )
    val secondaryElementColor = baseColor.copy(
        alpha = 0.6F
    )

    val hasInteractions = postNumberOfLikesText.isNotBlank() || postNumberOfCommentsText.isNotBlank()

    Column(
        modifier = Modifier
            .width(ReaderTagsFeedComposeUtils.PostItemWidth)
            .height(ReaderTagsFeedComposeUtils.PostItemHeight),
        verticalArrangement = Arrangement.spacedBy(Margin.Small.value),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Site name
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSiteClick(item) },
                    ),
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

        // Post content row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Post text content
            PostTextContent(
                title = postTitle,
                excerpt = postExcerpt,
                onClick = { onPostCardClick(item) },
                titleColor = baseColor,
                excerptColor = primaryElementColor,
                modifier = Modifier
                    .weight(1f),
            )

            // Post image
            if (postImageUrl.isNotBlank()) {
                PostImage(
                    imageUrl = postImageUrl,
                    onClick = { onPostCardClick(item) },
                )
            }
        }

        // Likes and comments row
        if (hasInteractions) {
            val interactionTextStyle = MaterialTheme.typography.bodySmall

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Number of likes
                Text(
                    text = postNumberOfLikesText,
                    style = interactionTextStyle,
                    color = secondaryElementColor,
                    maxLines = 1,
                )
                Spacer(Modifier.height(Margin.Medium.value))
                // "•" separator. We should only show it if likes *and* comments text is not empty.
                if (postNumberOfLikesText.isNotBlank() && postNumberOfCommentsText.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = Margin.Small.value
                        ),
                        text = "•",
                        style = interactionTextStyle,
                        color = secondaryElementColor,
                    )
                }
                // Number of comments
                Text(
                    text = postNumberOfCommentsText,
                    style = interactionTextStyle,
                    color = secondaryElementColor,
                    maxLines = 1,
                )
            }
        }

        // Actions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Like action
            TextButton(
                modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = { onPostLikeClick(item) },
                enabled = isLikeButtonEnabled,
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
                    tint = if (isPostLiked) {
                        androidx.compose.material.MaterialTheme.colors.primary
                    } else {
                        secondaryElementColor
                    },
                )
                Text(
                    text = stringResource(R.string.reader_label_like),
                    color = if (isPostLiked) {
                        androidx.compose.material.MaterialTheme.colors.primary
                    } else {
                        secondaryElementColor
                    },
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
fun PostImage(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier
            .size(ReaderTagsFeedComposeUtils.POST_ITEM_IMAGE_SIZE)
            .clip(RoundedCornerShape(corner = CornerSize(8.dp)))
            .clickable { onClick() },
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}

// Post title and excerpt Column
@Composable
fun PostTextContent(
    title: String,
    excerpt: String,
    titleColor: Color,
    excerptColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) {
            maxWidth.toPx().toInt()
        }

        val textMeasurer = rememberTextMeasurer()
        val titleStyle = MaterialTheme.typography.titleMedium

        val excerptMaxLines = remember(title, titleStyle, maxWidthPx) {
            val titleLayoutResult = textMeasurer.measure(
                text = title,
                style = titleStyle,
                maxLines = ReaderTagsFeedComposeUtils.POST_ITEM_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxWidthPx),
            )

            val titleLines = titleLayoutResult.lineCount
            CONTENT_TOTAL_LINES - titleLines
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Margin.Small.value),
        ) {
            // Post title
            Text(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = ReaderTagsFeedComposeUtils.POST_ITEM_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )

            // Post excerpt
            Text(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                text = excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = excerptColor,
                maxLines = excerptMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedPostListItemPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
            ) {
                item {
                    ReaderTagsFeedPostListItem(
                        TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    " pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer " +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer " +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl." +
                                    "Lorem ipsum dolor sit amet consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl." +
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor " +
                                    "sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna" +
                                    "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit" +
                                    "amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna" +
                                    "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet," +
                                    "consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum" +
                                    "posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur" +
                                    "adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere." +
                                    "Vivamus in pretium nisl.",
                            postImageUrl = "https://picsum.photos/200/300",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem" +
                                    "ipsum dolor sit amet, " +
                                    "consectetur adipiscing elit. Integer pellentesque sapien sed urna" +
                                    "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet," +
                                    "consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum" +
                                    "posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur" +
                                    "adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere." +
                                    "Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing" +
                                    "elit. Integer pellentesque sapien sed urna fermentum posuere. Vivamus in" +
                                    "pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem" +
                                    "ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien" +
                                    "sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postImageUrl = "",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet.",
                            postExcerpt = "Lorem ipsum dolor sit amet.",
                            postImageUrl = "https://picsum.photos/200/300",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet.",
                            postExcerpt = "Lorem ipsum dolor sit amet.",
                            postImageUrl = "",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postExcerpt = "Lorem ipsum dolor sit amet.",
                            postImageUrl = "https://picsum.photos/200/300",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                                    "Integer pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postExcerpt = "Lorem ipsum dolor sit amet.",
                            postImageUrl = "",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet.",
                            postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl." +
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor" +
                                    "sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna" +
                                    "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet," +
                                    "consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum" +
                                    "posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur" +
                                    "adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere." +
                                    "Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing" +
                                    "elit. Integer pellentesque sapien sed urna fermentum posuere. Vivamus in" +
                                    "pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                                    "Integer pellentesque sapien sed urna fermentum" +
                                    "posuere. Vivamus in pretium nisl.",
                            postImageUrl = "https://picsum.photos/200/300",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
                item {
                    ReaderTagsFeedPostListItem(
                        item = TagsFeedPostItem(
                            siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postDateLine = "1h",
                            postTitle = "Lorem ipsum dolor sit amet.",
                            postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    "pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl." +
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque" +
                                    "sapien sed urna fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor" +
                                    "sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed urna" +
                                    "fermentum posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet," +
                                    "consectetur adipiscing elit. Integer pellentesque sapien sed urna fermentum" +
                                    "posuere. Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur" +
                                    "adipiscing elit. Integer pellentesque sapien sed urna fermentum posuere." +
                                    "Vivamus in pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing" +
                                    "elit. Integer pellentesque sapien sed urna fermentum posuere. Vivamus in" +
                                    "pretium nisl. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer" +
                                    " pellentesque sapien sed urna fermentum posuere. Vivamus in pretium nisl.",
                            postImageUrl = "",
                            postNumberOfLikesText = "15 likes",
                            postNumberOfCommentsText = "4 comments",
                            isPostLiked = true,
                            isLikeButtonEnabled = true,
                            blogId = 123L,
                            postId = 123L,
                            onSiteClick = {},
                            onPostCardClick = {},
                            onPostLikeClick = {},
                            onPostMoreMenuClick = {},
                        )
                    )
                }
            }
        }
    }
}
