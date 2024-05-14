package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import javax.inject.Inject

class ReaderTagsFeedUiStateMapper @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val urlUtilsWrapper: UrlUtilsWrapper,
) {
    @Suppress("LongParameterList")
    fun mapLoadedTagFeedItem(
        tag: ReaderTag,
        posts: ReaderPostList,
        onTagClick: (ReaderTag) -> Unit,
        onSiteClick: (TagsFeedPostItem) -> Unit,
        onPostCardClick: (TagsFeedPostItem) -> Unit,
        onPostLikeClick: (TagsFeedPostItem) -> Unit,
        onPostMoreMenuClick: () -> Unit,
        onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit,
    ) = ReaderTagsFeedViewModel.TagFeedItem(
        tagChip = ReaderTagsFeedViewModel.TagChip(
            tag = tag,
            onTagClick = onTagClick,
        ),
        postList = ReaderTagsFeedViewModel.PostList.Loaded(
            posts.map { post ->
                TagsFeedPostItem(
                    siteName = post.blogName.takeIf { it.isNotBlank() }
                        ?: post.blogUrl.let { urlUtilsWrapper.removeScheme(it) },
                    postDateLine = dateTimeUtilsWrapper.javaDateToTimeSpan(
                        post.getDisplayDate(dateTimeUtilsWrapper)
                    ),
                    postTitle = post.title,
                    postExcerpt = post.excerpt,
                    postImageUrl = post.featuredImage,
                    postNumberOfLikesText = if (post.numLikes > 0) readerUtilsWrapper.getShortLikeLabelText(
                        numLikes = post.numLikes
                    ) else "",
                    postNumberOfCommentsText = if (post.numReplies > 0) readerUtilsWrapper.getShortCommentLabelText(
                        numComments = post.numReplies
                    ) else "",
                    isPostLiked = post.isLikedByCurrentUser,
                    isLikeButtonEnabled = true,
                    postId = post.postId,
                    blogId = post.blogId,
                    onSiteClick = onSiteClick,
                    onPostCardClick = onPostCardClick,
                    onPostLikeClick = onPostLikeClick,
                    onPostMoreMenuClick = onPostMoreMenuClick,
                )
            }
        ),
        onItemEnteredView = onItemEnteredView,
    )

    fun mapErrorTagFeedItem(
        tag: ReaderTag,
        errorType: ReaderTagsFeedViewModel.ErrorType,
        onTagClick: (ReaderTag) -> Unit,
        onRetryClick: () -> Unit,
        onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit,
    ): ReaderTagsFeedViewModel.TagFeedItem =
        ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = tag,
                onTagClick = onTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Error(
                type = errorType,
                onRetryClick = onRetryClick,
            ),
            onItemEnteredView = onItemEnteredView,
        )

    fun mapInitialPostsUiState(
        tags: List<ReaderTag>,
        isRefreshing: Boolean,
        onTagClick: (ReaderTag) -> Unit,
        onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit,
        onRefresh: () -> Unit,
    ): ReaderTagsFeedViewModel.UiState.Loaded =
        ReaderTagsFeedViewModel.UiState.Loaded(
            data = tags.map { tag ->
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag,
                        onTagClick = onTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Initial,
                    onItemEnteredView = onItemEnteredView,
                )
            },
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        )

    fun mapLoadingTagFeedItem(
        tag: ReaderTag,
        onTagClick: (ReaderTag) -> Unit,
        onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit,
    ): ReaderTagsFeedViewModel.TagFeedItem =
        ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = tag,
                onTagClick = onTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Loading,
            onItemEnteredView = onItemEnteredView,
        )
}
