package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.getOrAwaitValue
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderTestUtils
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedUiStateMapper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.viewmodel.Event
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderTagsFeedViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var readerPostRepository: ReaderPostRepository

    @Mock
    lateinit var readerTagsFeedUiStateMapper: ReaderTagsFeedUiStateMapper

    @Mock
    lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler

    @Mock
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    lateinit var postLikeUseCase: PostLikeUseCase

    @Mock
    lateinit var readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder

    @Mock
    lateinit var readerPostUiStateBuilder: ReaderPostUiStateBuilder

    @Mock
    lateinit var displayUtilsWrapper: DisplayUtilsWrapper

    @Mock
    lateinit var readerTracker: ReaderTracker

    @Mock
    lateinit var navigationEvents: MediatorLiveData<Event<ReaderNavigationEvents>>

    @Mock
    lateinit var snackbarEvents: MediatorLiveData<Event<SnackbarMessageHolder>>

    private lateinit var viewModel: ReaderTagsFeedViewModel

    private val collectedUiStates: MutableList<ReaderTagsFeedViewModel.UiState> = mutableListOf()

    private val actionEvents = mutableListOf<ActionEvent>()
    private val readerNavigationEvents = mutableListOf<Event<ReaderNavigationEvents>>()

    val tag = ReaderTag(
        "tag",
        "tag",
        "tag",
        "endpoint",
        ReaderTagType.FOLLOWED,
    )

    @Before
    fun setUp() {
        viewModel = ReaderTagsFeedViewModel(
            bgDispatcher = testDispatcher(),
            readerPostRepository = readerPostRepository,
            readerTagsFeedUiStateMapper = readerTagsFeedUiStateMapper,
            readerPostCardActionsHandler = readerPostCardActionsHandler,
            readerPostTableWrapper = readerPostTableWrapper,
            postLikeUseCase = postLikeUseCase,
            readerPostMoreButtonUiStateBuilder = readerPostMoreButtonUiStateBuilder,
            readerPostUiStateBuilder = readerPostUiStateBuilder,
            displayUtilsWrapper = displayUtilsWrapper,
            readerTracker = readerTracker,
        )
        whenever(readerPostCardActionsHandler.navigationEvents)
            .thenReturn(navigationEvents)
        whenever(readerPostCardActionsHandler.snackbarEvents)
            .thenReturn(snackbarEvents)
        observeActionEvents()
        observeNavigationEvents()
    }

    @Test
    fun `given valid tag, when loaded, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getLoadedTagFeedItem(tag))
            )
        )
    }

    @Test
    fun `given invalid tag, when loaded, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val error = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            throw error
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapErrorTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getErrorTagFeedItem(tag))
            )
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `given valid tags, when loaded, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    getLoadedTagFeedItem(tag1),
                    getLoadedTagFeedItem(tag2)
                ),
            )
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `given valid and invalid tags, when loaded, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val error2 = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            throw error2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()
        mockMapErrorTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    getLoadedTagFeedItem(tag1),
                    getErrorTagFeedItem(tag2),
                )
            )
        )
    }

    @Test
    fun `Should emit FilterTagPostsFeed when onTagChipClick is called`() {
        // When
        viewModel.onTagChipClick(tag)

        // Then
        assertIs<ActionEvent.FilterTagPostsFeed>(actionEvents.first())
    }

    @Test
    fun `Should track READER_TAGS_FEED_HEADER_TAPPED when onTagChipClick is called`() {
        // When
        viewModel.onTagChipClick(tag)

        // Then
        verify(readerTracker).track(AnalyticsTracker.Stat.READER_TAGS_FEED_HEADER_TAPPED)
    }

    @Test
    fun `Should emit OpenTagPostList when onMoreFromTagClick is called`() {
        // When
        viewModel.onMoreFromTagClick(tag)

        // Then
        assertIs<ActionEvent.OpenTagPostList>(actionEvents.first())
    }

    @Test
    fun `Should track READER_TAGS_FEED_MORE_FROM_TAG_TAPPED when onMoreFromTagClick is called`() {
        // When
        viewModel.onMoreFromTagClick(tag)

        // Then
        verify(readerTracker).track(AnalyticsTracker.Stat.READER_TAGS_FEED_MORE_FROM_TAG_TAPPED)
    }


    @Test
    fun `Should emit ShowTagsList when onOpenTagsListClick is called`() {
        // When
        viewModel.onOpenTagsListClick()

        // Then
        assertIs<ActionEvent.ShowTagsList>(actionEvents.first())
    }

    @Test
    fun `Should emit ShowBlogPreview when onSiteClick is called`() = test {
        // Given
        whenever(readerPostTableWrapper.getBlogPost(any(), any(), any()))
            .thenReturn(ReaderPost())

        // When
        viewModel.onSiteClick(TagsFeedPostItem(
            siteName = "",
            postDateLine = "",
            postTitle = "",
            postExcerpt = "",
            postImageUrl = "",
            postNumberOfLikesText = "",
            postNumberOfCommentsText = "",
            isPostLiked = true,
            isLikeButtonEnabled = true,
            postId = 123L,
            blogId = 123L,
            onSiteClick = {},
            onPostCardClick = {},
            onPostLikeClick = {},
            onPostMoreMenuClick = {}
        ))

        // Then
        assertIs<Event<ReaderNavigationEvents.ShowBlogPreview>>(readerNavigationEvents.first())
    }

    @Suppress("LongMethod")
    @Test
    fun `given tags fetched, when start again, then nothing happens`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()
        val firstCollectedStates = collectedUiStates.toList()
        Mockito.clearInvocations(readerPostRepository)

        // Then
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()

        assertThat(collectedUiStates).isEqualTo(firstCollectedStates) // still same states, nothing new emitted
        verifyNoInteractions(readerPostRepository)
    }

    @Suppress("LongMethod")
    @Test
    fun `given tags fetched, when start again refreshing, then move back to initial state`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        viewModel.onRefresh()

        // Then
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()

        val loadedState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.Loaded
        assertThat(loadedState.data).isEqualTo(
            listOf(
                getInitialTagFeedItem(tag1),
                getInitialTagFeedItem(tag2)
            )
        )
        assertThat(loadedState.isRefreshing).isFalse()
    }

    @Suppress("LongMethod")
    @Test
    fun `given no tags requested, when start, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tags = emptyList<ReaderTag>()

        // When
        viewModel.onTagsChanged(tags)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).last().isInstanceOf(ReaderTagsFeedViewModel.UiState.Empty::class.java)
    }

    @Suppress("LongMethod")
    @Test
    fun `given tags fetched, when refreshing, then update isRefreshing status`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        // Then
        viewModel.onRefresh()

        val loadedState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.Loaded
        assertThat(loadedState.isRefreshing).isTrue()
    }

    @Suppress("LongMethod")
    @Test
    fun `given tags fetched, when refreshing, then RefreshTagsFeed action is posted`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag1)).doSuspendableAnswer {
            delay(100)
            posts1
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag2)).doSuspendableAnswer {
            delay(200)
            posts2
        }
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        // Then
        viewModel.onRefresh()

        val action = viewModel.actionEvents.getOrAwaitValue()
        assertThat(action).isEqualTo(ActionEvent.RefreshTags)
    }

    @Test
    fun `Should update UI immediately when like button is tapped`() = testCollectingUiStates {
        // Given
        val tagsFeedPostItem = TagsFeedPostItem(
            siteName = "",
            postDateLine = "",
            postTitle = "",
            postExcerpt = "",
            postImageUrl = "",
            postNumberOfLikesText = "",
            postNumberOfCommentsText = "",
            isPostLiked = false,
            isLikeButtonEnabled = true,
            postId = 123L,
            blogId = 123L,
            onSiteClick = {},
            onPostCardClick = {},
            onPostLikeClick = {},
            onPostMoreMenuClick = {}
        )
        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems(items = listOf(tagsFeedPostItem))
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag))
        advanceUntilIdle()
        viewModel.onPostLikeClick(tagsFeedPostItem)

        // Then
        val latestUiState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.Loaded
        val latestUiStatePostList = (latestUiState.data.first().postList as ReaderTagsFeedViewModel.PostList.Loaded)
        assertThat(latestUiStatePostList.items.first().isPostLiked).isEqualTo(!tagsFeedPostItem.isPostLiked)
    }

    @Test
    fun `Should send update like status request when like button is tapped`() = testCollectingUiStates {
        // Given
        val tagsFeedPostItem = TagsFeedPostItem(
            siteName = "",
            postDateLine = "",
            postTitle = "",
            postExcerpt = "",
            postImageUrl = "",
            postNumberOfLikesText = "",
            postNumberOfCommentsText = "",
            isPostLiked = false,
            isLikeButtonEnabled = true,
            postId = 123L,
            blogId = 123L,
            onSiteClick = {},
            onPostCardClick = {},
            onPostLikeClick = {},
            onPostMoreMenuClick = {}
        )
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems(items = listOf(tagsFeedPostItem))
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }
        whenever(readerPostTableWrapper.getBlogPost(any(), any(), any()))
            .thenReturn(ReaderPost())
        whenever(postLikeUseCase.perform(any(), any(), any()))
            .thenReturn(flowOf())

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()
        viewModel.onPostLikeClick(tagsFeedPostItem)

        // Then
        verify(postLikeUseCase).perform(any(), any(), any())
    }

    @Test
    fun `Should emit RefreshTags when onBackFromTagDetails is called`() {
        // When
        viewModel.onBackFromTagDetails()

        // Then
        assertIs<ActionEvent.RefreshTags>(actionEvents.first())
    }

    @Test
    fun `Should track READER_POST_CARD_TAPPED when onPostCardClick is called`() = testCollectingUiStates {
        // Given
        val blogId = 123L
        val feedId = 456L
        val isFollowedByCurrentUser = true
        whenever(readerPostTableWrapper.getBlogPost(any(), any(), any()))
            .thenReturn(ReaderPost().apply {
                this.blogId = blogId
                this.feedId = feedId
                this.isFollowedByCurrentUser = isFollowedByCurrentUser
            })
        // When
        viewModel.onPostCardClick(
            postItem = TagsFeedPostItem(
                siteName = "",
                postDateLine = "",
                postTitle = "",
                postExcerpt = "",
                postImageUrl = "",
                postNumberOfLikesText = "",
                postNumberOfCommentsText = "",
                isPostLiked = true,
                isLikeButtonEnabled = true,
                postId = 123L,
                blogId = 123L,
                onSiteClick = {},
                onPostCardClick = {},
                onPostLikeClick = {},
                onPostMoreMenuClick = {}
            )
        )

        // Then
        verify(readerTracker).trackBlog(
            stat = AnalyticsTracker.Stat.READER_POST_CARD_TAPPED,
            blogId = blogId,
            feedId = feedId,
            isFollowed = isFollowedByCurrentUser,
            source = ReaderTracker.SOURCE_TAGS_FEED,
        )
    }

    @Test
    fun `should fetch again when onRetryClick is called`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        val error = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            throw error
        }.doSuspendableAnswer {
            delay(100)
            posts
        }

        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()
        mockMapErrorTagFeedItems()

        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag))
        advanceUntilIdle()

        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getErrorTagFeedItem(tag))
            )
        )

        // When
        viewModel.onRetryClick(tag)
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getLoadedTagFeedItem(tag))
            )
        )
    }

    private fun mockMapInitialTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapInitialPostsUiState(any(), any(), any(), any(), any(), any()))
            .thenAnswer {
                val tags = it.getArgument<List<ReaderTag>>(0)
                ReaderTagsFeedViewModel.UiState.Loaded(
                    tags.map { tag -> getInitialTagFeedItem(tag) }
                )
            }
    }

    private fun mockMapLoadingTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapLoadingTagFeedItem(any(), any(), any(), any()))
            .thenAnswer {
                val tag = it.getArgument<ReaderTag>(0)
                ReaderTagsFeedViewModel.TagFeedItem(
                    ReaderTagsFeedViewModel.TagChip(tag, {}, {}),
                    ReaderTagsFeedViewModel.PostList.Loading
                )
            }
    }

    private fun mockMapLoadedTagFeedItems(items: List<TagsFeedPostItem> = emptyList()) {
        whenever(
            readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        ).thenAnswer {
            getLoadedTagFeedItem(it.getArgument(0), items)
        }
    }

    private fun mockMapErrorTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapErrorTagFeedItem(any(), any(), any(), any(), any(), any()))
            .thenAnswer {
                getErrorTagFeedItem(it.getArgument(0))
            }
    }

    private fun getInitialTagFeedItem(tag: ReaderTag) = ReaderTagsFeedViewModel.TagFeedItem(
        ReaderTagsFeedViewModel.TagChip(tag, {}, {}),
        ReaderTagsFeedViewModel.PostList.Initial
    )

    private fun getLoadedTagFeedItem(tag: ReaderTag, items: List<TagsFeedPostItem> = emptyList()) =
        ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag, {}, {}),
            ReaderTagsFeedViewModel.PostList.Loaded(items)
        )

    private fun getErrorTagFeedItem(tag: ReaderTag) = ReaderTagsFeedViewModel.TagFeedItem(
        ReaderTagsFeedViewModel.TagChip(tag, {}, {}),
        ReaderTagsFeedViewModel.PostList.Error(ReaderTagsFeedViewModel.ErrorType.Default, {}),
    )

    private fun testCollectingUiStates(block: suspend TestScope.() -> Unit) = test {
        val collectedUiStatesJob = launch {
            collectedUiStates.clear()
            viewModel.uiStateFlow.toList(collectedUiStates)
        }
        this.block()
        collectedUiStatesJob.cancel()
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.observeForever {
            it?.let { actionEvents.add(it) }
        }
    }

    private fun observeNavigationEvents() {
        viewModel.navigationEvents.observeForever {
            it?.let { readerNavigationEvents.add(it) }
        }
    }
}
