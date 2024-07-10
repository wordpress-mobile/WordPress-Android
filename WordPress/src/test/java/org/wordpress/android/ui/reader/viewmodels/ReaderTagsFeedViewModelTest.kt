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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
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
import org.wordpress.android.ui.reader.utils.ReaderAnnouncementHelper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedUiStateMapper
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel
import org.wordpress.android.ui.reader.viewmodels.tagsfeed.ReaderTagsFeedViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.ReaderAnnouncementCardItemData
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import kotlin.test.assertIs

@Suppress("LargeClass")
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

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var readerAnnouncementHelper: ReaderAnnouncementHelper

    private lateinit var viewModel: ReaderTagsFeedViewModel

    private val collectedUiStates: MutableList<ReaderTagsFeedViewModel.UiState> = mutableListOf()

    private val actionEvents = mutableListOf<ActionEvent>()
    private val readerNavigationEvents = mutableListOf<Event<ReaderNavigationEvents>>()
    private val errorMessageEvents = mutableListOf<Event<Int>>()

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
            networkUtilsWrapper = networkUtilsWrapper,
            readerAnnouncementHelper = readerAnnouncementHelper,
        )
        whenever(readerPostCardActionsHandler.navigationEvents)
            .thenReturn(navigationEvents)
        whenever(readerPostCardActionsHandler.snackbarEvents)
            .thenReturn(snackbarEvents)
        observeActionEvents()
        observeNavigationEvents()
        observeErrorMessageEvents()
    }

    @Test
    fun `when tags changed, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        mockMapInitialTagFeedItems()

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).contains(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getInitialTagFeedItem(tag)),
            )
        )
    }

    @Test
    fun `given has announcement, when tags changed, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val announcementItems = listOf<ReaderAnnouncementCardItemData>(mock(), mock())
        mockMapInitialTagFeedItems()
        whenever(readerAnnouncementHelper.hasReaderAnnouncement()).thenReturn(true)
        whenever(readerAnnouncementHelper.getReaderAnnouncementItems()).thenReturn(announcementItems)

        // When
        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()

        // Then
        val loadedState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.Loaded
        assertThat(loadedState.data).isEqualTo(listOf(getInitialTagFeedItem(tag)))
        assertThat(loadedState.announcementItem!!.items).isEqualTo(announcementItems)
    }

    @Test
    fun `given has announcement, when done clicked, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tag = ReaderTestUtils.createTag("tag")
        val announcementItems = listOf<ReaderAnnouncementCardItemData>(mock(), mock())
        mockMapInitialTagFeedItems()
        whenever(readerAnnouncementHelper.hasReaderAnnouncement()).thenReturn(true)
        whenever(readerAnnouncementHelper.getReaderAnnouncementItems()).thenReturn(announcementItems)

        viewModel.onTagsChanged(listOf(tag))
        advanceUntilIdle()

        // When
        val loadedState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.Loaded
        loadedState.announcementItem!!.onDoneClicked()
        advanceUntilIdle()

        // Then
        verify(readerAnnouncementHelper).dismissReaderAnnouncement()
        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(getInitialTagFeedItem(tag)),
            )
        )
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

    @Test
    fun `given tags fetched, when onTagsChanged again, then nothing happens`() = testCollectingUiStates {
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

    @Test
    fun `given new tags fetched, when onTagsChanged again, then state updates`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1") // will be present both times
        val tag2 = ReaderTestUtils.createTag("tag2") // will be present only first time
        val tag3 = ReaderTestUtils.createTag("tag3") // will be present only second time

        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts3 = ReaderPostList().apply {
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
        whenever(readerPostRepository.fetchNewerPostsForTag(tag3)).doSuspendableAnswer {
            delay(300)
            posts3
        }

        mockMapInitialTagFeedItems()
        mockMapLoadingTagFeedItems()
        mockMapLoadedTagFeedItems()
        mockMapInitialTagFeedItem()

        // When
        viewModel.onTagsChanged(listOf(tag1, tag2))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag1))
        advanceUntilIdle()
        viewModel.onItemEnteredView(getInitialTagFeedItem(tag2))
        advanceUntilIdle()

        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    getLoadedTagFeedItem(tag1),
                    getLoadedTagFeedItem(tag2),
                )
            )
        )

        // Then
        viewModel.onTagsChanged(listOf(tag1, tag3))
        advanceUntilIdle()

        assertThat(collectedUiStates.last()).isEqualTo(
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = listOf(
                    getLoadedTagFeedItem(tag1), // still loaded even without entering view
                    getInitialTagFeedItem(tag3),
                )
            )
        )
    }

    @Test
    fun `given no tags, when onTagsChanged, then UI state should update properly`() = testCollectingUiStates {
        // Given
        val tags = emptyList<ReaderTag>()

        // When
        viewModel.onTagsChanged(tags)
        advanceUntilIdle()

        // Then
        assertThat(collectedUiStates).last().isInstanceOf(ReaderTagsFeedViewModel.UiState.Empty::class.java)
    }

    @Test
    fun `given tags fetched, when onTagsChanged again refreshing, then move back to initial state`() =
        testCollectingUiStates {
            // Given
            val tag1 = ReaderTestUtils.createTag("tag1")
            val tag2 = ReaderTestUtils.createTag("tag2")
            val posts1 = ReaderPostList().apply {
                add(ReaderPost())
            }
            val posts2 = ReaderPostList().apply {
                add(ReaderPost())
            }
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
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
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
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
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
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
    fun `given tags fetched and no connection, when refreshing, then show error message`() = testCollectingUiStates {
        // Given
        val tag1 = ReaderTestUtils.createTag("tag1")
        val tag2 = ReaderTestUtils.createTag("tag2")
        val posts1 = ReaderPostList().apply {
            add(ReaderPost())
        }
        val posts2 = ReaderPostList().apply {
            add(ReaderPost())
        }
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
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

        val messageRes = errorMessageEvents.last().peekContent()
        assertThat(messageRes).isEqualTo(R.string.no_network_message)
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
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        // When
        viewModel.onBackFromTagDetails()

        // Then
        assertIs<ActionEvent.RefreshTags>(actionEvents.first())
    }

    @Test
    fun `Should not emit RefreshTags when onBackFromTagDetails is called with no connection`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // When
        viewModel.onBackFromTagDetails()

        // Then
        actionEvents.isEmpty()
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

    @Test
    fun `when calling onViewCreated multiple times, then initialize handler once`() = test {
        // When
        viewModel.onViewCreated()
        viewModel.onViewCreated()

        // Then
        verify(readerPostCardActionsHandler, times(1)).initScope(any())
    }

    @Test
    fun `given connection on, when onViewCreated, then init UI state with Loading`() = testCollectingUiStates {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        // When
        viewModel.onViewCreated()

        // Then
        assertThat(collectedUiStates.last()).isEqualTo(ReaderTagsFeedViewModel.UiState.Loading)
    }

    @Test
    fun `given connection off, when onViewCreated, then init UI state with NoConnection`() = testCollectingUiStates {
        // Given
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        // When
        viewModel.onViewCreated()

        // Then
        assertThat(collectedUiStates.last()).isInstanceOf(ReaderTagsFeedViewModel.UiState.NoConnection::class.java)
    }

    @Test
    fun `given NoConnectionState and connection off, when onRetryClick, then UI state is NoConnection`() =
        testCollectingUiStates {
            // Given
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
            viewModel.onViewCreated()
            val noConnectionState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.NoConnection

            // When
            noConnectionState.onRetryClick()
            advanceUntilIdle()

            // Then
            val lastStates = collectedUiStates.takeLast(2)
            assertThat(lastStates[0]).isEqualTo(ReaderTagsFeedViewModel.UiState.Loading)
            assertThat(lastStates[1]).isInstanceOf(ReaderTagsFeedViewModel.UiState.NoConnection::class.java)
        }

    @Test
    fun `given NoConnectionState and connection on, when onRetryClick, then refresh is requested`() =
        testCollectingUiStates {
            // Given
            whenever(networkUtilsWrapper.isNetworkAvailable())
                .thenReturn(false)
                .thenReturn(true)
            viewModel.onViewCreated()
            val noConnectionState = collectedUiStates.last() as ReaderTagsFeedViewModel.UiState.NoConnection

            // When
            noConnectionState.onRetryClick()
            advanceUntilIdle()

            // Then
            val lastState = collectedUiStates.last()
            assertThat(lastState).isEqualTo(ReaderTagsFeedViewModel.UiState.Loading)
            viewModel.actionEvents.getOrAwaitValue().let {
                assertThat(it).isEqualTo(ActionEvent.RefreshTags)
            }
        }

    private fun mockMapInitialTagFeedItems() {
        whenever(
            readerTagsFeedUiStateMapper.mapInitialPostsUiState(
                any(), anyOrNull(), any(), any(), any(), any(), any()
            )
        ).thenAnswer {
            val tags = it.getArgument<List<ReaderTag>>(0)
            val announcementItem = it.getArgument<ReaderTagsFeedViewModel.ReaderAnnouncementItem?>(1)
            ReaderTagsFeedViewModel.UiState.Loaded(
                data = tags.map { tag -> getInitialTagFeedItem(tag) },
                announcementItem = announcementItem,
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

    private fun mockMapInitialTagFeedItem() {
        whenever(readerTagsFeedUiStateMapper.mapInitialTagFeedItem(any(), any(), any(), any()))
            .thenAnswer {
                getInitialTagFeedItem(it.getArgument(0))
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

    private fun observeErrorMessageEvents() {
        viewModel.errorMessageEvents.observeForever {
            it?.let { errorMessageEvents.add(it) }
        }
    }
}
