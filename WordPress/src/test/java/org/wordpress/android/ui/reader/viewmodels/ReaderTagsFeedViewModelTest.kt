package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedUiStateMapper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.viewmodel.Event

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

    val tag = ReaderTag(
        "tag",
        "tag",
        "tag",
        "endpoint",
        ReaderTagType.FOLLOWED,
    )

    private val postListLoadingItem = ReaderTagsFeedViewModel.TagFeedItem(
        tagChip = ReaderTagsFeedViewModel.TagChip(
            tag = tag,
            onTagClick = {},
        ),
        postList = ReaderTagsFeedViewModel.PostList.Loading,
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
    }

    @Test
    fun `given valid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tagFeedItem = ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag, {}),
            ReaderTagsFeedViewModel.PostList.Loaded(listOf())
        )
        val posts = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            posts
        }
        whenever(readerTagsFeedUiStateMapper.mapLoadingPostsUiState(any(), any()))
            .thenReturn(
                ReaderTagsFeedViewModel.UiState.Loaded(
                    listOf(postListLoadingItem, postListLoadingItem)
                )
            )
        whenever(readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(tagFeedItem)

        // When
        viewModel.start(listOf(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(tagFeedItem)
            )
        )
    }

    @Test
    fun `given invalid tag, when fetchTag, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val error = ReaderPostFetchException("error")
        val tagFeedItem = ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag, {}),
            ReaderTagsFeedViewModel.PostList.Error(
                ReaderTagsFeedViewModel.ErrorType.Default, {}
            ),
        )
        whenever(readerPostRepository.fetchNewerPostsForTag(tag)).doSuspendableAnswer {
            delay(100)
            throw error
        }
        whenever(readerTagsFeedUiStateMapper.mapLoadingPostsUiState(any(), any()))
            .thenReturn(
                ReaderTagsFeedViewModel.UiState.Loaded(
                    listOf(postListLoadingItem, postListLoadingItem)
                )
            )
        whenever(readerTagsFeedUiStateMapper.mapErrorTagFeedItem(any(), any(), any(), any()))
            .thenReturn(tagFeedItem)

        // When
        viewModel.start(listOf(tag))
//        viewModel.fetchTag(tag)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(tagFeedItem)
            )
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `given valid tags, when fetchAll, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTag(
            "tag1",
            "tag1",
            "tag1",
            "endpoint1",
            ReaderTagType.FOLLOWED,
        )
        val tag2 = ReaderTag(
            "tag2",
            "tag2",
            "tag2",
            "endpoint2",
            ReaderTagType.FOLLOWED,
        )
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
        whenever(readerTagsFeedUiStateMapper.mapLoadingPostsUiState(any(), any()))
            .thenReturn(
                ReaderTagsFeedViewModel.UiState.Loaded(
                    listOf(
                        ReaderTagsFeedViewModel.TagFeedItem(
                            tagChip = ReaderTagsFeedViewModel.TagChip(
                                tag = tag1,
                                onTagClick = {},
                            ),
                            postList = ReaderTagsFeedViewModel.PostList.Loading,
                        ),
                        ReaderTagsFeedViewModel.TagFeedItem(
                            tagChip = ReaderTagsFeedViewModel.TagChip(
                                tag = tag2,
                                onTagClick = {},
                            ),
                            postList = ReaderTagsFeedViewModel.PostList.Loading,
                        ),
                    )
                )
            )
        val tagFeedItem = ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag1, {}),
            ReaderTagsFeedViewModel.PostList.Loaded(listOf()),
        )
        whenever(readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(tagFeedItem)

        // When
        viewModel.start(listOf(tag1, tag2))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    tagFeedItem,
                    tagFeedItem,
                )
            )
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `given valid and invalid tags, when fetchAll, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTag(
            "tag1",
            "tag1",
            "tag1",
            "endpoint1",
            ReaderTagType.FOLLOWED,
        )
        val tag2 = ReaderTag(
            "tag2",
            "tag2",
            "tag2",
            "endpoint2",
            ReaderTagType.FOLLOWED,
        )
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
        whenever(readerTagsFeedUiStateMapper.mapLoadingPostsUiState(any(), any()))
            .thenReturn(
                ReaderTagsFeedViewModel.UiState.Loaded(
                    listOf(
                        ReaderTagsFeedViewModel.TagFeedItem(
                            tagChip = ReaderTagsFeedViewModel.TagChip(
                                tag = tag1,
                                onTagClick = {},
                            ),
                            postList = ReaderTagsFeedViewModel.PostList.Loading,
                        ),
                        ReaderTagsFeedViewModel.TagFeedItem(
                            tagChip = ReaderTagsFeedViewModel.TagChip(
                                tag = tag2,
                                onTagClick = {},
                            ),
                            postList = ReaderTagsFeedViewModel.PostList.Loading,
                        )
                    )
                )
            )
        val tagFeedItemLoaded = ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag1, {}),
            ReaderTagsFeedViewModel.PostList.Loaded(listOf())
        )
        val tagFeedItemError = ReaderTagsFeedViewModel.TagFeedItem(
            ReaderTagsFeedViewModel.TagChip(tag2, {}),
            ReaderTagsFeedViewModel.PostList.Error(
                ReaderTagsFeedViewModel.ErrorType.Default, {}
            )
        )
        whenever(readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(tagFeedItemLoaded)
        whenever(readerTagsFeedUiStateMapper.mapErrorTagFeedItem(any(), any(), any(), any()))
            .thenReturn(tagFeedItemError)

        // When
        viewModel.start(listOf(tag1, tag2))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    tagFeedItemLoaded,
                    tagFeedItemError,
                )
            )
        )
    }

    private fun testCollectingUiStates(block: suspend TestScope.() -> Unit) = test {
        val collectedUiStatesJob = launch {
            collectedUiStates.clear()
            viewModel.uiStateFlow.toList(collectedUiStates)
        }
        this.block()
        collectedUiStatesJob.cancel()
    }
}
