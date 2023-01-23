package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderRecommendedBlogsCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderWelcomeBannerCardUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderSubs
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Error
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderDiscoverCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderDiscoverDataProvider
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val INITIATE_LOAD_MORE_OFFSET = 3
const val PHOTON_WIDTH_QUALITY_RATION = 0.5 // load images in 1/2 screen width to save users' data
const val FEATURED_IMAGE_HEIGHT_WIDTH_RATION = 0.56 // 9:16

class ReaderDiscoverViewModel @Inject constructor(
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val readerDiscoverDataProvider: ReaderDiscoverDataProvider,
    private val reblogUseCase: ReblogUseCase,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerTracker: ReaderTracker,
    displayUtilsWrapper: DisplayUtilsWrapper,
    private val getFollowedTagsUseCase: GetFollowedTagsUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private lateinit var parentViewModel: ReaderViewModel

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents: LiveData<Event<PreLoadPostContent>> = _preloadPostEvents

    /**
     * Post which is about to be reblogged after the user selects a target site.
     */
    private var pendingReblogPost: ReaderPost? = null

    private var swipeToRefreshTriggered = false

    /**
     * Don't recalculate the size after a device orientation change as it'd result in change of the url -> it wouldn't
     * use cached images.
     */
    private val photonWidth: Int = (displayUtilsWrapper.getDisplayPixelWidth() * PHOTON_WIDTH_QUALITY_RATION).toInt()
    private val photonHeight: Int = (photonWidth * FEATURED_IMAGE_HEIGHT_WIDTH_RATION).toInt()

    private val communicationChannelObserver = Observer { data: Event<ReaderDiscoverCommunication>? ->
        data?.let {
            data.getContentIfNotHandled()?.let {
                handleDataProviderEvent(it)
            }
        }
    }

    fun start(parentViewModel: ReaderViewModel) {
        if (isStarted) return
        isStarted = true
        this.parentViewModel = parentViewModel
        init()
    }

    private fun init() {
        // Start with loading state
        _uiState.value = DiscoverUiState.LoadingUiState

        readerPostCardActionsHandler.initScope(viewModelScope)

        // Get the correct repository
        readerDiscoverDataProvider.start()

        // Listen to changes to the discover feed
        _uiState.addSource(readerDiscoverDataProvider.discoverFeed) { posts ->
            launch {
                val userTags = getFollowedTagsUseCase.get()
                if (userTags.isEmpty()) {
                    _uiState.value = DiscoverUiState.EmptyUiState.ShowNoFollowedTagsUiState {
                        parentViewModel.onShowReaderInterests()
                    }
                } else {
                    if (posts != null && posts.cards.isNotEmpty()) {
                        _uiState.value = DiscoverUiState.ContentUiState(
                            convertCardsToUiStates(posts),
                            reloadProgressVisibility = false,
                            loadMoreProgressVisibility = false,
                            scrollToTop = swipeToRefreshTriggered
                        )
                        swipeToRefreshTriggered = false
                    } else {
                        _uiState.value = DiscoverUiState.EmptyUiState.ShowNoPostsUiState {
                            _navigationEvents.value = Event(ShowReaderSubs)
                        }
                    }
                }
            }
        }

        // TODO reader improvements: Consider using Channel/Flow
        readerDiscoverDataProvider.communicationChannel.observeForever(communicationChannelObserver)

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

    private suspend fun convertCardsToUiStates(posts: ReaderDiscoverCards): List<ReaderCardUiState> {
        return posts.cards.map { card ->
            when (card) {
                is WelcomeBannerCard -> ReaderWelcomeBannerCardUiState(
                    titleRes = R.string.reader_welcome_banner
                )
                is ReaderPostCard -> postUiStateBuilder.mapPostToUiState(
                    source = ReaderTracker.SOURCE_DISCOVER,
                    post = card.post,
                    isDiscover = true,
                    photonWidth = photonWidth,
                    photonHeight = photonHeight,
                    onButtonClicked = this@ReaderDiscoverViewModel::onButtonClicked,
                    onItemClicked = this@ReaderDiscoverViewModel::onPostItemClicked,
                    onItemRendered = this@ReaderDiscoverViewModel::onItemRendered,
                    onDiscoverSectionClicked = this@ReaderDiscoverViewModel::onDiscoverClicked,
                    onMoreButtonClicked = this@ReaderDiscoverViewModel::onMoreButtonClicked,
                    onMoreDismissed = this@ReaderDiscoverViewModel::onMoreMenuDismissed,
                    onVideoOverlayClicked = this@ReaderDiscoverViewModel::onVideoOverlayClicked,
                    onPostHeaderViewClicked = this@ReaderDiscoverViewModel::onPostHeaderClicked,
                    onTagItemClicked = this@ReaderDiscoverViewModel::onTagItemClicked,
                    postListType = TAG_FOLLOWED
                )
                is InterestsYouMayLikeCard -> {
                    postUiStateBuilder.mapTagListToReaderInterestUiState(
                        card.interests,
                        this@ReaderDiscoverViewModel::onReaderTagClicked
                    )
                }
                is ReaderRecommendedBlogsCard -> {
                    postUiStateBuilder.mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
                        recommendedBlogs = card.blogs,
                        onItemClicked = this@ReaderDiscoverViewModel::onRecommendedSiteItemClicked,
                        onFollowClicked = this@ReaderDiscoverViewModel::onFollowSiteClicked
                    )
                }
            }
        }
    }

    private fun handleDataProviderEvent(it: ReaderDiscoverCommunication) {
        when (it) {
            is Started -> {
                handleStartedEvent(it)
            }
            is Success -> {
            } // no op
            is Error ->
                handleErrorEvent()
        }
    }

    private fun handleStartedEvent(it: ReaderDiscoverCommunication) {
        uiState.value.let { state ->
            if (state is DiscoverUiState.ContentUiState) {
                when (it.task) {
                    REQUEST_FIRST_PAGE -> {
                        _uiState.value = state.copy(reloadProgressVisibility = true)
                    }
                    REQUEST_MORE -> {
                        _uiState.value = state.copy(loadMoreProgressVisibility = true)
                    }
                }
            } else {
                _uiState.value = DiscoverUiState.LoadingUiState
            }
        }
    }

    private fun handleErrorEvent() {
        _uiState.value?.let { uiState ->
            when (uiState) {
                is DiscoverUiState.LoadingUiState -> {
                    // show fullscreen error
                    _uiState.value = DiscoverUiState.EmptyUiState.RequestFailedUiState { onRetryButtonClick() }
                }
                is DiscoverUiState.ContentUiState -> {
                    _uiState.value = uiState.copy(
                        reloadProgressVisibility = false,
                        loadMoreProgressVisibility = false
                    )
                    // show snackbar
                    _snackbarEvents.postValue(
                        Event(
                            SnackbarMessageHolder(
                                UiStringRes(R.string.reader_error_request_failed_title)
                            )
                        )
                    )
                }
                is DiscoverUiState.EmptyUiState.RequestFailedUiState -> Unit // Do nothing
                is DiscoverUiState.EmptyUiState.ShowNoFollowedTagsUiState -> Unit // Do nothing
                is DiscoverUiState.EmptyUiState.ShowNoPostsUiState -> Unit // Do nothing
            }
        }
    }

    private fun onReaderTagClicked(tag: String) {
        launch(ioDispatcher) {
            readerTracker.track(AnalyticsTracker.Stat.READER_DISCOVER_TOPIC_TAPPED)
            val readerTag = readerUtilsWrapper.getTagFromTagName(tag, FOLLOWED)
            _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
        }
    }

    private fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(
                    it,
                    type,
                    isBookmarkList = false,
                    source = ReaderTracker.SOURCE_DISCOVER
                )
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
                readerPostCardActionsHandler.handleHeaderClicked(
                    it.blogId,
                    it.feedId,
                    it.isFollowedByCurrentUser
                )
            }
        }
    }

    private fun onTagItemClicked(tagSlug: String) {
        launch(ioDispatcher) {
            val readerTag = readerUtilsWrapper.getTagFromTagName(tagSlug, FOLLOWED)
            _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
        }
    }

    private fun onPostItemClicked(postId: Long, blogId: Long) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.handleOnItemClicked(
                    it,
                    ReaderTracker.SOURCE_DISCOVER
                )
            }
        }
    }

    private fun onRecommendedSiteItemClicked(blogId: Long, feedId: Long, isFollowed: Boolean) {
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_SUGGESTED_SITE_VISITED,
            blogId,
            feedId
        )
        _navigationEvents.postValue(Event(ShowBlogPreview(blogId, feedId, isFollowed)))
    }

    private fun onFollowSiteClicked(recommendedBlogUiState: ReaderRecommendedBlogUiState) {
        launch {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_SUGGESTED_SITE_TOGGLE_FOLLOW,
                recommendedBlogUiState.blogId,
                recommendedBlogUiState.feedId,
                !recommendedBlogUiState.isFollowed
            )
            readerPostCardActionsHandler.handleFollowRecommendedSiteClicked(
                recommendedBlogUiState,
                ReaderTracker.SOURCE_DISCOVER
            )
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
        (uiState.value as? DiscoverUiState.ContentUiState)?.cards?.let {
            val closeToEndIndex = it.size - INITIATE_LOAD_MORE_OFFSET
            if (closeToEndIndex > 0) {
                val isCardCloseToEnd: Boolean = it.getOrNull(closeToEndIndex) == item
                if (isCardCloseToEnd) {
                    readerTracker.track(AnalyticsTracker.Stat.READER_DISCOVER_PAGINATED)
                    launch(ioDispatcher) { readerDiscoverDataProvider.loadMoreCards() }
                }
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun onDiscoverClicked(postId: Long, blogId: Long) {
        // TODO malinjir: add on discover clicked listener
    }

    private fun onMoreButtonClicked(postUiState: ReaderPostUiState) {
        changeMoreMenuVisibility(postUiState, true)
    }

    private fun onMoreMenuDismissed(postUiState: ReaderPostUiState) {
        changeMoreMenuVisibility(postUiState, false)
    }

    private fun changeMoreMenuVisibility(currentUiState: ReaderPostUiState, show: Boolean) {
        launch {
            findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                val moreMenuItems = if (show) {
                    readerPostMoreButtonUiStateBuilder.buildMoreMenuItems(
                        post, this@ReaderDiscoverViewModel::onButtonClicked
                    )
                } else {
                    null
                }

                replaceUiStateItem(currentUiState, currentUiState.copy(moreMenuItems = moreMenuItems))
            }
        }
    }

    private fun replaceUiStateItem(before: ReaderPostUiState, after: ReaderPostUiState) {
        (_uiState.value as? DiscoverUiState.ContentUiState)?.let {
            val updatedList = it.cards.toMutableList()
            val index = it.cards.indexOf(before)
            if (index != -1) {
                updatedList[index] = after
                _uiState.value = it.copy(cards = updatedList)
            }
        }
    }

    fun onReblogSiteSelected(siteLocalId: Int) {
        launch {
            val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
            val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
            if (navigationTarget != null) {
                _navigationEvents.value = Event(navigationTarget)
            } else {
                _snackbarEvents.value = Event(SnackbarMessageHolder(UiStringRes(R.string.reader_reblog_error)))
            }
            pendingReblogPost = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        readerDiscoverDataProvider.stop()
        readerPostCardActionsHandler.onCleared()
        readerDiscoverDataProvider.communicationChannel.removeObserver(communicationChannelObserver)

        appPrefsWrapper.readerDiscoverWelcomeBannerShown = true
    }

    fun swipeToRefresh() {
        readerTracker.track(AnalyticsTracker.Stat.READER_PULL_TO_REFRESH)
        swipeToRefreshTriggered = true
        launch {
            readerDiscoverDataProvider.refreshCards()
        }

        appPrefsWrapper.readerDiscoverWelcomeBannerShown = true
    }

    fun onRetryButtonClick() {
        launch {
            readerDiscoverDataProvider.refreshCards()
        }
    }

    sealed class DiscoverUiState(
        val contentVisiblity: Boolean = false,
        val fullscreenProgressVisibility: Boolean = false,
        val swipeToRefreshEnabled: Boolean = false,
        open val fullscreenEmptyVisibility: Boolean = false,
        open val scrollToTop: Boolean = false
    ) {
        open val reloadProgressVisibility: Boolean = false
        open val loadMoreProgressVisibility: Boolean = false

        data class ContentUiState(
            val cards: List<ReaderCardUiState>,
            override val reloadProgressVisibility: Boolean,
            override val loadMoreProgressVisibility: Boolean,
            override val scrollToTop: Boolean
        ) : DiscoverUiState(contentVisiblity = true, swipeToRefreshEnabled = true)

        object LoadingUiState : DiscoverUiState(fullscreenProgressVisibility = true)

        sealed class EmptyUiState : DiscoverUiState(fullscreenEmptyVisibility = true) {
            abstract val titleResId: Int
            abstract val buttonResId: Int
            open val subTitleRes: Int? = null
            abstract val action: () -> Unit
            open val illustrationResId: Int? = null

            data class RequestFailedUiState(override val action: () -> Unit) : EmptyUiState() {
                override val titleResId = R.string.connection_error
                override val subTitleRes = R.string.reader_error_request_failed_title
                override val buttonResId = R.string.retry
            }

            data class ShowNoFollowedTagsUiState(override val action: () -> Unit) : EmptyUiState() {
                override val titleResId = R.string.reader_discover_empty_title
                override val subTitleRes = R.string.reader_discover_empty_subtitle
                override val buttonResId = R.string.reader_discover_empty_button_text
            }

            data class ShowNoPostsUiState(override val action: () -> Unit) : EmptyUiState() {
                override val titleResId = R.string.reader_discover_no_posts_title
                override val buttonResId = R.string.reader_discover_no_posts_button_text
                override val subTitleRes = R.string.reader_discover_no_posts_subtitle
                override val illustrationResId = R.drawable.img_illustration_empty_results_216dp
            }
        }
    }
}
