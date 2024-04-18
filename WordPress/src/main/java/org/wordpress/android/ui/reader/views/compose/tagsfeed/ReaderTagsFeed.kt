package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.background
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
            is UiState.LoadingTagsAndPosts -> LoadingTagsAndPosts()
            is UiState.LoadedTagsLoadingPosts -> LoadedTagsLoadingPosts(uiState)
            is UiState.LoadedTagsAndPosts -> LoadedTagsAndPosts(uiState)
            is UiState.Empty -> Empty()
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
                Spacer(modifier = Modifier.height(Margin.Large.value))
                Box(
                    modifier = Modifier
                        .padding(start = Margin.Large.value)
                        .width(75.dp)
                        .height(36.dp)
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(AppColor.Black.copy(alpha = 0.08F)),
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
private fun LoadedTagsLoadingPosts(uiState: UiState.LoadedTagsLoadingPosts) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = Margin.Large.value,
                end = Margin.Large.value,
            ),
        userScrollEnabled = false,
    ) {
        uiState.items.forEach {
            item {
                Spacer(modifier = Modifier.height(Margin.Large.value))
                ReaderFilterChip(
                    text = UiString.UiStringText(it.tag.tagTitle),
                    onClick = it.onTagClicked,
                    height = 36.dp,
                )
                Spacer(modifier = Modifier.height(Margin.Large.value))
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
        }
    }
}

@Composable
private fun LoadedTagsAndPosts(uiState: UiState.LoadedTagsAndPosts) {
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
            Spacer(modifier = Modifier.height(Margin.Large.value))
            ReaderFilterChip(
                text = UiString.UiStringText(tagsFeedItem.tag.tagTitle),
                onClick = tagsFeedItem.onTagClicked,
                height = 36.dp,
            )
            Spacer(modifier = Modifier.height(Margin.Large.value))
            when (tagsFeedItem) {
                // If item is Success, show posts list
                is TagsFeedItem.Loaded.Success -> {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
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
                is TagsFeedItem.Loaded.Error -> {
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
    ) {
        val numberOfLoadingRows = 3
        repeat(numberOfLoadingRows) {
            item {
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
sealed class TagsFeedItem(
    open val tag: ReaderTag,
    open val onTagClicked: () -> Unit,
) {
    sealed class Loaded(
        override val tag: ReaderTag,
        override val onTagClicked: () -> Unit,
    ) : TagsFeedItem(tag, onTagClicked) {
        data class Success(
            override val tag: ReaderTag,
            override val onTagClicked: () -> Unit,
            val posts: List<TagsFeedPostItem>,
        ) : Loaded(tag, onTagClicked)

        data class Error(
            override val tag: ReaderTag,
            override val onTagClicked: () -> Unit,
        ) : Loaded(tag, onTagClicked)
    }

    data class Loading(
        override val tag: ReaderTag,
        override val onTagClicked: () -> Unit,
    ) : TagsFeedItem(tag, onTagClicked)
}

sealed class UiState {
    object LoadingTagsAndPosts : UiState()

    data class LoadedTagsLoadingPosts(
        val items: List<TagsFeedItem.Loading>,
    ) : UiState()

    data class LoadedTagsAndPosts(
        val items: List<TagsFeedItem.Loaded>,
    ) : UiState()

    object Empty : UiState()
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
fun ReaderTagsFeedLoadedTagsAndPosts() {
    AppTheme {
        ReaderTagsFeed(
            uiState = UiState.LoadedTagsAndPosts(
                items = listOf(
                    TagsFeedItem.Loaded.Success(
                        tag = ReaderTag(
                            "Tag Loaded Success",
                            "Tag Loaded Success",
                            "Tag Loaded Success",
                            "Tag Loaded Success",
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
                    TagsFeedItem.Loaded.Error(
                        tag = ReaderTag(
                            "Tag Loaded Error",
                            "Tag Loaded Error",
                            "Tag Loaded Error",
                            "Tag Loaded Error",
                            ReaderTagType.TAGS,
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
fun ReaderTagsFeedLoadingTagsAndPosts() {
    AppTheme {
        ReaderTagsFeed(
            uiState = UiState.LoadingTagsAndPosts
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedLoadedTagsLoadingPosts() {
    AppTheme {
        ReaderTagsFeed(
            uiState = UiState.LoadedTagsLoadingPosts(
                items = listOf(
                    TagsFeedItem.Loading(
                        tag = ReaderTag(
                            "Tag 1",
                            "Tag 1",
                            "Tag 1",
                            "Tag 1",
                            ReaderTagType.TAGS,
                        ),
                        onTagClicked = {},
                    ),
                    TagsFeedItem.Loading(
                        tag = ReaderTag(
                            "Tag 2",
                            "Tag 2",
                            "Tag 2",
                            "Tag 2",
                            ReaderTagType.TAGS,
                        ),
                        onTagClicked = {},
                    ),
                    TagsFeedItem.Loading(
                        tag = ReaderTag(
                            "Tag 3",
                            "Tag 3",
                            "Tag 3",
                            "Tag 3",
                            ReaderTagType.TAGS,
                        ),
                        onTagClicked = {},
                    )
                )
            )
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
