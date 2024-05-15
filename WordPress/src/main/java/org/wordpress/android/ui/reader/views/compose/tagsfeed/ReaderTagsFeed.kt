package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ErrorType
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.PostList
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.TagChip
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.TagFeedItem
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.UiState
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterChip
import org.wordpress.android.ui.utils.UiString

@Composable
fun ReaderTagsFeed(uiState: UiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 48.dp),
    ) {
        when (uiState) {
            is UiState.Loading -> Loading()
            is UiState.Loaded -> Loaded(uiState)
            is UiState.Empty -> Empty(uiState)
            is UiState.Initial -> {
                // no-op
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Loaded(uiState: UiState.Loaded) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = {
            uiState.onRefresh()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state = pullRefreshState),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            items(
                items = uiState.data,
            ) { item ->
                val tagChip = item.tagChip
                val postList = item.postList

                LaunchedEffect(item.postList) {
                    item.onEnteredView()
                }

                val backgroundColor = if (isSystemInDarkTheme()) {
                    AppColor.White.copy(alpha = 0.12F)
                } else {
                    AppColor.Black.copy(alpha = 0.08F)
                }
                Spacer(modifier = Modifier.height(Margin.Large.value))
                // Tag chip UI
                ReaderFilterChip(
                    modifier = Modifier.padding(
                        start = Margin.Large.value,
                    ),
                    text = UiString.UiStringText(tagChip.tag.tagTitle),
                    onClick = { tagChip.onTagChipClick(tagChip.tag) },
                    height = 36.dp,
                )
                Spacer(modifier = Modifier.height(Margin.Large.value))
                // Posts list UI
                when (postList) {
                    is PostList.Initial, is PostList.Loading -> PostListLoading()
                    is PostList.Loaded -> PostListLoaded(postList, tagChip, backgroundColor)
                    is PostList.Error -> PostListError(backgroundColor, tagChip, postList)
                }
                Spacer(modifier = Modifier.height(Margin.ExtraExtraMediumLarge.value))
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun Loading() {
    val fetchingPostsLabel = stringResource(id = R.string.posts_fetching)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics {
                contentDescription = fetchingPostsLabel
            },
        userScrollEnabled = false,
    ) {
        val numberOfLoadingRows = 3
        repeat(numberOfLoadingRows) {
            item {
                val backgroundColor = if (isSystemInDarkTheme()) {
                    AppColor.White.copy(alpha = 0.12F)
                } else {
                    AppColor.Black.copy(alpha = 0.08F)
                }
                Spacer(modifier = Modifier.height(Margin.Large.value))
                Box(
                    modifier = Modifier
                        .padding(start = Margin.Large.value)
                        .width(75.dp)
                        .height(36.dp)
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(backgroundColor),
                )

                Spacer(modifier = Modifier.height(Margin.Large.value))
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(Margin.Large.value),
                    contentPadding = PaddingValues(horizontal = Margin.Large.value),
                ) {
                    items(ReaderTagsFeedComposeUtils.LOADING_POSTS_COUNT) {
                        ReaderTagsFeedPostListItemLoading()
                    }
                }
            }
        }
    }
}

@Composable
private fun Empty(uiState: UiState.Empty) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            modifier = Modifier
                .padding(
                    start = Margin.ExtraExtraMediumLarge.value,
                    end = Margin.ExtraExtraMediumLarge.value,
                    bottom = Margin.Medium.value,
                ),
            text = stringResource(id = R.string.reader_discover_empty_title),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium,
            ),
        )
        // Subtitle
        Text(
            modifier = Modifier
                .padding(
                    start = Margin.ExtraExtraMediumLarge.value,
                    end = Margin.ExtraExtraMediumLarge.value,
                    bottom = Margin.Large.value,
                ),
            text = stringResource(id = R.string.reader_discover_empty_subtitle_follow),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium,
            ),
        )
        // Button
        Button(
            onClick = uiState.onOpenTagsListClick,
            modifier = Modifier.padding(
                start = Margin.ExtraMediumLarge.value,
                end = Margin.ExtraMediumLarge.value,
                bottom = Margin.ExtraLarge.value,
            ),
            contentPadding = PaddingValues(
                horizontal = 32.dp,
                vertical = 8.dp,
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onPrimary,
                backgroundColor = MaterialTheme.colors.onSurface,
            ),
        ) {
            androidx.compose.material.Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                text = stringResource(id = R.string.reader_discover_empty_button_text),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PostListLoading() {
    val loadingLabel = stringResource(id = R.string.loading)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = loadingLabel
            },
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
        contentPadding = PaddingValues(
            start = Margin.Large.value,
            end = Margin.Large.value
        ),
    ) {
        items(ReaderTagsFeedComposeUtils.LOADING_POSTS_COUNT) {
            ReaderTagsFeedPostListItemLoading()
        }
    }
}

@Composable
private fun PostListLoaded(
    postList: PostList.Loaded,
    tagChip: TagChip,
    backgroundColor: Color
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
        contentPadding = PaddingValues(
            start = Margin.Large.value,
            end = Margin.Large.value
        ),
    ) {
        items(
            items = postList.items,
        ) { postItem ->
            ReaderTagsFeedPostListItem(
                item = postItem
            )
        }
        item {
            val baseColor = if (isSystemInDarkTheme()) AppColor.White else AppColor.Black
            val primaryElementColor = baseColor.copy(
                alpha = 0.87F
            )
            Box(
                modifier = Modifier
                    .height(ReaderTagsFeedComposeUtils.PostItemHeight)
                    .padding(
                        start = Margin.ExtraLarge.value,
                        end = Margin.ExtraLarge.value,
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = {
                                tagChip.onMoreFromTagClick(tagChip.tag)
                            }
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        modifier = Modifier
                            .drawBehind {
                                drawCircle(
                                    color = backgroundColor,
                                    radius = this.size.maxDimension
                                )
                            },
                        painter = painterResource(R.drawable.ic_arrow_right_white_24dp),
                        tint = MaterialTheme.colors.onSurface,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.height(Margin.ExtraMediumLarge.value))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = stringResource(
                            id = R.string.reader_tags_feed_see_more_from_tag,
                            tagChip.tag.tagDisplayName
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = primaryElementColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostListError(
    backgroundColor: Color,
    tagChip: TagChip,
    postList: PostList.Error,
) {
    Column(
        modifier = Modifier
            .height(250.dp)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(start = 60.dp, end = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Margin.ExtraLarge.value))
        Icon(
            modifier = Modifier
                .drawBehind {
                    drawCircle(
                        color = backgroundColor,
                        radius = this.size.maxDimension
                    )
                },
            painter = painterResource(R.drawable.ic_wifi_off_24px),
            tint = MaterialTheme.colors.onSurface,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(Margin.ExtraExtraMediumLarge.value))
        val tagName = tagChip.tag.tagDisplayName
        Text(
            text = stringResource(id = R.string.reader_tags_feed_error_title, tagName),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Margin.Medium.value))
        val errorMessage = when (postList.type) {
            is ErrorType.Default -> stringResource(R.string.reader_tags_feed_loading_error_description)
            is ErrorType.NoContent -> stringResource(R.string.reader_tags_feed_no_content_error_description, tagName)
        }
        Text(
            text = errorMessage,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = if (isSystemInDarkTheme()) {
                AppColor.White.copy(alpha = 0.4F)
            } else {
                AppColor.Black.copy(alpha = 0.4F)
            },
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Margin.ExtraLarge.value))
        Button(
            onClick = { postList.onRetryClick() },
            modifier = Modifier
                .height(36.dp)
                .width(114.dp),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onPrimary,
                backgroundColor = MaterialTheme.colors.onSurface,
            ),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.surface,
                text = stringResource(R.string.reader_tags_feed_error_retry),
            )
        }
    }
}

data class TagsFeedPostItem(
    val siteName: String,
    val postDateLine: String,
    val postTitle: String,
    val postExcerpt: String,
    val postImageUrl: String,
    val postNumberOfLikesText: String,
    val postNumberOfCommentsText: String,
    val isPostLiked: Boolean,
    val isLikeButtonEnabled: Boolean,
    val postId: Long,
    val blogId: Long,
    val onSiteClick: (TagsFeedPostItem) -> Unit,
    val onPostCardClick: (TagsFeedPostItem) -> Unit,
    val onPostLikeClick: (TagsFeedPostItem) -> Unit,
    val onPostMoreMenuClick: (TagsFeedPostItem) -> Unit,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedLoaded() {
    AppTheme {
        val postListLoaded = PostList.Loaded(
            listOf(
                TagsFeedPostItem(
                    siteName = "Site Name 1",
                    postDateLine = "1h",
                    postTitle = "Post Title 1",
                    postExcerpt = "Post excerpt 1",
                    postImageUrl = "postImageUrl1",
                    postNumberOfLikesText = "15 likes",
                    postNumberOfCommentsText = "",
                    isPostLiked = true,
                    isLikeButtonEnabled = true,
                    postId = 123L,
                    blogId = 123L,
                    onSiteClick = {},
                    onPostCardClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "Site Name 2",
                    postDateLine = "2h",
                    postTitle = "Post Title 2",
                    postExcerpt = "Post excerpt 2",
                    postImageUrl = "postImageUrl2",
                    postNumberOfLikesText = "",
                    postNumberOfCommentsText = "3 comments",
                    isPostLiked = true,
                    isLikeButtonEnabled = true,
                    postId = 456L,
                    blogId = 456L,
                    onSiteClick = {},
                    onPostCardClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "Site Name 3",
                    postDateLine = "3h",
                    postTitle = "Post Title 3",
                    postExcerpt = "Post excerpt 3",
                    postImageUrl = "postImageUrl3",
                    postNumberOfLikesText = "123 likes",
                    postNumberOfCommentsText = "9 comments",
                    isPostLiked = true,
                    isLikeButtonEnabled = true,
                    postId = 789L,
                    blogId = 789L,
                    onSiteClick = {},
                    onPostCardClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "Site Name 4",
                    postDateLine = "4h",
                    postTitle = "Post Title 4",
                    postExcerpt = "Post excerpt 4",
                    postImageUrl = "postImageUrl4",
                    postNumberOfLikesText = "1234 likes",
                    postNumberOfCommentsText = "91 comments",
                    isPostLiked = true,
                    isLikeButtonEnabled = true,
                    postId = 1234L,
                    blogId = 1234L,
                    onSiteClick = {},
                    onPostCardClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "Site Name 5",
                    postDateLine = "5h",
                    postTitle = "Post Title 5",
                    postExcerpt = "Post excerpt 5",
                    postImageUrl = "postImageUrl5",
                    postNumberOfLikesText = "12 likes",
                    postNumberOfCommentsText = "34 comments",
                    isPostLiked = true,
                    isLikeButtonEnabled = true,
                    postId = 5678L,
                    blogId = 5678L,
                    onSiteClick = {},
                    onPostCardClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
            )
        )
        val readerTag = ReaderTag(
            "Tag 1",
            "Tag 1",
            "Tag 1",
            "Tag 1",
            ReaderTagType.TAGS,
        )
        ReaderTagsFeed(
            uiState = UiState.Loaded(
                data = listOf(
                    TagFeedItem(
                        tagChip = TagChip(readerTag, {}, {}),
                        postList = postListLoaded
                    ),
                    TagFeedItem(
                        tagChip = TagChip(readerTag, {}, {}),
                        postList = PostList.Initial,
                    ),
                    TagFeedItem(
                        tagChip = TagChip(readerTag, {}, {}),
                        postList = PostList.Error(ErrorType.Default, {}),
                    ),
                    TagFeedItem(
                        tagChip = TagChip(readerTag, {}, {}),
                        postList = PostList.Error(ErrorType.NoContent, {}),
                    ),
                )
            )
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedLoading() {
    AppTheme {
        ReaderTagsFeed(
            uiState = UiState.Loading
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedEmpty() {
    AppTheme {
        ReaderTagsFeed(
            uiState = UiState.Empty(
                onOpenTagsListClick = {},
            )
        )
    }
}
