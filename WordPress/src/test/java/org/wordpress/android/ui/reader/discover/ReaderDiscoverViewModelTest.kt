package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderRecommendedBlogsCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.EmptyUiState.RequestFailedUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.EmptyUiState.ShowNoFollowedTagsUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.EmptyUiState.ShowNoPostsUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderSubs
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
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState.ReaderBlogSectionClickData
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ReactiveMutableLiveData

private const val POST_PARAM_POSITION = 1
private const val ON_BUTTON_CLICKED_PARAM_POSITION = 6
private const val ON_POST_ITEM_CLICKED_PARAM_POSITION = 7
private const val ON_ITEM_RENDERED_PARAM_POSITION = 8
private const val ON_MORE_MENU_CLICKED_PARAM_POSITION = 10
private const val ON_MORE_MENU_DISMISSED_PARAM_POSITION = 11
private const val ON_VIDEO_OVERLAY_CLICKED_PARAM_POSITION = 12
private const val ON_POST_HEADER_CLICKED_PARAM_POSITION = 13
private const val ON_TAG_CLICKED_PARAM_POSITION = 14

private const val NUMBER_OF_ITEMS = 10L

private const val RECOMMENDED_BLOG_PARAM_POSITION = 0
private const val ON_RECOMMENDED_BLOG_ITEM_CLICKED_PARAM_POSITION = 1
private const val ON_RECOMMENDED_BLOG_FOLLOW_CLICKED_PARAM_POSITION = 2

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var readerDiscoverDataProvider: ReaderDiscoverDataProvider

    @Mock
    private lateinit var uiStateBuilder: ReaderPostUiStateBuilder

    @Mock
    private lateinit var menuUiStateBuilder: ReaderPostMoreButtonUiStateBuilder

    @Mock
    private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler

    @Mock
    private lateinit var reblogUseCase: ReblogUseCase

    @Mock
    private lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Mock
    private lateinit var getFollowedTagsUseCase: GetFollowedTagsUseCase

    @Mock
    private lateinit var readerTracker: ReaderTracker

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var displayUtilsWrapper: DisplayUtilsWrapper

    @Mock
    private lateinit var parentViewModel: ReaderViewModel

    private val fakeDiscoverFeed = ReactiveMutableLiveData<ReaderDiscoverCards>()
    private val fakeCommunicationChannel = MutableLiveData<Event<ReaderDiscoverCommunication>>()
    private val fakeNavigationFeed = MutableLiveData<Event<ReaderNavigationEvents>>()
    private val fakeSnackBarFeed = MutableLiveData<Event<SnackbarMessageHolder>>()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    @Suppress("LongMethod")
    fun setUp() = test {
        viewModel = ReaderDiscoverViewModel(
            uiStateBuilder,
            menuUiStateBuilder,
            readerPostCardActionsHandler,
            readerDiscoverDataProvider,
            reblogUseCase,
            readerUtilsWrapper,
            appPrefsWrapper,
            readerTracker,
            displayUtilsWrapper,
            getFollowedTagsUseCase,
            testDispatcher(),
            testDispatcher()
        )
        whenever(readerDiscoverDataProvider.discoverFeed).thenReturn(fakeDiscoverFeed)
        whenever(readerPostCardActionsHandler.navigationEvents).thenReturn(fakeNavigationFeed)
        whenever(readerPostCardActionsHandler.snackbarEvents).thenReturn(fakeSnackBarFeed)
        whenever(readerUtilsWrapper.getTagFromTagName(anyOrNull(), anyOrNull())).thenReturn(mock())
        whenever(menuUiStateBuilder.buildMoreMenuItems(anyOrNull(), anyOrNull())).thenReturn(mock())
        whenever(
            uiStateBuilder.mapPostToUiState(
                source = anyString(),
                post = anyOrNull(),
                isDiscover = anyBoolean(),
                photonWidth = anyInt(),
                photonHeight = anyInt(),
                postListType = anyOrNull(),
                onButtonClicked = anyOrNull(),
                onItemClicked = anyOrNull(),
                onItemRendered = anyOrNull(),
                onDiscoverSectionClicked = anyOrNull(),
                onMoreButtonClicked = anyOrNull(),
                onMoreDismissed = anyOrNull(),
                onVideoOverlayClicked = anyOrNull(),
                onPostHeaderViewClicked = anyOrNull(),
                onTagItemClicked = anyOrNull(),
                moreMenuItems = anyOrNull()
            )
        ).thenAnswer {
            createDummyReaderPostUiState(
                it.getArgument(POST_PARAM_POSITION),
                it.getArgument(ON_ITEM_RENDERED_PARAM_POSITION),
                it.getArgument(ON_TAG_CLICKED_PARAM_POSITION),
                it.getArgument(ON_BUTTON_CLICKED_PARAM_POSITION),
                it.getArgument(ON_VIDEO_OVERLAY_CLICKED_PARAM_POSITION),
                it.getArgument(ON_POST_HEADER_CLICKED_PARAM_POSITION),
                it.getArgument(ON_POST_ITEM_CLICKED_PARAM_POSITION),
                it.getArgument(ON_MORE_MENU_CLICKED_PARAM_POSITION),
                it.getArgument(ON_MORE_MENU_DISMISSED_PARAM_POSITION)
            )
        }
        whenever(readerDiscoverDataProvider.communicationChannel).thenReturn(fakeCommunicationChannel)
        whenever(uiStateBuilder.mapTagListToReaderInterestUiState(anyOrNull(), anyOrNull())).thenReturn(
            createReaderInterestsCardUiState(createReaderTagList())
        )
        whenever(
            uiStateBuilder.mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            createReaderRecommendedBlogsCardUiState(
                recommendedBlogs = it.getArgument(RECOMMENDED_BLOG_PARAM_POSITION),
                onItemClicked = it.getArgument(
                    ON_RECOMMENDED_BLOG_ITEM_CLICKED_PARAM_POSITION
                ),
                onFollowClicked = it.getArgument(
                    ON_RECOMMENDED_BLOG_FOLLOW_CLICKED_PARAM_POSITION
                )
            )
        }
        whenever(reblogUseCase.onReblogSiteSelected(anyInt(), anyOrNull())).thenReturn(mock())
        whenever(reblogUseCase.convertReblogStateToNavigationEvent(anyOrNull())).thenReturn(mock<OpenEditorForReblog>())
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList().apply { add(mock()) })
    }

    @Test
    fun `initial uiState is loading`() {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        viewModel.start(parentViewModel)
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
    fun `ShowFollowInterestsEmptyUiState is shown when the user does NOT follow any tags`() = test {
        // Arrange
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList())
        val uiStates = init().uiStates
        // Act
        viewModel.start(parentViewModel)
        // Assert
        assertThat(uiStates.size).isEqualTo(2)
        assertThat(uiStates[1]).isInstanceOf(ShowNoFollowedTagsUiState::class.java)
    }

    @Test
    fun `ShowNoPostsUiState is shown when the discoverFeed does not contain any posts`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeDiscoverFeed.value = createDummyReaderCardsList(numberOfItems = 0)
        // Assert
        assertThat(uiStates[1]).isInstanceOf(ShowNoPostsUiState::class.java)
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
    fun `if ReaderRecommendedBlogsCard exist then ReaderRecommendedBlogsCardUiState will be present`() =
        test {
            // Arrange
            val uiStates = init(autoUpdateFeed = false).uiStates

            // Act
            fakeDiscoverFeed.value = ReaderDiscoverCards(createReaderRecommendedBlogsCardList())

            // Assert
            val contentUiState = uiStates[1] as ContentUiState
            assertThat(contentUiState.cards.first()).isInstanceOf(ReaderRecommendedBlogsCardUiState::class.java)
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
    fun `if welcome card exists then ReaderWelcomeBannerCardUiState will be present`() = test {
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
        viewModel.start(parentViewModel)
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
        assertThat(uiStates.last().reloadProgressVisibility).isTrue
    }

    @Test
    fun `Load more progress shown when REQUEST_MORE event starts and a content is displayed`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(Started(REQUEST_MORE)))
        // Assert
        assertThat(uiStates.last().loadMoreProgressVisibility).isTrue
    }

    @Test
    fun `Fullscreen progress shown when an event starts and a content is not displayed`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(Started(mock())))
        // Assert
        assertThat(uiStates.last().fullscreenProgressVisibility).isTrue
    }

    @Test
    fun `Fullscreen error is shown on error event when there is no content`() = test {
        // Arrange
        val uiStates = init(autoUpdateFeed = false).uiStates
        viewModel.start(parentViewModel)
        // Act
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Assert
        assertThat(uiStates.last()).isInstanceOf(RequestFailedUiState::class.java)
    }

    @Test
    fun `Progress indicators are hidden when an event results in error`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Assert
        assertThat(uiStates.last().fullscreenProgressVisibility).isFalse
        assertThat(uiStates.last().reloadProgressVisibility).isFalse
        assertThat(uiStates.last().loadMoreProgressVisibility).isFalse
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
            anyString()
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
            eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post.feedId),
            eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post.isFollowedByCurrentUser)
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
            eq((fakeDiscoverFeed.value!!.cards[2] as ReaderPostCard).post),
            eq(ReaderTracker.SOURCE_DISCOVER)
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
        val navigationObserver = init().navigation
        fakeNavigationFeed.value = Event(ShowSitePickerForResult(mock(), mock(), mock()))
        // Act
        viewModel.onReblogSiteSelected(1)
        // Assert
        assertThat(navigationObserver.last().peekContent()).isInstanceOf(OpenEditorForReblog::class.java)
    }

    @Test
    fun `When user clicks on recommended blog a blog preview is shown`() {
        // Arrange
        val (uiStates, navigationObserver) = init(autoUpdateFeed = false)
        fakeDiscoverFeed.value = ReaderDiscoverCards(createReaderRecommendedBlogsCardList())

        // Act
        (uiStates.last() as ContentUiState).let {
            (it.cards.first() as ReaderRecommendedBlogsCardUiState).let { card ->
                card.blogs[0].onItemClicked.invoke(1, 0L, false)
            }
        }

        // Assert
        assertThat(navigationObserver.last().peekContent()).isInstanceOf(ShowBlogPreview::class.java)
    }

    @Test
    fun `When user follows recommended blog post action handler is invoked`() = test {
        // Arrange
        val (uiStates) = init(autoUpdateFeed = false)
        fakeDiscoverFeed.value = ReaderDiscoverCards(createReaderRecommendedBlogsCardList())

        // Act
        val blog = (uiStates.last() as ContentUiState).let {
            (it.cards.first() as ReaderRecommendedBlogsCardUiState).let { card ->
                card.blogs[0].apply { onFollowClicked(this) }
            }
        }

        // Assert
        verify(readerPostCardActionsHandler).handleFollowRecommendedSiteClicked(
            eq(blog),
            anyString()
        )
    }

    @Test
    fun `Data are refreshed when the user swipes down to refresh`() = test {
        // Arrange
        init()
        // Act
        viewModel.swipeToRefresh()
        // Assert
        verify(readerDiscoverDataProvider).refreshCards()
    }

    @Test
    fun `Data are refreshed when the user clicks on retry`() = test {
        // Arrange
        init()
        // Act
        viewModel.onRetryButtonClick()
        // Assert
        verify(readerDiscoverDataProvider).refreshCards()
    }

    @Test
    fun `Scroll to top is triggered when discover feed is updated after swipe to refresh`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        viewModel.swipeToRefresh()
        fakeDiscoverFeed.value = createDummyReaderCardsList()
        // Assert
        assertThat(uiStates.last().scrollToTop).isTrue
    }

    @Test
    fun `Scroll to top is not triggered when discover feed is updated after load more action`() = test {
        // Arrange
        val uiStates = init().uiStates
        val closeToEndIndex = NUMBER_OF_ITEMS.toInt() - INITIATE_LOAD_MORE_OFFSET
        init()
        // Act
        ((viewModel.uiState.value as ContentUiState).cards[closeToEndIndex] as ReaderPostUiState).let {
            it.onItemRendered.invoke(it)
        }
        fakeDiscoverFeed.value = createDummyReaderCardsList()
        // Assert
        assertThat(uiStates.last().scrollToTop).isFalse
    }

    @Test
    fun `Action button on no tags empty screen opens reader interests screen`() = test {
        // Arrange
        whenever(getFollowedTagsUseCase.get()).thenReturn(ReaderTagList())
        init()
        fakeDiscoverFeed.value = ReaderDiscoverCards(listOf())
        // Act
        (viewModel.uiState.value as ShowNoFollowedTagsUiState).action.invoke()
        // Assert
        verify(parentViewModel).onShowReaderInterests()
    }

    @Test
    fun `Action button on no posts empty screen opens subs screen`() = test {
        // Arrange
        val navigation = init().navigation
        fakeDiscoverFeed.value = ReaderDiscoverCards(listOf())
        // Act
        (viewModel.uiState.value as ShowNoPostsUiState).action.invoke()
        // Assert
        assertThat(navigation[0].peekContent()).isInstanceOf(ShowReaderSubs::class.java)
    }

    @Test
    fun `Action button on error empty screen invokes refresh cards`() = test {
        // Arrange
        init(autoUpdateFeed = false).uiStates
        viewModel.start(parentViewModel)
        fakeCommunicationChannel.postValue(Event(NetworkUnavailable(mock())))
        // Act
        (viewModel.uiState.value as RequestFailedUiState).action.invoke()
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
        viewModel.start(parentViewModel)
        if (autoUpdateFeed) {
            fakeDiscoverFeed.value = createDummyReaderCardsList()
        }
        return Observers(uiStates, navigation, msgs)
    }

    // since we are adding an InterestsYouMayLikeCard we remove one item from the numberOfItems since it counts as 1.
    private fun createDummyReaderCardsList(numberOfItems: Long = NUMBER_OF_ITEMS): ReaderDiscoverCards {
        return ReaderDiscoverCards(
            if (numberOfItems != 0L) {
                createInterestsYouMayLikeCardList()
                    .plus(createDummyReaderPostCardList(numberOfItems - 1))
            } else {
                listOf()
            }
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

    @Suppress("LongParameterList")
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
            source = "source",
            postId = post.postId,
            blogId = post.blogId,
            feedId = post.feedId,
            isFollowed = post.isFollowedByCurrentUser,
            blogSection = ReaderBlogSectionUiState(
                post.postId,
                post.blogId,
                "",
                mock(),
                "",
                "",
                "",
                false,
                blavatarType = BLAVATAR_CIRCULAR,
                ReaderBlogSectionClickData(postHeaderClicked, 0)
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
        ReaderInterestsCardUiState(readerTagList.map { ReaderInterestUiState("", mock(), mock()) })

    private fun createReaderRecommendedBlogsCardUiState(
        recommendedBlogs: List<ReaderBlog>,
        onItemClicked: (Long, Long, Boolean) -> Unit,
        onFollowClicked: (ReaderRecommendedBlogUiState) -> Unit
    ): ReaderRecommendedBlogsCardUiState {
        return ReaderRecommendedBlogsCardUiState(
            blogs = recommendedBlogs.map {
                ReaderRecommendedBlogUiState(
                    blogId = it.blogId,
                    name = it.name,
                    url = it.url,
                    description = it.description,
                    iconUrl = it.imageUrl,
                    feedId = it.feedId,
                    onItemClicked = onItemClicked,
                    onFollowClicked = onFollowClicked,
                    isFollowed = it.isFollowing
                )
            }
        )
    }

    private fun createReaderTagList(numOfTags: Int = 1) = ReaderTagList().apply {
        (0 until numOfTags).forEach {
            add(createReaderTag())
        }
    }

    private fun createRecommendedBlogsList(numOfBlogs: Int = 1): List<ReaderBlog> {
        return List(numOfBlogs) { createRecommendedBlog() }
    }

    private fun createReaderTag() = ReaderTag(
        "",
        "",
        "",
        null,
        mock(),
        false
    )

    private fun createRecommendedBlog() = ReaderBlog().apply {
        blogId = 1L
        description = "description"
        url = "url"
        name = "name"
        imageUrl = null
        feedId = 0L
        isFollowing = false
    }

    private fun createInterestsYouMayLikeCardList() = listOf(InterestsYouMayLikeCard(createReaderTagList()))
    private fun createWelcomeBannerCard() = listOf(WelcomeBannerCard)
    private fun createReaderRecommendedBlogsCardList(): List<ReaderRecommendedBlogsCard> {
        return listOf(ReaderRecommendedBlogsCard(createRecommendedBlogsList()))
    }

    private data class Observers(
        val uiStates: List<DiscoverUiState>,
        val navigation: List<Event<ReaderNavigationEvents>>,
        val snackbarMsgs: List<Event<SnackbarMessageHolder>>
    )
}
