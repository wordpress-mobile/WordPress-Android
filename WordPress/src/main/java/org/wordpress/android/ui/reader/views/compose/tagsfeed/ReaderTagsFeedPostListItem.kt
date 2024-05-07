package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.layout.ContentScale
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
    Column(
        modifier = Modifier
            .width(240.dp)
            .height(340.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Site name
            Text(
                modifier = Modifier
                    .weight(1F)
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
        // Post title
        Text(
            modifier = Modifier
                .padding(top = Margin.Medium.value)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onPostCardClick(item) },
                ),
            text = postTitle,
            style = MaterialTheme.typography.titleMedium,
            color = baseColor,
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
                    predicate = postImageUrl.isBlank(),
                    other = Modifier.height(180.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onPostCardClick(item) },
                ),
            text = postExcerpt,
            style = MaterialTheme.typography.bodySmall,
            color = primaryElementColor,
            maxLines = if (!postImageUrl.isBlank()) 2 else 10,
            overflow = TextOverflow.Ellipsis,
        )
        // Post image
        if (!postImageUrl.isBlank()) {
            PostImage(
                imageUrl = postImageUrl,
                onClick = { onPostCardClick(item) },
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Number of likes
            Text(
                text = postNumberOfLikesText,
                style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryElementColor,
                )
            }
            // Number of comments
            Text(
                text = postNumberOfCommentsText,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(Margin.Medium.value))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
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
            .fillMaxWidth()
            .height(150.dp)
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedPostListItemPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
                    Spacer(Modifier.width(24.dp))
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
