package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverDataProvider
import org.wordpress.android.ui.reader.usecases.PreLoadPostContent
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val INITIATE_LOAD_MORE_OFFSET = 3

class ReaderDiscoverViewModel @Inject constructor(
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val readerDiscoverDataProvider: ReaderDiscoverDataProvider,
    private val reblogUseCase: ReblogUseCase,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents = _preloadPostEvents

    /**
     * Post which is about to be reblogged after the user selects a target site.
     */
    private var pendingReblogPost: ReaderPost? = null

    /* TODO malinjir calculate photon dimensions - check if DisplayUtils.getDisplayPixelWidth
        returns result based on device orientation */
    private val photonWidth: Int = 500
    private val photonHeight: Int = 500

    fun start() {
        if (isStarted) return
        isStarted = true

        init()
    }

    private fun init() {
        // Start with loading state
        _uiState.value = LoadingUiState

        // Get the correct repository
        readerDiscoverDataProvider.start()

        // Listen to changes to the discover feed
        _uiState.addSource(readerDiscoverDataProvider.discoverFeed) { posts ->
            val discoverFeedContainsOnlyWelcomeCard = posts.cards.size == 1 &&
                    posts.cards.filterIsInstance<WelcomeBannerCard>().isNotEmpty()

            if (!discoverFeedContainsOnlyWelcomeCard) {
                _uiState.value = ContentUiState(
                        posts.cards.map {
                            when (it) {
                                is WelcomeBannerCard -> ReaderWelcomeBannerCardUiState(
                                        titleRes = R.string.reader_welcome_banner
                                )
                                is ReaderPostCard -> postUiStateBuilder.mapPostToUiState(
                                        post = it.post,
                                        isDiscover = true,
                                        photonWidth = photonWidth,
                                        photonHeight = photonHeight,
                                        isBookmarkList = false,
                                        onButtonClicked = this::onButtonClicked,
                                        onItemClicked = this::onPostItemClicked,
                                        onItemRendered = this::onItemRendered,
                                        onDiscoverSectionClicked = this::onDiscoverClicked,
                                        onMoreButtonClicked = this::onMoreButtonClicked,
                                        onMoreDismissed = this::onMoreMenuDismissed,
                                        onVideoOverlayClicked = this::onVideoOverlayClicked,
                                        onPostHeaderViewClicked = this::onPostHeaderClicked,
                                        onTagItemClicked = this::onTagItemClicked,
                                        postListType = TAG_FOLLOWED
                                )
                                is InterestsYouMayLikeCard -> {
                                    postUiStateBuilder.mapTagListToReaderInterestUiState(
                                            it.interests,
                                            this::onReaderTagClicked
                                    )
                                }
                            }
                        },
                        swipeToRefreshIsRefreshing = false
                )
            }
        }

        readerDiscoverDataProvider.communicationChannel.observeForever { data ->
            data?.let {
                // TODO listen for communications from the reeaderPostRepository, but not 4ever!
            }
        }

        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            val target = event.peekContent()
            if (target is ShowSitePickerForResult) {
                pendingReblogPost = target.post
            }
            _navigationEvents.value = event
        }

        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _preloadPostEvents.addSource(readerPostCardActionsHandler.preloadPostEvents) { event ->
            _preloadPostEvents.value = event
        }
    }

    private fun onReaderTagClicked(tag: String) {
        val readerTag = readerUtilsWrapper.getTagFromTagName(tag, FOLLOWED)
        _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
    }

    private fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(it, type, isBookmarkList = false)
            }
        }
    }

    private fun onVideoOverlayClicked(postId: Long, blogId: Long) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.handleVideoOverlayClicked(it.featuredVideo)
            }
        }
    }

    private fun onPostHeaderClicked(postId: Long, blogId: Long) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.handleHeaderClicked(it.blogId, it.feedId)
            }
        }
    }

    private fun onTagItemClicked(tagSlug: String) {
        val readerTag = readerUtilsWrapper.getTagFromTagName(tagSlug, FOLLOWED)
        _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
    }

    private fun onPostItemClicked(postId: Long, blogId: Long) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.handleOnItemClicked(it)
            }
        }
    }

    private fun onItemRendered(itemUiState: ReaderCardUiState) {
        initiateLoadMoreIfNecessary(itemUiState)
    }

    private fun findPost(postId: Long, blogId: Long): ReaderPost? {
        return readerDiscoverDataProvider.discoverFeed.value?.cards?.let {
            it.filterIsInstance<ReaderPostCard>()
                    .find { card -> card.post.postId == postId && card.post.blogId == blogId }
                    ?.post
        }
    }

    private fun initiateLoadMoreIfNecessary(item: ReaderCardUiState) {
        (uiState.value as? ContentUiState)?.cards?.let {
            val closeToEndIndex = it.size - INITIATE_LOAD_MORE_OFFSET
            if (closeToEndIndex > 0) {
                val isCardCloseToEnd: Boolean = it.getOrNull(closeToEndIndex) == item
                // TODO malinjir we might want to show some kind of progress indicator when the request is in progress
                if (isCardCloseToEnd) launch(bgDispatcher) { readerDiscoverDataProvider.loadMoreCards() }
            }
        }
    }

    private fun onDiscoverClicked(postId: Long, blogId: Long) {
        AppLog.d(T.READER, "OnDiscoverClicked")
    }

    // TODO malinjir get rid of the view reference
    private fun onMoreButtonClicked(postUiState: ReaderPostUiState) {
        AppLog.d(T.READER, "OnMoreButtonClicked")
        changeMoreMenuVisibility(postUiState, true)
    }

    private fun onMoreMenuDismissed(postUiState: ReaderPostUiState) {
        changeMoreMenuVisibility(postUiState, false)
    }

    private fun changeMoreMenuVisibility(currentUiState: ReaderPostUiState, show: Boolean) {
        findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
            val updatedUiState = currentUiState.copy(
                    moreMenuItems = if (show) readerPostMoreButtonUiStateBuilder.buildMoreMenuItems(
                            post,
                            TAG_FOLLOWED,
                            this::onButtonClicked
                    )
                    else null
            )

            replaceUiStateItem(currentUiState, updatedUiState)
        }
    }

    private fun replaceUiStateItem(before: ReaderPostUiState, after: ReaderPostUiState) {
        (_uiState.value as? ContentUiState)?.let {
            val updatedList = it.cards.toMutableList()
            val index = it.cards.indexOf(before)
            if (index != -1) {
                updatedList[index] = after
                _uiState.value = it.copy(cards = updatedList)
            }
        }
    }

    fun onReblogSiteSelected(siteLocalId: Int) {
        // TODO malinjir almost identical to ReaderPostCardActionsHandler.handleReblogClicked.
        //  Consider refactoring when ReaderPostCardActionType is transformed into a sealed class.
        val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
        val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
        if (navigationTarget != null) {
            _navigationEvents.postValue(Event(navigationTarget))
        } else {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.reader_reblog_error))))
        }
        pendingReblogPost = null
    }

    override fun onCleared() {
        super.onCleared()
        readerDiscoverDataProvider.stop()
        readerPostCardActionsHandler.onCleared()

        appPrefsWrapper.readerDiscoverWelcomeBannerShown = true
    }

    fun swipeToRefresh() {
        launch {
            (uiState.value as ContentUiState).copy(swipeToRefreshIsRefreshing = true)
            readerDiscoverDataProvider.refreshCards()

            appPrefsWrapper.readerDiscoverWelcomeBannerShown = true
        }
    }

    sealed class DiscoverUiState(
        val contentVisiblity: Boolean = false,
        val progressVisibility: Boolean = false,
        val swipeToRefreshEnabled: Boolean = false
    ) {
        open val swipeToRefreshIsRefreshing: Boolean = false

        data class ContentUiState(
            val cards: List<ReaderCardUiState>,
            override val swipeToRefreshIsRefreshing: Boolean
        ) : DiscoverUiState(contentVisiblity = true, swipeToRefreshEnabled = true)

        object LoadingUiState : DiscoverUiState(progressVisibility = true)
        object ErrorUiState : DiscoverUiState()
    }
}
