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
    fun mapLoadedTagFeedItem(
        tag: ReaderTag,
        posts: ReaderPostList,
        onTagClick: () -> Unit,
        onSiteClick: () -> Unit,
        onPostImageClick: () -> Unit,
        onPostLikeClick: () -> Unit,
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
                    postNumberOfLikesText = readerUtilsWrapper.getShortLikeLabelText(
                        numLikes = it.numLikes
                    ),
                    postNumberOfCommentsText = readerUtilsWrapper.getShortCommentLabelText(
                        numComments = it.numReplies
                    ),
                    isPostLiked = it.isLikedByCurrentUser,
                    onSiteClick = onSiteClick,
                    onPostImageClick = onPostImageClick,
                    onPostLikeClick = onPostLikeClick,
                    onPostMoreMenuClick = onPostMoreMenuClick,
                )
            }
        ),
    )

    fun mapErrorTagFeedItem(
        tag: ReaderTag,
        errorType: ReaderTagsFeedViewModel.ErrorType,
        onTagClick: () -> Unit,
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
        onTagClick: () -> Unit,
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
