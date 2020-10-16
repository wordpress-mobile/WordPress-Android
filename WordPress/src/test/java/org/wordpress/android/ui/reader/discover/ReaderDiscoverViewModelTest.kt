package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
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
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderDiscoverDataProvider
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState.ReaderBlogSectionClickData
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData

private const val POST_PARAM_POSITION = 0
private const val ON_ITEM_RENDERED_PARAM_POSITION = 7
private const val ON_TAG_CLICKED_PARAM_POSITION = 13
private const val ON_BUTTON_CLICKED_PARAM_POSITION = 5
private const val ON_VIDEO_OVERLAY_CLICKED_PARAM_POSITION = 11
private const val ON_POST_HEADER_CLICKED_PARAM_POSITION = 12
private const val ON_POST_ITEM_CLICKED_PARAM_POSITION = 6
private const val ON_MORE_MENU_CLICKED_PARAM_POSITION = 9
private const val ON_MORE_MENU_DISMISSED_PARAM_POSITION = 10
private const val NUMBER_OF_ITEMS = 10L

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var readerDiscoverDataProvider: ReaderDiscoverDataProvider
    @Mock private lateinit var uiStateBuilder: ReaderPostUiStateBuilder
    @Mock private lateinit var menuUiStateBuilder: ReaderPostMoreButtonUiStateBuilder
    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var displayUtilsWrapper: DisplayUtilsWrapper

    private val fakeDiscoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>()
    private val fakeCommunicationChannel = MutableLiveData<Event<ReaderDiscoverCommunication>>()
    private val fakeNavigationFeed = MutableLiveData<Event<ReaderNavigationEvents>>()
    private val fakeSnackBarFeed = MutableLiveData<Event<SnackbarMessageHolder>>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() = test {
        viewModel = ReaderDiscoverViewModel(
                uiStateBuilder,
                menuUiStateBuilder,
                readerPostCardActionsHandler,
                readerDiscoverDataProvider,
                reblogUseCase,
                readerUtilsWrapper,
                appPrefsWrapper,
                analyticsTrackerWrapper,
                displayUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        whenever(readerDiscoverDataProvider.discoverFeed).thenReturn(fakeDiscoverFeed)
        whenever(readerPostCardActionsHandler.navigationEvents).thenReturn(fakeNavigationFeed)
        whenever(readerPostCardActionsHandler.snackbarEvents).thenReturn(fakeSnackBarFeed)
        whenever(readerUtilsWrapper.getTagFromTagName(anyOrNull(), anyOrNull())).thenReturn(mock())
        whenever(menuUiStateBuilder.buildMoreMenuItems(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(mock())
        whenever(
                uiStateBuilder.mapPostToUiState(
                        anyOrNull(), anyBoolean(), anyInt(), anyInt(), anyOrNull(), anyOrNull(),
                        anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(),
                        anyOrNull(), anyOrNull()
                )
        ).thenAnswer {
            val post = it.getArgument<ReaderPost>(POST_PARAM_POSITION)
            // propagate some of the arguments
            createDummyReaderPostUiState(
                    post,
                    it.getArgument<(ReaderCardUiState) -> Unit>(ON_ITEM_RENDERED_PARAM_POSITION),
                    it.getArgument<(String) -> Unit>(ON_TAG_CLICKED_PARAM_POSITION),
                    it.getArgument<(Long, Long, ReaderPostCardActionType) -> Unit>(ON_BUTTON_CLICKED_PARAM_POSITION),
                    it.getArgument<(Long, Long) -> Unit>(ON_VIDEO_OVERLAY_CLICKED_PARAM_POSITION),
                    it.getArgument<(Long, Long) -> Unit>(ON_POST_HEADER_CLICKED_PARAM_POSITION),
                    it.getArgument<(Long, Long) -> Unit>(ON_POST_ITEM_CLICKED_PARAM_POSITION),
                    it.getArgument<(ReaderPostUiState) -> Unit>(ON_MORE_MENU_CLICKED_PARAM_POSITION),
                    it.getArgument<(ReaderPostUiState) -> Unit>(ON_MORE_MENU_DISMISSED_PARAM_POSITION)
            )
        }
        whenever(readerDiscoverDataProvider.communicationChannel).thenReturn(fakeCommunicationChannel)
        whenever(uiStateBuilder.mapTagListToReaderInterestUiState(anyOrNull(), anyOrNull())).thenReturn(
                createReaderInterestsCardUiState(createReaderTagList())
        )
        whenever(reblogUseCase.onReblogSiteSelected(anyInt(), anyOrNull())).thenReturn(mock())
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull())).thenReturn(mock<OpenEditorForReblog>())
    }

    @Test
    fun `initial uiState is loading`() {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        viewModel.start()
        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isEqualTo(LoadingUiState)
    }

    @Test
    fun `uiState updated when discover feed finishes loading`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
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

    @Test
    fun `if InterestsYouMayLikeCard exist then it will be present in the ContentUIState`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = ReaderDiscoverCards(createInterestsYouMayLikeCardList())
        // Assert
        val contentUiState = uiStates[1] as ContentUiState
        assertThat(contentUiState.cards.first()).isInstanceOf(ReaderInterestsCardUiState::class.java)
    }

    @Test
    fun `if ReaderPostCard exist then ReaderPostUiState will be present in the ContentUIState`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = ReaderDiscoverCards(createDummyReaderPostCardList())
        // Assert
        val contentUiState = uiStates[1] as ContentUiState
        assertThat(contentUiState.cards.first()).isInstanceOf(ReaderPostUiState::class.java)
    }

    @Test
    fun `if welcome card exists with other cards, it is present in ContentUiState`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = ReaderDiscoverCards(
                createWelcomeBannerCard()
                        .plus(createDummyReaderPostCardList())
        )
        // Assert
        val contentUiState = uiStates[1] as ContentUiState
        assertThat(contentUiState.cards.first()).isInstanceOf(ReaderWelcomeBannerCardUiState::class.java)
    }

    @Test
    fun `if welcome card exists as the only card in the feed then ContentUiState is not shown`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = ReaderDiscoverCards(createWelcomeBannerCard())
        // Assert
        assertThat(uiStates.size).isEqualTo(1)
        assertThat(uiStates[0]).isInstanceOf(LoadingUiState::class.java)
    }

    @Test
    fun `WelcomeBannerCard has welcome title set to it`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = ReaderDiscoverCards(createWelcomeBannerCard().plus(createDummyReaderPostCardList()))
        // Assert
        val contentUiState = uiStates[1] as ContentUiState
        val welcomeBannerCardUiState = contentUiState.cards.first() as ReaderWelcomeBannerCardUiState
        assertThat(welcomeBannerCardUiState.titleRes).isEqualTo(R.string.reader_welcome_banner)
    }

    @Test
    fun `Discover data provider is started when the vm is started`() = test {
        // Act
        viewModel.start()
        // Assert
        verify(readerDiscoverDataProvider).start()
    }

    @Test
    fun `PTR progress shown when REQUEST_FIRST_PAGE event starts and a content is displayed`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(Started(REQUEST_FIRST_PAGE)))
        // Assert
        assertThat(uiStates.last().reloadProgressVisibility).isTrue()
    }

    @Test
    fun `Load more progress shown when REQUEST_MORE event starts and a content is displayed`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(Started(REQUEST_MORE)))
        // Assert
        assertThat(uiStates.last().loadMoreProgressVisibility).isTrue()
    }

    @Test
    fun `Fullscreen progress shown when an event starts and a content is not displayed`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(Started(mock())))
        // Assert
        assertThat(uiStates.last().fullscreenProgressVisibility).isTrue()
    }

    @Test
    fun `Fullscreen error is shown on error event when there is no content`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        viewModel.start()
        // Act
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Assert
        assertThat(uiStates.last().fullscreenErrorVisibility).isTrue()
    }

    @Test
    fun `Progress indicators are hidden when an event results in error`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Assert
        assertThat(uiStates.last().fullscreenProgressVisibility).isFalse()
        assertThat(uiStates.last().reloadProgressVisibility).isFalse()
        assertThat(uiStates.last().loadMoreProgressVisibility).isFalse()
    }

    @Test
    fun `Snackbar message is shown when an event results in error`() = test {
        // Arrange
        val msgs = init().snackbarMsgs
        // Act
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Assert
        assertThat(msgs[0].peekContent().message).isEqualTo(UiStringRes(R.string.reader_error_request_failed_title))
    }

    @Test
    fun `When user clicks on a tag, a list of posts for that tag is shown`() = test {
        // Arrange
        val observers = init()
        // Act
        ((observers.uiStates.last() as ContentUiState).cards[1] as ReaderPostUiState).tagItems[0].onClick!!.invoke("t")
        // Assert
        assertThat(observers.navigation[0].peekContent()).isInstanceOf(ShowPostsByTag::class.java)
    }

    @Test
    fun `When user clicks on like button postActionHandler is invoked`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState).likeAction.onClicked!!.invoke(2, 200, LIKE)
        // Assert
        verify(readerPostCardActionsHandler).onAction(
                eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post),
                eq(LIKE),
                eq(false),
                eq(false)
        )
    }

    @Test
    fun `When user clicks on video overlay post action handler is invoked`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState).onVideoOverlayClicked(2, 200)
        // Assert
        verify(readerPostCardActionsHandler).handleVideoOverlayClicked(
                eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post.featuredVideo)
        )
    }

    @Test
    fun `When user clicks on header post action handler is invoked`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState)
                .blogSection.blogSectionClickData!!.onBlogSectionClicked!!.invoke(2, 200)
        // Assert
        verify(readerPostCardActionsHandler).handleHeaderClicked(
                eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post.blogId),
                eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post.feedId)
        )
    }

    @Test
    fun `When user clicks on list item post action handler is invoked`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState)
                .onItemClicked.invoke(2, 200)
        // Assert
        verify(readerPostCardActionsHandler).handleOnItemClicked(
                eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post)
        )
    }

    @Test
    fun `When user clicks on more button menu is shown`() = test {
        // Arrange
        val uiStates = init().uiStates
        val cardUiState = ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState)
        // Act
        cardUiState.onMoreButtonClicked.invoke(cardUiState)
        // Assert
        assertThat(((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState).moreMenuItems).isNotNull
    }

    @Test
    fun `When user dismisses the menu the ui state is updated`() = test {
        // Arrange
        val uiStates = init().uiStates
        var cardUiState = ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState)
        // Act
        cardUiState.onMoreButtonClicked.invoke(cardUiState) // show menu
        cardUiState = ((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState)
        cardUiState.onMoreDismissed.invoke(cardUiState) // dismiss menu
        // Assert
        assertThat(((uiStates.last() as ContentUiState).cards[2] as ReaderPostUiState).moreMenuItems).isNull()
    }

    @Test
    fun `When user picks a site for reblog action the app shows the editor`() {
        // Arrange
        val navigaitonObserver = init().navigation
        fakeNavigationFeed.value = Event(ShowSitePickerForResult(mock(), mock(), mock()))
        // Act
        viewModel.onReblogSiteSelected(1)
        // Assert
        assertThat(navigaitonObserver.last().peekContent()).isInstanceOf(OpenEditorForReblog::class.java)
    }

    @Test
    fun `Data are refreshed when the user swipes down to refresh`() = test {
        // Arrange
        val navigaitonObserver = init()
        // Act
        viewModel.swipeToRefresh()
        // Assert
        verify(readerDiscoverDataProvider).refreshCards()
    }

    @Test
    fun `Data are refreshed when the user clicks on retry`() = test {
        // Arrange
        val navigaitonObserver = init()
        // Act
        viewModel.onRetryButtonClick()
        // Assert
        verify(readerDiscoverDataProvider).refreshCards()
    }

    private fun init(autoUpdateFeed: Boolean = true): Observers {
        val uiStates = mutableListOf<DiscoverUiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val navigation = mutableListOf<Event<ReaderNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigation.add(it)
        }
        val msgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever {
            msgs.add(it)
        }
        viewModel.start()
        if (autoUpdateFeed) {
            fakeDiscoverFeed.value = createDummyReaderCardsList()
        }
        return Observers(uiStates, navigation, msgs)
    }

    // since we are adding an InterestsYouMayLikeCard we remove one item from the numberOfItems since it counts as 1.
    private fun createDummyReaderCardsList(numberOfItems: Long = NUMBER_OF_ITEMS): ReaderDiscoverCards {
        return ReaderDiscoverCards(
                createInterestsYouMayLikeCardList()
                        .plus(createDummyReaderPostCardList(numberOfItems - 1))
        )
    }

    private fun createDummyReaderPostCardList(numberOfItems: Long = NUMBER_OF_ITEMS) =
            (1..numberOfItems).map { ReaderPostCard(createDummyReaderPost(it)) }.toList()

    private fun createDummyReaderPost(id: Long): ReaderPost = ReaderPost().apply {
        this.postId = id
        this.blogId = id * 100
        this.feedId = id * 1000
        this.title = "DummyPost"
        this.featuredVideo = id.toString()
    }

    private fun createDummyReaderPostUiState(
        post: ReaderPost,
        onItemRendered: (ReaderCardUiState) -> Unit = mock(),
        onTagClicked: (String) -> Unit,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        postHeaderClicked: (Long, Long) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onMoreMenuClicked: (ReaderPostUiState) -> Unit,
        onMoreMenuDismissed: (ReaderPostUiState) -> Unit
    ): ReaderPostUiState {
        return ReaderPostUiState(
                postId = post.postId,
                blogId = post.blogId,
                blogSection = ReaderBlogSectionUiState(
                        post.postId, post.blogId, "", "", "", "", ReaderBlogSectionClickData(postHeaderClicked, 0)
                ),
                tagItems = listOf(TagUiState("", "", false, onTagClicked)),
                excerpt = "",
                title = mock(),
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
                expandableTagsViewVisibility = false,
                bookmarkAction = PrimaryAction(true, onClicked = onButtonClicked, type = BOOKMARK),
                likeAction = PrimaryAction(true, onClicked = onButtonClicked, type = LIKE),
                reblogAction = PrimaryAction(true, onClicked = onButtonClicked, type = REBLOG),
                commentsAction = PrimaryAction(true, onClicked = onButtonClicked, type = COMMENTS),
                onItemClicked = onItemClicked,
                onItemRendered = onItemRendered,
                onMoreButtonClicked = onMoreMenuClicked,
                onVideoOverlayClicked = onVideoOverlayClicked,
                moreMenuItems = mock(),
                onMoreDismissed = onMoreMenuDismissed
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
    private fun createWelcomeBannerCard() = listOf(WelcomeBannerCard)

    private data class Observers(
        val uiStates: List<DiscoverUiState>,
        val navigation: List<Event<ReaderNavigationEvents>>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )
}
