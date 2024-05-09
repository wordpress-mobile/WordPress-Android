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
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.ReaderTestUtils
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedUiStateMapper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
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
    lateinit var navigationEvents: MediatorLiveData<Event<ReaderNavigationEvents>>

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
        )
        whenever(readerPostCardActionsHandler.navigationEvents)
            .thenReturn(navigationEvents)
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
        viewModel.start(listOf(tag))
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
        viewModel.start(listOf(tag))
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
        viewModel.start(listOf(tag1, tag2))
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
        viewModel.start(listOf(tag1, tag2))
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
    fun `Should emit OpenTagPostsFeed when onTagClick is called`() {
        // When
        viewModel.onTagClick(tag)

        // Then
        assertIs<ActionEvent.OpenTagPostsFeed>(actionEvents.first())
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
        mockMapLoadedTagFeedItems()

        // When
        viewModel.start(listOf(tag1, tag2))
        advanceUntilIdle()
        val firstCollectedStates = collectedUiStates.toList()
        Mockito.clearInvocations(readerPostRepository)

        // Then
        viewModel.start(listOf(tag1, tag2))
        advanceUntilIdle()

        assertThat(collectedUiStates).isEqualTo(firstCollectedStates) // still same states, nothing new emitted
        verifyNoInteractions(readerPostRepository)
    }

    @Suppress("LongMethod")
    @Test
    fun `given no tags requested, when start, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tags = emptyList<ReaderTag>()

        // When
        viewModel.start(tags)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).last().isInstanceOf(ReaderTagsFeedViewModel.UiState.Empty::class.java)
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
        viewModel.start(listOf(tag))
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
        viewModel.start(listOf(tag))
        advanceUntilIdle()
        viewModel.onPostLikeClick(tagsFeedPostItem)

        // Then
        verify(postLikeUseCase).perform(any(), any(), any())
    }

    private fun mockMapInitialTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapInitialPostsUiState(any(), any(), any()))
            .thenAnswer {
                val tags = it.getArgument<List<ReaderTag>>(0)
                ReaderTagsFeedViewModel.UiState.Loaded(
                    tags.map { tag -> getInitialTagFeedItem(tag) }
                )
            }
    }

    private fun mockMapLoadingTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapLoadingTagFeedItem(any(), any(), any()))
            .thenAnswer {
                val tag = it.getArgument<ReaderTag>(0)
                ReaderTagsFeedViewModel.TagFeedItem(
                    ReaderTagsFeedViewModel.TagChip(tag, {}),
                    ReaderTagsFeedViewModel.PostList.Loading
                )
            }
    }

    private fun mockMapLoadedTagFeedItems(items: List<TagsFeedPostItem> = emptyList()) {
        whenever(
            readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(any(), any(), any(), any(), any(), any(), any(), any())
        ).thenAnswer {
            getLoadedTagFeedItem(it.getArgument(0), items)
        }
    }

    private fun mockMapErrorTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapErrorTagFeedItem(any(), any(), any(), any(), any()))
            .thenAnswer {
                getErrorTagFeedItem(it.getArgument(0))
            }
    }

    private fun getInitialTagFeedItem(tag: ReaderTag) = ReaderTagsFeedViewModel.TagFeedItem(
        ReaderTagsFeedViewModel.TagChip(tag, {}),
        ReaderTagsFeedViewModel.PostList.Initial
    )

    private fun getLoadedTagFeedItem(tag: ReaderTag, items: List<TagsFeedPostItem> = emptyList()) =
        ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag, {}),
            ReaderTagsFeedViewModel.PostList.Loaded(items)
        )

    private fun getErrorTagFeedItem(tag: ReaderTag) = ReaderTagsFeedViewModel.TagFeedItem(
        ReaderTagsFeedViewModel.TagChip(tag, {}),
        ReaderTagsFeedViewModel.PostList.Error(
            ReaderTagsFeedViewModel.ErrorType.Default, {}
        ),
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
