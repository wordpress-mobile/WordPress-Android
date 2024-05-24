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
import org.wordpress.android.util.UrlUtilsWrapper
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderTagsFeedUiStateMapperTest : BaseUnitTest() {
    private val dateTimeUtilsWrapper = mock<DateTimeUtilsWrapper>()

    private val readerUtilsWrapper = mock<ReaderUtilsWrapper>()

    private val urlUtilsWrapper = mock<UrlUtilsWrapper>()

    private val classToTest: ReaderTagsFeedUiStateMapper = ReaderTagsFeedUiStateMapper(
        dateTimeUtilsWrapper = dateTimeUtilsWrapper,
        readerUtilsWrapper = readerUtilsWrapper,
        urlUtilsWrapper = urlUtilsWrapper,
    )

    @Suppress("LongMethod")
    @Test
    fun `Should map loaded TagFeedItem correctly`() {
        // Given
        val readerPost = ReaderPost().apply {
            blogName = "Name"
            title = "Title"
            excerpt = "Excerpt"
            featuredImage = "url"
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
        val onTagChipClick = { _: ReaderTag -> }
        val onMoreFromTagClick = { _: ReaderTag -> }
        val onSiteClick: (TagsFeedPostItem) -> Unit = {}
        val onPostCardClick: (TagsFeedPostItem) -> Unit = {}
        val onPostLikeClick: (TagsFeedPostItem) -> Unit = {}
        val onPostMoreMenuClick: (TagsFeedPostItem) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}

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
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onSiteClick = onSiteClick,
            onPostCardClick = onPostCardClick,
            onPostLikeClick = onPostLikeClick,
            onPostMoreMenuClick = onPostMoreMenuClick,
            onItemEnteredView = onItemEnteredView,
        )
        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagChipClick = onTagChipClick,
                onMoreFromTagClick = onMoreFromTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Loaded(
                listOf(
                    TagsFeedPostItem(
                        siteName = readerPost.blogName,
                        postDateLine = dateLine,
                        postTitle = readerPost.title,
                        postExcerpt = readerPost.excerpt,
                        postImageUrl = readerPost.featuredImage,
                        postNumberOfLikesText = numberLikesText,
                        postNumberOfCommentsText = numberCommentsText,
                        isPostLiked = readerPost.isLikedByCurrentUser,
                        isLikeButtonEnabled = true,
                        postId = 0L,
                        blogId = 0L,
                        onSiteClick = onSiteClick,
                        onPostLikeClick = onPostLikeClick,
                        onPostCardClick = onPostCardClick,
                        onPostMoreMenuClick = onPostMoreMenuClick,
                    )
                )
            ),
            onItemEnteredView = onItemEnteredView,
        )
        assertEquals(expected, actual)
    }

    @Suppress("LongMethod")
    @Test
    fun `Should map loaded TagFeedItem correctly with blank blog name`() {
        // Given
        val readerPost = ReaderPost().apply {
            blogName = ""
            blogUrl = "https://blogurl.wordpress.com"
            title = "Title"
            excerpt = "Excerpt"
            featuredImage = "url"
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
        val onTagChipClick = { _: ReaderTag -> }
        val onMoreFromTagClick = { _: ReaderTag -> }
        val onSiteClick: (TagsFeedPostItem) -> Unit = {}
        val onPostCardClick: (TagsFeedPostItem) -> Unit = {}
        val onPostLikeClick: (TagsFeedPostItem) -> Unit = {}
        val onPostMoreMenuClick: (TagsFeedPostItem) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}

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
        whenever(urlUtilsWrapper.removeScheme(readerPost.blogUrl))
            .thenReturn("blogurl.wordpress.com")

        val actual = classToTest.mapLoadedTagFeedItem(
            tag = readerTag,
            posts = postList,
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onSiteClick = onSiteClick,
            onPostCardClick = onPostCardClick,
            onPostLikeClick = onPostLikeClick,
            onPostMoreMenuClick = onPostMoreMenuClick,
            onItemEnteredView = onItemEnteredView,
        )
        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagChipClick = onTagChipClick,
                onMoreFromTagClick = onMoreFromTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Loaded(
                listOf(
                    TagsFeedPostItem(
                        siteName = "blogurl.wordpress.com",
                        postDateLine = dateLine,
                        postTitle = readerPost.title,
                        postExcerpt = readerPost.excerpt,
                        postImageUrl = readerPost.featuredImage,
                        postNumberOfLikesText = numberLikesText,
                        postNumberOfCommentsText = numberCommentsText,
                        isPostLiked = readerPost.isLikedByCurrentUser,
                        isLikeButtonEnabled = true,
                        postId = 0L,
                        blogId = 0L,
                        onSiteClick = onSiteClick,
                        onPostLikeClick = onPostLikeClick,
                        onPostCardClick = onPostCardClick,
                        onPostMoreMenuClick = onPostMoreMenuClick,
                    )
                )
            ),
            onItemEnteredView = onItemEnteredView,
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
        val onTagChipClick: (ReaderTag) -> Unit = {}
        val onMoreFromTagClick: (ReaderTag) -> Unit = {}
        val onRetryClick: (ReaderTag) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}
        // When
        val actual = classToTest.mapErrorTagFeedItem(
            tag = readerTag,
            errorType = errorType,
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onRetryClick = onRetryClick,
            onItemEnteredView = onItemEnteredView,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagChipClick = onTagChipClick,
                onMoreFromTagClick = onMoreFromTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Error(
                type = errorType,
                onRetryClick = onRetryClick,
            ),
            onItemEnteredView = onItemEnteredView,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should map loading TagFeedItem correctly`() {
        // Given
        val readerTag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val onTagChipClick: (ReaderTag) -> Unit = {}
        val onMoreFromTagClick: (ReaderTag) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}
        // When
        val actual = classToTest.mapLoadingTagFeedItem(
            tag = readerTag,
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onItemEnteredView = onItemEnteredView,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagChipClick = onTagChipClick,
                onMoreFromTagClick = onMoreFromTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Loading,
            onItemEnteredView = onItemEnteredView,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should map initial TagFeedItem correctly`() {
        // Given
        val readerTag = ReaderTag(
            "tag",
            "tag",
            "tag",
            "endpoint",
            ReaderTagType.FOLLOWED,
        )
        val onTagChipClick: (ReaderTag) -> Unit = {}
        val onMoreFromTagClick: (ReaderTag) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}
        // When
        val actual = classToTest.mapInitialTagFeedItem(
            tag = readerTag,
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onItemEnteredView = onItemEnteredView,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.TagFeedItem(
            tagChip = ReaderTagsFeedViewModel.TagChip(
                tag = readerTag,
                onTagChipClick = onTagChipClick,
                onMoreFromTagClick = onMoreFromTagClick,
            ),
            postList = ReaderTagsFeedViewModel.PostList.Initial,
            onItemEnteredView = onItemEnteredView,
        )
        assertEquals(expected, actual)
    }

    @Suppress("LongMethod")
    @Test
    fun `Should map initial posts UI state correctly`() {
        // Given
        val onTagChipClick: (ReaderTag) -> Unit = {}
        val onMoreFromTagClick: (ReaderTag) -> Unit = {}
        val onItemEnteredView: (ReaderTagsFeedViewModel.TagFeedItem) -> Unit = {}
        val onRefresh: () -> Unit = {}
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
        val announcementItem = ReaderTagsFeedViewModel.ReaderAnnouncementItem(
            items = listOf(mock(), mock()),
            onDoneClicked = {},
        )

        // When
        val actual = classToTest.mapInitialPostsUiState(
            tags = tags,
            announcementItem = announcementItem,
            isRefreshing = true,
            onTagChipClick = onTagChipClick,
            onMoreFromTagClick = onMoreFromTagClick,
            onItemEnteredView = onItemEnteredView,
            onRefresh = onRefresh,
        )

        // Then
        val expected = ReaderTagsFeedViewModel.UiState.Loaded(
            data = listOf(
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag1,
                        onTagChipClick = onTagChipClick,
                        onMoreFromTagClick = onMoreFromTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Initial,
                    onItemEnteredView = onItemEnteredView,
                ),
                ReaderTagsFeedViewModel.TagFeedItem(
                    tagChip = ReaderTagsFeedViewModel.TagChip(
                        tag = tag2,
                        onTagChipClick = onTagChipClick,
                        onMoreFromTagClick = onMoreFromTagClick,
                    ),
                    postList = ReaderTagsFeedViewModel.PostList.Initial,
                    onItemEnteredView = onItemEnteredView,
                )
            ),
            announcementItem = announcementItem,
            isRefreshing = true,
            onRefresh = onRefresh,
        )
        assertEquals(expected, actual)
    }
}
