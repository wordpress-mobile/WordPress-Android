package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.test
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverDataProvider
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData

private const val POST_PARAM_POSITION = 0
private const val ON_ITEM_RENDERED_PARAM_POSITION = 7
private const val NUMBER_OF_ITEMS = 10L

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var readerDiscoverDataProvider: ReaderDiscoverDataProvider
    @Mock private lateinit var uiStateBuilder: ReaderPostUiStateBuilder
    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    private val fakeDiscoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>()
    private val communicationChannel = MutableLiveData<Event<ReaderRepositoryCommunication>>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() = test {
        viewModel = ReaderDiscoverViewModel(
                uiStateBuilder,
                readerPostCardActionsHandler,
                readerDiscoverDataProvider,
                reblogUseCase,
                readerUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(readerDiscoverDataProvider.discoverFeed).thenReturn(fakeDiscoverFeed)
        whenever(
                uiStateBuilder.mapPostToUiState(
                        anyOrNull(), anyInt(), anyInt(), anyOrNull(), anyBoolean(), anyOrNull(),
                        anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
                )
        ).thenAnswer {
            val post = it.getArgument<ReaderPost>(POST_PARAM_POSITION)
            // propagate some of the arguments
            createDummyReaderPostUiState(
                    post,
                    it.getArgument<(ReaderCardUiState) -> Unit>(ON_ITEM_RENDERED_PARAM_POSITION)
            )
        }
        whenever(readerDiscoverDataProvider.communicationChannel).thenReturn(communicationChannel)
        whenever(uiStateBuilder.mapTagListToReaderInterestUiState(anyOrNull(), anyOrNull())).thenReturn(
                createReaderInterestsCardUiState(createReaderTagList())
        )
    }

    @Test
    fun `initial uiState is loading`() {
        // Arrange
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        // Act
        viewModel.start()

        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isEqualTo(LoadingUiState)
    }

    @Test
    fun `uiState updated when discover feed finishes loading`() = test {
        // Arrange
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.start()

        // Act
        fakeDiscoverFeed.value = createDummyReaderCardsList() // mock finished loading

        // Assert
        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates[1]).isInstanceOf(ContentUiState::class.java)
    }

    @Test
    fun `load more action is initiated when we are close to the end of the list`() = test {
        // Arrange
        val closeToEndIndex = NUMBER_OF_ITEMS.toInt() - INITIATE_LOAD_MORE_OFFSET
        init()

        // Act
        ((viewModel.uiState.value as ContentUiState).cards[closeToEndIndex] as ReaderPostUiState).let {
            it.onItemRendered.invoke(it)
        }
        // Assert
        verify(readerDiscoverDataProvider).loadMoreCards()
    }

    @Test
    fun `load more action is NOT initiated when we are at the beginning of the list`() = test {
        // Arrange
        val notCloseToEndIndex = 2
        init()

        // Act

        ((viewModel.uiState.value as ContentUiState).cards[notCloseToEndIndex] as ReaderPostUiState).let {
            it.onItemRendered.invoke(it)
        }
        // Assert
        verify(readerDiscoverDataProvider, never()).loadMoreCards()
    }

    private fun init() {
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.start()
        fakeDiscoverFeed.value = createDummyReaderCardsList()
    }

    // since we are adding an InterestsYouMayLikeCard we remove one item from the numberOfItems since it counts as 1.
    private fun createDummyReaderCardsList(numberOfItems: Long = NUMBER_OF_ITEMS): ReaderDiscoverCards {
        return ReaderDiscoverCards(
                createDummyReaderPostCardList(numberOfItems - 1)
                        .plus(createInterestsYouMayLikeCardList())
        )
    }

    private fun createDummyReaderPostCardList(numberOfItems: Long = NUMBER_OF_ITEMS) =
            (1..numberOfItems).map { ReaderPostCard(createDummyReaderPost(it)) }.toList()

    private fun createDummyReaderPost(id: Long): ReaderPost = ReaderPost().apply {
        this.postId = id
        this.blogId = id
        this.title = "DummyPost"
    }

    private fun createDummyReaderPostUiState(
        post: ReaderPost,
        onItemRendered: (ReaderCardUiState) -> Unit = mock()
    ): ReaderPostUiState {
        return ReaderPostUiState(
                postId = post.postId,
                blogId = post.blogId,
                blogUrl = "",
                dateLine = "",
                avatarOrBlavatarUrl = "",
                blogName = "",
                excerpt = "",
                title = "",
                photoFrameVisibility = false,
                photoTitle = "",
                featuredImageUrl = "",
                featuredImageCornerRadius = mock(),
                thumbnailStripSection = mock(),
                videoOverlayVisibility = false,
                featuredImageVisibility = false,
                moreMenuVisibility = false,
                fullVideoUrl = "",
                discoverSection = mock(),
                bookmarkAction = mock(),
                likeAction = mock(),
                reblogAction = mock(),
                commentsAction = mock(),
                onItemClicked = mock(),
                onItemRendered = onItemRendered,
                onMoreButtonClicked = mock(),
                onVideoOverlayClicked = mock(),
                postHeaderClickData = mock(),
                moreMenuItems = mock()
        )
    }

    private fun createReaderInterestsCardUiState(readerTagList: ReaderTagList) =
            ReaderInterestsCardUiState(readerTagList.map { ReaderInterestUiState("", false, mock()) })

    private fun createReaderTagList(numOfTags: Int = 1) = ReaderTagList().apply {
        for (x in 0 until numOfTags) {
            add(createReaderTag())
        }
    }

    private fun createReaderTag() = ReaderTag(
            "",
            "",
            "",
            null,
            mock(),
            false
    )

    private fun createInterestsYouMayLikeCardList() = listOf(InterestsYouMayLikeCard(createReaderTagList()))
}
