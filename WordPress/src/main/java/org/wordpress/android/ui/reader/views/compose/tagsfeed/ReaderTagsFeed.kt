package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterChip
import org.wordpress.android.ui.utils.UiString

@Composable
fun ReaderTagsFeed(uiState: UiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        when (uiState) {
            is UiState.Loading -> LoadingTagsAndPosts()
            is UiState.Loaded -> Loaded(uiState)
            is UiState.Empty -> Empty()
        }
    }
}

@Composable
private fun Loaded(uiState: UiState.Loaded) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = Margin.Large.value,
                end = Margin.Large.value,
            )
    ) {
        items(
            items = uiState.data,
        ) { (tagChip, postList) ->
            val backgroundColor = if (isSystemInDarkTheme()) {
                AppColor.White.copy(alpha = 0.12F)
            } else {
                AppColor.Black.copy(alpha = 0.08F)
            }
            Spacer(modifier = Modifier.height(Margin.Large.value))
            with(uiState) {
                // Tag chip UI
                when (tagChip) {
                    is TagChip.Loading -> {
                        Box(
                            modifier = Modifier
                                .padding(start = Margin.Large.value)
                                .width(75.dp)
                                .height(36.dp)
                                .clip(shape = RoundedCornerShape(16.dp))
                                .background(backgroundColor),
                        )
                    }

                    is TagChip.Loaded -> {
                        ReaderFilterChip(
                            text = UiString.UiStringText(tagChip.tag.tagTitle),
                            onClick = tagChip.onTagClicked,
                            height = 36.dp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Margin.Large.value))
                // Posts list UI
                when (postList) {
                    is PostList.Loading -> {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            userScrollEnabled = false,
                        ) {
                            item {
                                ReaderTagsFeedPostListItemLoading()
                                Spacer(Modifier.width(12.dp))
                                ReaderTagsFeedPostListItemLoading()
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }

                    is PostList.Loaded -> {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
                        ) {
                            items(
                                items = postList.items,
                            ) { postItem ->
                                with(postItem) {
                                    ReaderTagsFeedPostListItem(
                                        siteName = siteName,
                                        postDateLine = postDateLine,
                                        postTitle = postTitle,
                                        postExcerpt = postExcerpt,
                                        postImageUrl = postImageUrl,
                                        postNumberOfLikesText = postNumberOfLikesText,
                                        postNumberOfCommentsText = postNumberOfCommentsText,
                                        isPostLiked = isPostLiked,
                                        onPostImageClick = onPostImageClick,
                                        onPostLikeClick = onPostLikeClick,
                                        onPostMoreMenuClick = onPostMoreMenuClick,
                                    )
                                }
                            }
                        }
                    }

                    is PostList.Error -> {
//                        TODO()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingTagsAndPosts() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    userScrollEnabled = false,
                ) {
                    item {
                        ReaderTagsFeedPostListItemLoading()
                        Spacer(Modifier.width(12.dp))
                        ReaderTagsFeedPostListItemLoading()
                        Spacer(Modifier.width(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Empty() {
// TODO empty state (https://github.com/wordpress-mobile/WordPress-Android/issues/20584)
}

// TODO move to VM
sealed class UiState {
    data class Loaded(val data: List<Pair<TagChip, PostList>>) : UiState()

    object Loading : UiState()

    object Empty : UiState()
}

sealed class TagChip {
    data class Loaded(
        val tag: ReaderTag,
        val onTagClicked: () -> Unit,
    ) : TagChip()

    object Loading : TagChip()
}

sealed class PostList {
    data class Loaded(val items: List<TagsFeedPostItem>) : PostList()

    object Loading : PostList()

    object Error : PostList()
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
    val onPostImageClick: () -> Unit,
    val onPostLikeClick: () -> Unit,
    val onPostMoreMenuClick: () -> Unit,
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedLoaded() {
    AppTheme {
        val postListLoaded = PostList.Loaded(
            listOf(
                TagsFeedPostItem(
                    siteName = "siteName1",
                    postDateLine = "postDateLine1",
                    postTitle = "postTitle1",
                    postExcerpt = "postExcerpt1",
                    postImageUrl = "postImageUrl1",
                    postNumberOfLikesText = "postNumberOfLikesText1",
                    postNumberOfCommentsText = "postNumberOfCommentsText1",
                    isPostLiked = true,
                    onPostImageClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "siteName2",
                    postDateLine = "postDateLine2",
                    postTitle = "postTitle2",
                    postExcerpt = "postExcerpt2",
                    postImageUrl = "postImageUrl2",
                    postNumberOfLikesText = "postNumberOfLikesText2",
                    postNumberOfCommentsText = "postNumberOfCommentsText2",
                    isPostLiked = true,
                    onPostImageClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "siteName2",
                    postDateLine = "postDateLine2",
                    postTitle = "postTitle2",
                    postExcerpt = "postExcerpt2",
                    postImageUrl = "postImageUrl2",
                    postNumberOfLikesText = "postNumberOfLikesText2",
                    postNumberOfCommentsText = "postNumberOfCommentsText2",
                    isPostLiked = true,
                    onPostImageClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "siteName3",
                    postDateLine = "postDateLine3",
                    postTitle = "postTitle3",
                    postExcerpt = "postExcerpt3",
                    postImageUrl = "postImageUrl3",
                    postNumberOfLikesText = "postNumberOfLikesText3",
                    postNumberOfCommentsText = "postNumberOfCommentsText3",
                    isPostLiked = true,
                    onPostImageClick = {},
                    onPostLikeClick = {},
                    onPostMoreMenuClick = {},
                ),
                TagsFeedPostItem(
                    siteName = "siteName4",
                    postDateLine = "postDateLine4",
                    postTitle = "postTitle4",
                    postExcerpt = "postExcerpt4",
                    postImageUrl = "postImageUrl4",
                    postNumberOfLikesText = "postNumberOfLikesText4",
                    postNumberOfCommentsText = "postNumberOfCommentsText4",
                    isPostLiked = true,
                    onPostImageClick = {},
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
                    (TagChip.Loaded(readerTag, {}) to postListLoaded),
                    (TagChip.Loaded(readerTag, {}) to PostList.Loading),
                    (TagChip.Loaded(readerTag, {}) to PostList.Error),
                    (TagChip.Loading to PostList.Loading),
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
            uiState = UiState.Empty
        )
    }
}
