package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.ReaderTestUtils
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
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
        )
        whenever(readerPostCardActionsHandler.navigationEvents)
            .thenReturn(navigationEvents)
        observeActionEvents()
        observeNavigationEvents()
    }

    @Test
    fun `given valid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.start(listOf(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getLoadedTagFeedItem(tag))
            )
        )
    }

    @Test
    fun `given invalid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val error = ReaderPostFetchException("error")
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            throw error
        }
        mockMapLoadingTagFeedItems()
        mockMapErrorTagFeedItems()

        // When
        viewModel.start(listOf(tag))
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
    fun `given valid tags, when start, then UI state should update properly`() = testCollectingUiStates {
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
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()

        // When
        viewModel.start(listOf(tag1, tag2))
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
    fun `given valid and invalid tags, when fetchAll, then UI state should update properly`() = testCollectingUiStates {
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
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()
        mockMapErrorTagFeedItems()

        // When
        viewModel.start(listOf(tag1, tag2))
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
            "", "", "", "", "", "", "", true, 123L, 123L, {}, {}, {}, {}
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
        mockMapLoadingTagFeedItems()
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

    private fun mockMapLoadingTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapLoadingPostsUiState(any(), any()))
            .thenAnswer {
                val tags = it.getArgument<List<ReaderTag>>(0)
                ReaderTagsFeedViewModel.UiState.Loaded(
                    tags.map { tag ->
                        ReaderTagsFeedViewModel.TagFeedItem(
                            tagChip = ReaderTagsFeedViewModel.TagChip(
                                tag = tag,
                                onTagClick = {},
                            ),
                            postList = ReaderTagsFeedViewModel.PostList.Loading,
                        )
                    }
                )
            }
    }

    private fun mockMapLoadedTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer {
                getLoadedTagFeedItem(it.getArgument(0))
            }
    }

    private fun mockMapErrorTagFeedItems() {
        whenever(readerTagsFeedUiStateMapper.mapErrorTagFeedItem(any(), any(), any(), any()))
            .thenAnswer {
                getErrorTagFeedItem(it.getArgument(0))
            }
    }

    private fun getLoadedTagFeedItem(tag: ReaderTag) = ReaderTagsFeedViewModel.TagFeedItem(
        ReaderTagsFeedViewModel.TagChip(tag, {}),
        ReaderTagsFeedViewModel.PostList.Loaded(listOf())
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
