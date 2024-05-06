package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

class ReaderTagsFeedUiStateMapper @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
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
    ) = ReaderTagsFeedViewModel.TagFeedItem(
        tagChip = ReaderTagsFeedViewModel.TagChip(
            tag = tag,
            onTagClick = onTagClick,
        ),
        postList = ReaderTagsFeedViewModel.PostList.Loaded(
            posts.map {
                TagsFeedPostItem(
                    siteName = it.blogName,
                    postDateLine = dateTimeUtilsWrapper.javaDateToTimeSpan(
                        it.getDisplayDate(dateTimeUtilsWrapper)
                    ),
                    postTitle = it.title,
                    postExcerpt = it.excerpt,
                    postImageUrl = it.blogImageUrl,
                    postNumberOfLikesText = if (it.numLikes > 0) readerUtilsWrapper.getShortLikeLabelText(
                        numLikes = it.numLikes
                    ) else "",
                    postNumberOfCommentsText = if (it.numReplies > 0) readerUtilsWrapper.getShortCommentLabelText(
                        numComments = it.numReplies
                    ) else "",
                    isPostLiked = it.isLikedByCurrentUser,
                    postId = it.postId,
                    blogId = it.blogId,
                    onSiteClick = onSiteClick,
                    onPostCardClick = onPostCardClick,
                    onPostLikeClick = onPostLikeClick,
                    onPostMoreMenuClick = onPostMoreMenuClick,
                )
            }
        ),
    )

    fun mapErrorTagFeedItem(
        tag: ReaderTag,
        errorType: ReaderTagsFeedViewModel.ErrorType,
        onTagClick: (ReaderTag) -> Unit,
        onRetryClick: () -> Unit,
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
        )

    fun mapLoadingPostsUiState(
        tags: List<ReaderTag>,
        onTagClick: (ReaderTag) -> Unit,
    ): ReaderTagsFeedViewModel.UiState.Loaded =
        ReaderTagsFeedViewModel.UiState.Loaded(
            tags.map { tag ->
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag,
                        onTagClick = onTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Loading,
                )
            }
        )
}
