package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterChip
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiString

@Composable
fun ReaderTagsFeed(uiState: UiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        when (uiState) {
            is UiState.Loaded -> Loaded(uiState)
            UiState.Loading -> Loading()
            UiState.Empty -> Empty()
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
            items = uiState.items,
            key = { it.tag.tagDisplayName },
        ) { tagsFeedItem ->
            ReaderFilterChip(
                text = UiString.UiStringText(tagsFeedItem.tag.tagTitle),
                onClick = tagsFeedItem.onTagClicked,
                height = 36.dp,
            )
            when (tagsFeedItem) {
                // If item is Success, show posts list
                is TagsFeedItem.Success -> {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        items(
                            items = tagsFeedItem.posts,
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
                // If item is Error, show error UI and retry button
                is TagsFeedItem.Error -> {
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
    }
}

// TODO empty state (https://github.com/wordpress-mobile/WordPress-Android/issues/20584)
@Composable
private fun Empty() {
}


// TODO move to VM
sealed class UiState {
    // TODO review Loaded parameters
    data class Loaded(
        val items: List<TagsFeedItem>,
    ) : UiState()

    object Loading : UiState()

    object Empty : UiState()
}

sealed class TagsFeedItem(
    open val tag: ReaderTag,
    open val onTagClicked: () -> Unit,
) {
    data class Success(
        override val tag: ReaderTag,
        override val onTagClicked: () -> Unit,
        val posts: List<TagsFeedPostItem>,
    ) : TagsFeedItem(tag, onTagClicked)

    data class Error(
        override val tag: ReaderTag,
        override val onTagClicked: () -> Unit,
    ) : TagsFeedItem(tag, onTagClicked)
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
        ReaderTagsFeed(
            uiState = UiState.Loaded(
                items = listOf(
                    TagsFeedItem.Success(
                        tag = ReaderTag(
                            "Tag 1",
                            "Tag 1",
                            "Tag 1",
                            "Tag 1",
                            ReaderTagType.TAGS,
                        ),
                        posts = listOf(
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
                        ),
                        onTagClicked = {},
                    ),
                    TagsFeedItem.Success(
                        tag = ReaderTag(
                            "Tag 2",
                            "Tag 2",
                            "Tag 2",
                            "Tag 2",
                            ReaderTagType.TAGS,
                        ),
                        posts = listOf(
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
                        ),
                        onTagClicked = {},
                    ),
                    TagsFeedItem.Error(
                        tag = ReaderTag(
                            "Tag 3",
                            "Tag 3",
                            "Tag 3",
                            "Tag 3",
                            ReaderTagType.TAGS,
                        ),
                        onTagClicked = {},
                    ),
                    TagsFeedItem.Success(
                        tag = ReaderTag(
                            "Tag 4",
                            "Tag 4",
                            "Tag 4",
                            "Tag 4",
                            ReaderTagType.TAGS,
                        ),
                        posts = listOf(
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
                        ),
                        onTagClicked = {},
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
            uiState = UiState.Empty
        )
    }
}
