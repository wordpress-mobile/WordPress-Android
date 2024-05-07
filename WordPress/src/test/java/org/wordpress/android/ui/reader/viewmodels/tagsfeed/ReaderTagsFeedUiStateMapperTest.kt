package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DateTimeUtilsWrapper
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderTagsFeedUiStateMapperTest : BaseUnitTest() {
    private val dateTimeUtilsWrapper = mock<DateTimeUtilsWrapper>()

    private val readerUtilsWrapper = mock<ReaderUtilsWrapper>()

    private val classToTest: ReaderTagsFeedUiStateMapper = ReaderTagsFeedUiStateMapper(
        dateTimeUtilsWrapper = dateTimeUtilsWrapper,
        readerUtilsWrapper = readerUtilsWrapper,
    )

    @Suppress("LongMethod")
    @Test
    fun `Should map loaded TagFeedItem correctly`() {
        // Given
        val readerPost = ReaderPost().apply {
            blogName = "Name"
            title = "Title"
            excerpt = "Excerpt"
            blogImageUrl = "url"
            numLikes = 5
            numReplies = 10
            isLikedByCurrentUser = true
            datePublished = ""
        }
        val postList = ReaderPostList().apply {
            add(readerPost)
        }
        val readerTag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val onTagClick = { _: ReaderTag -> }
        val onSiteClick: (TagsFeedPostItem) -> Unit = {}
        val onPostCardClick: (TagsFeedPostItem) -> Unit = {}
        val onPostLikeClick = {}
        val onPostMoreMenuClick = {}

        val dateLine = "dateLine"
        val numberLikesText = "numberLikesText"
        val numberCommentsText = "numberCommentsText"

        // When
        whenever(dateTimeUtilsWrapper.dateFromIso8601(any()))
            .thenReturn(Date(0))
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any()))
            .thenReturn(dateLine)
        whenever(readerUtilsWrapper.getShortLikeLabelText(readerPost.numLikes))
            .thenReturn(numberLikesText)
        whenever(readerUtilsWrapper.getShortCommentLabelText(readerPost.numReplies))
            .thenReturn(numberCommentsText)

        val actual = classToTest.mapLoadedTagFeedItem(
            tag = readerTag,
            posts = postList,
            onTagClick = onTagClick,
            onSiteClick = onSiteClick,
            onPostCardClick = onPostCardClick,
            onPostLikeClick = onPostLikeClick,
            onPostMoreMenuClick = onPostMoreMenuClick,
        )
        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagClick = onTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Loaded(
                listOf(
                    TagsFeedPostItem(
                        siteName = readerPost.blogName,
                        postDateLine = dateLine,
                        postTitle = readerPost.title,
                        postExcerpt = readerPost.excerpt,
                        postImageUrl = readerPost.blogImageUrl,
                        postNumberOfLikesText = numberLikesText,
                        postNumberOfCommentsText = numberCommentsText,
                        isPostLiked = readerPost.isLikedByCurrentUser,
                        postId = 0L,
                        blogId = 0L,
                        onSiteClick = onSiteClick,
                        onPostLikeClick = onPostLikeClick,
                        onPostCardClick = onPostCardClick,
                        onPostMoreMenuClick = onPostMoreMenuClick,
                    )
                )
            ),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should map error TagFeedItem correctly`() {
        // Given
        val readerTag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val errorType = ReaderTagsFeedViewModel.ErrorType.Default
        val onTagClick: (ReaderTag) -> Unit = {}
        val onRetryClick = {}
        // When
        val actual = classToTest.mapErrorTagFeedItem(
            tag = readerTag,
            errorType = errorType,
            onTagClick = onTagClick,
            onRetryClick = onRetryClick,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagClick = onTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Error(
                type = errorType,
                onRetryClick = onRetryClick,
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should map loading posts UI state correctly`() {
        // Given
        val onTagClick: (ReaderTag) -> Unit = {}
        val tag1 = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val tag2 = ReaderTag(
            "tag2",
            "tag2",
            "tag2",
            "endpoint2",
            ReaderTagType.FOLLOWED,
        )
        val tags = listOf(tag1, tag2)

        // When
        val actual = classToTest.mapLoadingPostsUiState(
            tags = tags,
            onTagClick = onTagClick,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.UiState.Loaded(
            data = listOf(
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag1,
                        onTagClick = onTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Loading,
                ),
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag2,
                        onTagClick = onTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Loading,
                )
            )
        )
        assertEquals(expected, actual)
    }
}
