package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderTypes
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_POST
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.TOGGLE_SEEN_STATUS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderPostListEvent
import org.wordpress.android.ui.reader.repository.ReaderPostListRepository
import org.wordpress.android.ui.reader.repository.ReaderPostListTarget
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTab
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
class ReaderPostListViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val reblogUseCase: ReblogUseCase,
    private val readerTracker: ReaderTracker,
    private val seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase,
    private val repository: ReaderPostListRepository,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val moreMenuBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val emptyStateBuilder: ReaderPostListEmptyStateBuilder,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val accountStore: AccountStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private var readerViewModel: ReaderViewModel? = null

    /**
     * Post which is about to be reblogged after the user selects a target site.
     */
    private var pendingReblogPost: ReaderPost? = null

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents = _preloadPostEvents

    private val _uiState = MutableStateFlow(ReaderPostListUiState())
    val uiState: StateFlow<ReaderPostListUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<ReaderPostListUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<ReaderPostListUiEvent> = _uiEvents.receiveAsFlow()

    var listType: ReaderPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE
        private set
    var currentTag: ReaderTag? = null
        private set
    var searchQuery: String? = null
        private set
    var source: String = ""
        private set

    private var originalTag: ReaderTag? = null
    private var blogId = 0L
    private var feedId = 0L
    private var isTopLevel = false
    private var isFilterable = false
    private val tagHistory = ArrayList<String>()
    private var hasRequestedPosts = false
    private var hasUpdatedPosts = false
    private val updateActions = HashSet<UpdateAction>()
    private var photonWidth = 0
    private var photonHeight = 0
    private var lastModeInfo: ReaderModeInfo? = null

    private var posts = ReaderPostList()
    private var cardStates: List<ReaderPostUiState?> = emptyList()
    private var postsVersion = 0
    private var isLoaded = false
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var gapMarkerIndex = -1
    private var isGapMarkerHidden = false
    private var isGapFilling = false
    private var isTagHeaderFollowed = false
    private var isTagFollowInFlight = false
    private var moreMenu: Pair<ReaderPost, List<ReaderPostCardAction>>? = null
    private val renderedIds = HashSet<String>()

    private var isFirstItemVisible = true
    private var isViewStarted = false
    private var isPullRefreshing = false
    private var isNewPostsBarVisible = false
    private var requestFailed = false
    private var autoUpdateSkipped = false
    private var isSiteAllSubfilter = false
    private var hasFollowedBlogs = false
    private var restorePosition = 0
    private var pendingScrollToBlogId: Long? = null
    private var scrollToPosition: Int? = null

    // card callbacks are created once so rebuilt card states compare equal when nothing else changed
    private val cardButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit = ::onCardButtonClicked
    private val cardClicked: (Long, Long) -> Unit = ::onCardClicked
    private val cardRendered: (ReaderCardUiState) -> Unit = ::onCardRendered
    private val moreButtonClicked: (ReaderPostUiState) -> Unit = ::onMoreButtonClicked
    private val moreMenuDismissed: (ReaderPostUiState) -> Unit = { closeMoreMenu() }
    private val videoOverlayClicked: (Long, Long) -> Unit = ::onVideoOverlayClicked
    private val postHeaderClicked: (Long, Long) -> Unit = ::onPostHeaderClicked

    private val refreshPostsObserver = Observer<Event<Unit>> { event ->
        event.getContentIfNotHandled()?.let { reload() }
    }
    private val followStatusObserver = Observer<FollowStatusChanged> { onFollowStatusChanged(it) }

    val hasPosts: Boolean
        get() = posts.isNotEmpty()

    val isBookmarksList: Boolean
        get() = listType == ReaderPostListType.TAG_FOLLOWED && currentTag?.isBookmarked == true

    private val isSearch: Boolean
        get() = listType == ReaderPostListType.SEARCH_RESULTS

    private val isFollowingScreen: Boolean
        get() = originalTag?.isFollowedSites == true

    fun start(
        args: ReaderPostListArgs,
        readerViewModel: ReaderViewModel?,
        photonWidth: Int,
        photonHeight: Int
    ) {
        this.readerViewModel = readerViewModel

        // the card image size depends on the window width, which changes on rotation
        val isPhotonSizeChanged = photonWidth != this.photonWidth || photonHeight != this.photonHeight
        this.photonWidth = photonWidth
        this.photonHeight = photonHeight

        if (isStarted) {
            if (isPhotonSizeChanged && isLoaded) reload()
            return
        }
        isStarted = true

        listType = args.listType
        currentTag = args.tag
        originalTag = args.originalTag
        blogId = args.blogId
        feedId = args.feedId
        isTopLevel = args.isTopLevel
        isFilterable = args.isFilterable
        searchQuery = args.searchQuery
        tagHistory.addAll(args.tagHistory)
        if (tagHistory.isEmpty() && listType == ReaderPostListType.TAG_PREVIEW) {
            args.tag?.let { tagHistory.add(it.tagSlug) }
        }
        hasRequestedPosts = args.hasRequestedPosts
        hasUpdatedPosts = args.hasUpdatedPosts
        restorePosition = args.restorePosition
        updateSource()

        init()
    }

    private fun init() {
        readerPostCardActionsHandler.initScope(viewModelScope)
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

        readerPostCardActionsHandler.refreshPosts.observeForever(refreshPostsObserver)
        readerPostCardActionsHandler.followStatusUpdated.observeForever(followStatusObserver)

        launch {
            readerPostCardActionsHandler.scrollToSiteId.collect { blogId ->
                pendingScrollToBlogId = blogId
            }
        }

        launch {
            repository.events.collect { onRepositoryEvent(it) }
        }

        updateUiState()
    }

    override fun onCleared() {
        readerPostCardActionsHandler.refreshPosts.removeObserver(refreshPostsObserver)
        readerPostCardActionsHandler.followStatusUpdated.removeObserver(followStatusObserver)
        super.onCleared()
    }

    /**
     * Returns the state to save so the list can be restored after process death
     */
    fun currentArgs(restorePosition: Int) = ReaderPostListArgs(
        listType = listType,
        tag = currentTag,
        originalTag = originalTag,
        blogId = blogId,
        feedId = feedId,
        isTopLevel = isTopLevel,
        isFilterable = isFilterable,
        searchQuery = searchQuery,
        tagHistory = tagHistory.toList(),
        hasRequestedPosts = hasRequestedPosts,
        hasUpdatedPosts = hasUpdatedPosts,
        restorePosition = restorePosition
    )

    /*
     * Called from the fragment's onStart(). Loads posts the first time, and requests newer posts from
     * the server if that hasn't been done yet.
     */
    fun onStart() {
        isViewStarted = true
        if (!isLoaded && loadJob?.isActive != true) {
            reload()
        }
        if (!hasRequestedPosts && networkUtilsWrapper.isNetworkAvailable()) {
            hasRequestedPosts = true
            if (listType.isTagType) {
                updateCurrentTagIfTime()
            } else if (listType == ReaderPostListType.BLOG_PREVIEW) {
                requestPosts(blogOrFeedTarget(), UpdateAction.REQUEST_NEWER)
            }
        }
    }

    fun onStop() {
        isViewStarted = false
    }

    /*
     * Called when the fragment resumes after having been paused. [addedTagName] is the name of a tag
     * the user just followed, and [siteSubfilter] is the site being filtered on, if any.
     */
    fun onResumeAfterPause(addedTagName: String?, siteSubfilter: SubfilterListItem.Site?) {
        when {
            listType == ReaderPostListType.TAG_FOLLOWED -> launch {
                val tag = currentTag
                if (isFollowingScreen && addedTagName != null) {
                    // user just added a tag so switch to it
                    sendUiEvent(ReaderPostListUiEvent.SetSubfilterFromTag(repository.getFollowedTag(addedTagName)))
                } else if (isFollowingScreen && (tag == null || !repository.tagExists(tag))) {
                    // user just removed a tag which was selected in the subfilter
                    sendUiEvent(ReaderPostListUiEvent.SetDefaultSubfilter)
                } else {
                    reload()
                    updateCurrentTagIfTime()
                }
            }

            siteSubfilter != null -> launch {
                if (repository.isFollowedSite(siteSubfilter.blog)) {
                    reload()
                } else if (isFilterable) {
                    sendUiEvent(ReaderPostListUiEvent.SetDefaultSubfilter)
                }
            }

            else -> reload()
        }
    }

    /*
     * Called when the subfilter (Subscriptions) changes the list being shown
     */
    fun changeReaderMode(info: ReaderModeInfo) {
        // the subfilter's LiveData replays its last value when the fragment's view is recreated
        if (info == lastModeInfo) return
        lastModeInfo = info

        val tag = currentTag
        val isChanged = (info.tag != null && tag != null && info.tag != tag) ||
                listType != info.listType ||
                blogId != info.blogId ||
                feedId != info.feedId ||
                info.isFirstLoad
        if (!isChanged) return

        if (!info.isFirstLoad) {
            ReaderTracker.trackTagLoaded(info.tag)
        }
        info.tag?.let { currentTag = it }
        listType = info.listType
        blogId = info.blogId
        feedId = info.feedId
        onTargetChanged()

        // the first mode change happens when the list opens, and onStart() takes care of that request
        if (info.requestNewerPosts && !info.isFirstLoad) {
            updatePosts(forced = false)
        }
    }

    fun onSubfilterChanged(subfilter: SubfilterListItem?) {
        isSiteAllSubfilter = subfilter is SubfilterListItem.SiteAll
        updateUiState()
    }

    fun onPullToRefresh() {
        updatePosts(forced = true)
    }

    private fun updatePosts(forced: Boolean) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            isPullRefreshing = false
            sendUiEvent(ReaderPostListUiEvent.NoConnection)
            updateUiState()
            return
        }

        if (forced) {
            repository.updateTagsAndFollowedBlogs()
        }

        val action = if (forced) UpdateAction.REQUEST_REFRESH else UpdateAction.REQUEST_NEWER
        val target = when (listType) {
            ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> currentTag?.let {
                ReaderPostListTarget.Tag(it)
            }
            ReaderPostListType.BLOG_PREVIEW -> blogOrFeedTarget()
            ReaderPostListType.SEARCH_RESULTS, ReaderPostListType.TAGS_FEED -> null
        }
        if (target != null) {
            requestPosts(target, action)
            // show swipe-to-refresh progress since this is a manual refresh
            isPullRefreshing = true
        }

        if (currentTag?.isBookmarked == true) {
            launch {
                repository.purgeUnbookmarkedPosts()
                reload()
            }
        }
        updateUiState()
    }

    /*
     * Called when the last row is shown, to load older posts
     */
    fun onLoadMore() {
        if (updateActions.isNotEmpty()) {
            return
        }
        launch {
            when (listType) {
                ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> {
                    val tag = currentTag ?: return@launch
                    loadMore(ReaderPostListTarget.Tag(tag)) {
                        requestPosts(ReaderPostListTarget.Tag(tag), UpdateAction.REQUEST_OLDER)
                    }
                }

                ReaderPostListType.BLOG_PREVIEW -> {
                    val target = blogOrFeedTarget()
                    loadMore(target) { requestPosts(target, UpdateAction.REQUEST_OLDER) }
                }

                ReaderPostListType.SEARCH_RESULTS -> {
                    val query = searchQuery ?: return@launch
                    loadMore(searchTarget(query)) { offset -> repository.searchPosts(query, offset) }
                }

                ReaderPostListType.TAGS_FEED -> {}
            }
        }
    }

    private suspend fun loadMore(target: ReaderPostListTarget, request: (Int) -> Unit) {
        val count = repository.getPostCount(target)
        if (count < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
            request(count)
            readerTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL)
        }
    }

    fun onGapMarkerClicked() {
        val tag = currentTag ?: return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            sendUiEvent(ReaderPostListUiEvent.NoConnection)
            return
        }
        // EventBus tells us when the gap is filled, and the reload that follows resets the marker
        repository.requestPosts(ReaderPostListTarget.Tag(tag), UpdateAction.REQUEST_OLDER_THAN_GAP)
        isGapFilling = true
        updateUiState()
    }

    fun onTagHeaderFollowClicked() {
        val tag = currentTag
        if (tag == null || isTagFollowInFlight) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            sendUiEvent(ReaderPostListUiEvent.NoConnection)
            return
        }
        isTagFollowInFlight = true
        updateUiState()
        launch {
            val isFollowRequest = !repository.isFollowedTag(tag)
            if (accountStore.hasAccessToken()) {
                isTagHeaderFollowed = isFollowRequest
                updateUiState()
            }
            val succeeded = repository.followTag(tag, isFollowRequest)
            if (succeeded) {
                val stat = if (isFollowRequest) {
                    AnalyticsTracker.Stat.READER_TAG_FOLLOWED
                } else {
                    AnalyticsTracker.Stat.READER_TAG_UNFOLLOWED
                }
                readerTracker.trackTag(stat, tag.tagSlug, source)
            } else {
                sendUiEvent(ReaderPostListUiEvent.TagFollowFailed(isFollowRequest))
            }
            isTagHeaderFollowed = repository.isFollowedTag(tag)
            isTagFollowInFlight = false
            updateUiState()
        }
    }

    fun onFirstItemVisibilityChanged(isVisible: Boolean) {
        isFirstItemVisible = isVisible
    }

    fun onNewPostsBarScrolledAway() {
        if (isNewPostsBarVisible) {
            isNewPostsBarVisible = false
            updateUiState()
        }
    }

    /*
     * Called when the fragment's view is recreated with this view model already holding posts (e.g. after
     * rotation), since the RecyclerView loses its position when it lays out before the list arrives
     */
    fun onViewRestored(position: Int) {
        if (position <= 0) return
        if (posts.isNotEmpty()) {
            scrollToPosition = position
            updateUiState()
        } else {
            restorePosition = position
        }
    }

    fun onScrollHandled() {
        if (scrollToPosition != null) {
            scrollToPosition = null
            _uiState.value = _uiState.value.copy(scrollToPosition = null)
        }
    }

    /*
     * Called when the user taps back - returns true if this went back to a previously viewed tag
     */
    @Suppress("ReturnCount")
    fun goBackInTagHistory(): Boolean {
        if (tagHistory.isEmpty()) {
            return false
        }
        var tagName = tagHistory.removeAt(tagHistory.lastIndex)
        if (tagName.equals(currentTag?.tagSlug, ignoreCase = true)) {
            if (tagHistory.isEmpty()) {
                return false
            }
            tagName = tagHistory.removeAt(tagHistory.lastIndex)
        }
        launch { setCurrentTag(repository.getFollowedTag(tagName)) }
        return true
    }

    private fun setCurrentTag(tag: ReaderTag) {
        if (ReaderTag.isSameTag(tag, currentTag)) {
            return
        }
        currentTag = tag
        if (listType == ReaderPostListType.TAG_PREVIEW) {
            tagHistory.add(tag.tagSlug)
        }
        onTargetChanged()
        updateCurrentTagIfTime()
    }

    fun onEmptyStateButtonTapped(button: ReaderPostListEmptyButton) {
        val path = when (button) {
            ReaderPostListEmptyButton.DISCOVER -> ReaderTag.DISCOVER_PATH
            ReaderPostListEmptyButton.FOLLOWED -> ReaderTag.FOLLOWING_PATH
        }
        launch {
            readerViewModel?.updateSelectedContent(repository.getTagFromEndpointOrDefault(path))
        }
    }

    /*
     * Search - the search field itself lives in ReaderSearchActivity
     */
    fun onSearchSubmitted(query: String) {
        resetSearch(query)
        // show the search as running until the service reports back, so "no results" doesn't flash first
        setIsUpdating(true, searchAction(0))
        searchJob = launch {
            // search results are ephemeral, so each search starts from scratch
            repository.deletePostsWithTag(ReaderUtils.getTagForSearchQuery(query))
            reload()
            repository.searchPosts(query, 0)
        }
    }

    fun onSearchCleared() = resetSearch(query = null)

    /*
     * drops the previous search, including one that hasn't started yet and anything it left running
     */
    private fun resetSearch(query: String?) {
        searchJob?.cancel()
        searchQuery = query
        requestFailed = false
        updateActions.clear()
        clearPosts()
    }

    // events from a search the user has since replaced or cleared are ignored
    private fun isCurrentSearch(query: String) = isSearch && query == searchQuery

    private fun onRepositoryEvent(event: ReaderPostListEvent) {
        when (event) {
            is ReaderPostListEvent.UpdateStarted -> if (isCurrentTarget(event.tag)) {
                requestFailed = false
                setIsUpdating(true, event.action)
            }

            is ReaderPostListEvent.UpdateEnded -> if (isCurrentTarget(event.tag)) {
                onUpdateEnded(event)
            }

            is ReaderPostListEvent.SearchStarted -> if (isCurrentSearch(event.query)) {
                // a re-run of the same query can follow a failed end from the run it replaced
                requestFailed = false
                setIsUpdating(true, searchAction(event.offset))
            }

            is ReaderPostListEvent.SearchEnded -> if (isCurrentSearch(event.query)) {
                onSearchEnded(event)
            }

            is ReaderPostListEvent.FollowedTagsFetched -> if (listType == ReaderPostListType.TAG_FOLLOWED) {
                onFollowedTagsFetched()
            }

            is ReaderPostListEvent.FollowedBlogsFetched -> if (event.didChange && isFollowedSitesList) {
                reload()
            }
        }
    }

    private fun onSearchEnded(event: ReaderPostListEvent.SearchEnded) {
        val action = searchAction(event.offset)
        if (event.didSucceed) {
            // don't publish the end of the search until the reload does, or "no results" would show (and be
            // announced) before the results. Success doesn't mean there are results - the reload shows that.
            updateActions.remove(action)
            reload()
        } else {
            requestFailed = true
            setIsUpdating(false, action)
        }
    }

    private fun onFollowedTagsFetched() = launch {
        // update the current tag if the list is empty - this happens on first run
        if (posts.isEmpty() && (repository.hasFollowedBlogs() || !hasUpdatedPosts)) {
            currentTag?.let { requestPosts(ReaderPostListTarget.Tag(it), UpdateAction.REQUEST_NEWER) }
        }
    }

    private fun onUpdateEnded(event: ReaderPostListEvent.UpdateEnded) {
        setIsUpdating(false, event.action)
        hasUpdatedPosts = true

        // the new posts bar doesn't apply to search results
        if (isSearch) {
            return
        }

        // the "new posts" bar appears when there are new posts in a followed tag and the user has
        // scrolled past the first post - the new posts aren't shown until the user taps the bar
        if (isNewPostsUpdate(event) && listType == ReaderPostListType.TAG_FOLLOWED && canShowNewPostsBar()) {
            isNewPostsBarVisible = true
            // the gap marker is no longer valid
            isGapMarkerHidden = true
            updateUiState()
        } else if (event.result.isNewOrChanged || event.action == UpdateAction.REQUEST_REFRESH) {
            reload()
        } else {
            requestFailed = event.result == UpdateResult.FAILED
            if (event.action == UpdateAction.REQUEST_OLDER_THAN_GAP) {
                // the gap wasn't filled, so reload to reset the gap marker's progress
                reload()
            } else {
                updateUiState()
            }
        }
    }

    private fun isNewPostsUpdate(event: ReaderPostListEvent.UpdateEnded) =
        event.result == UpdateResult.HAS_NEW && event.action == UpdateAction.REQUEST_NEWER

    // the list reloads when the user comes back, so there's no need for the bar while stopped
    private fun canShowNewPostsBar() = posts.isNotEmpty() && !isFirstItemVisible && isViewStarted

    private val isFollowedSitesList: Boolean
        get() = listType == ReaderPostListType.TAG_FOLLOWED &&
                currentTag.let { it != null && (it.isFollowedSites || it.isDefaultInMemoryTag) }

    // blog and feed updates don't carry a tag, so they only apply to blog previews
    private fun isCurrentTarget(tag: ReaderTag?) = if (tag == null) {
        listType == ReaderPostListType.BLOG_PREVIEW
    } else {
        ReaderTag.isSameTag(tag, currentTag)
    }

    private fun searchAction(offset: Int) =
        if (offset == 0) UpdateAction.REQUEST_NEWER else UpdateAction.REQUEST_OLDER

    private fun setIsUpdating(isUpdating: Boolean, action: UpdateAction) {
        isPullRefreshing = false
        if (isUpdating) updateActions.add(action) else updateActions.remove(action)
        updateUiState()
    }

    /*
     * update the current tag if it's time to do so
     */
    private fun updateCurrentTagIfTime() {
        val tag = currentTag ?: return
        launch {
            if (repository.shouldAutoUpdateTag(tag)) {
                requestPosts(ReaderPostListTarget.Tag(tag), UpdateAction.REQUEST_NEWER)
            } else if (isBookmarksList || tag.isListTopic) {
                // there won't be an update, so show the empty state for these lists right away
                autoUpdateSkipped = true
                updateUiState()
            }
        }
    }

    private fun requestPosts(target: ReaderPostListTarget, action: UpdateAction) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled update")
            return
        }
        repository.requestPosts(target, action)
    }

    private fun blogOrFeedTarget() = if (feedId != 0L) {
        ReaderPostListTarget.Feed(feedId)
    } else {
        ReaderPostListTarget.Blog(blogId)
    }

    private fun searchTarget(query: String) = ReaderPostListTarget.Tag(ReaderUtils.getTagForSearchQuery(query))

    private fun currentTarget(): ReaderPostListTarget? = when (listType) {
        ReaderPostListType.TAG_FOLLOWED, ReaderPostListType.TAG_PREVIEW -> currentTag?.let {
            ReaderPostListTarget.Tag(it)
        }
        ReaderPostListType.BLOG_PREVIEW -> blogOrFeedTarget()
        ReaderPostListType.SEARCH_RESULTS -> searchTarget(searchQuery.orEmpty())
        ReaderPostListType.TAGS_FEED -> null
    }

    private fun updateSource() {
        source = currentTag?.let { readerTracker.getSource(listType, ReaderTab.transformTagToTab(it)) }
            ?: readerTracker.getSource(listType)
    }

    /*
     * Called when the tag, blog or list type changes - clears the list and loads the new posts
     */
    private fun onTargetChanged() {
        updateSource()
        renderedIds.clear()
        autoUpdateSkipped = false
        clearPosts()
        reload()
    }

    private fun clearPosts() {
        loadJob?.cancel()
        posts = ReaderPostList()
        cardStates = emptyList()
        gapMarkerIndex = -1
        moreMenu = null
        isLoaded = false
        updateUiState()
    }

    /*
     * Reloads posts from the local database
     */
    fun reload() {
        isNewPostsBarVisible = false
        loadJob?.cancel()
        loadJob = launch { loadPosts() }
    }

    private suspend fun loadPosts() {
        val target = currentTarget()
        val newPosts = target?.let { repository.getPosts(it) } ?: ReaderPostList()
        val newGapMarkerIndex = loadListDetails(newPosts)

        // build the cards before swapping in the new posts, since the UI can re-render while they're built
        val newCardStates = buildCardStates(newPosts)
        if (!posts.isSameListWithBookmark(newPosts)) {
            postsVersion++
        }
        posts = newPosts
        cardStates = newCardStates
        gapMarkerIndex = newGapMarkerIndex
        isGapMarkerHidden = false
        isGapFilling = false
        requestFailed = false
        isLoaded = true
        applyPendingScroll()

        updateUiState()
        sendUiEvent(ReaderPostListUiEvent.DataLoaded)
    }

    /*
     * Loads what the list shows besides the posts: whether the user follows any sites (for the empty
     * state) and whether the previewed tag is followed (for the header). Returns the gap marker's index,
     * which is only applied once the new posts are swapped in.
     */
    private suspend fun loadListDetails(newPosts: ReaderPostList): Int {
        val tag = currentTag
        if (isFollowedSitesList) {
            hasFollowedBlogs = repository.hasFollowedBlogs()
        }
        if (listType == ReaderPostListType.TAG_PREVIEW && tag != null) {
            val isFollowed = repository.isFollowedTag(tag)
            // a follow tap during the read owns the header's state until its request finishes
            if (!isTagFollowInFlight) {
                isTagHeaderFollowed = isFollowed
            }
        }
        return if (listType.isTagType && tag != null) getGapMarkerIndex(tag, newPosts) else -1
    }

    private fun applyPendingScroll() {
        if (posts.isNotEmpty()) {
            if (restorePosition > 0) {
                scrollToPosition = restorePosition
            }
            pendingScrollToBlogId?.let { blogId ->
                positionOfFirstPostFromBlog(blogId)?.let { scrollToPosition = it }
                pendingScrollToBlogId = null
            }
        }
        restorePosition = 0
    }

    private suspend fun getGapMarkerIndex(tag: ReaderTag, newPosts: ReaderPostList): Int {
        val ids = repository.getGapMarkerIds(tag) ?: return -1
        val index = newPosts.indexOfIds(ids)
        return if (index > -1 && index == newPosts.size - 1) {
            // remove the gap marker if it's on the last post (edge case but it can happen after a purge)
            AppLog.w(T.READER, "gap marker at/after last post, removed")
            repository.removeGapMarker(tag)
            -1
        } else {
            index
        }
    }

    private suspend fun buildCardStates(posts: ReaderPostList): List<ReaderPostUiState?> {
        val isBookmarksList = isBookmarksList
        return withContext(bgDispatcher) {
            posts.map { post ->
                if (post.isXpost || (isBookmarksList && !post.isBookmarked)) {
                    null
                } else {
                    postUiStateBuilder.mapPostToUiStateBlocking(
                        source = source,
                        post = post,
                        photonWidth = photonWidth,
                        photonHeight = photonHeight,
                        postListType = listType,
                        onButtonClicked = cardButtonClicked,
                        onItemClicked = cardClicked,
                        onItemRendered = cardRendered,
                        onMoreButtonClicked = moreButtonClicked,
                        onMoreDismissed = moreMenuDismissed,
                        onVideoOverlayClicked = videoOverlayClicked,
                        onPostHeaderViewClicked = postHeaderClicked
                    )
                }
            }
        }
    }

    private fun buildItems(): List<ReaderPostListItem> {
        val items = ArrayList<ReaderPostListItem>()
        val tag = currentTag
        if (!isTopLevel && (tag?.isDiscover == true || listType == ReaderPostListType.BLOG_PREVIEW)) {
            if (tag?.isDiscover == true) {
                items.add(ReaderPostListItem.SiteHeader(ReaderConstants.DISCOVER_SITE_ID, 0, postsVersion))
            } else {
                items.add(ReaderPostListItem.SiteHeader(blogId, feedId, postsVersion))
            }
        } else if (listType == ReaderPostListType.TAG_PREVIEW && tag != null && posts.isNotEmpty()) {
            items.add(ReaderPostListItem.TagHeader(tag.label, isTagHeaderFollowed, isTagFollowInFlight))
        }

        val menu = moreMenu
        val isBookmarksList = isBookmarksList
        posts.forEachIndexed { index, post ->
            val cardState = cardStates.getOrNull(index)
            items.add(
                when {
                    post.isXpost -> ReaderPostListItem.XPost(post)
                    isBookmarksList && !post.isBookmarked -> ReaderPostListItem.RemovedPost(post)
                    // the card hasn't been built yet
                    cardState == null -> return@forEachIndexed
                    menu != null && menu.first.hasSameIds(post) -> ReaderPostListItem.Post(
                        cardState.copy(moreMenuItems = menu.second),
                        post.stableId
                    )
                    else -> ReaderPostListItem.Post(cardState, post.stableId)
                }
            )
            // the gap marker appears *below* the post it's attached to
            if (index == gapMarkerIndex && !isGapMarkerHidden) {
                items.add(ReaderPostListItem.GapMarker(isGapFilling))
            }
        }
        return items
    }

    private fun positionOfFirstPostFromBlog(blogId: Long): Int? = buildItems().indexOfFirst { item ->
        when (item) {
            is ReaderPostListItem.Post -> item.uiState.blogId == blogId
            is ReaderPostListItem.XPost -> item.post.blogId == blogId
            is ReaderPostListItem.RemovedPost -> item.post.blogId == blogId
            else -> false
        }
    }.takeIf { it > -1 }

    private fun emptyStateInput() = ReaderPostListEmptyStateBuilder.Input(
        listType = listType,
        tag = currentTag,
        isSelfHostedCta = isSelfHostedCta(),
        isNetworkAvailable = networkUtilsWrapper.isNetworkAvailable(),
        requestFailed = requestFailed,
        isUpdating = updateActions.isNotEmpty(),
        hasFollowedBlogs = hasFollowedBlogs,
        searchQuery = searchQuery
    )

    private fun isSelfHostedCta() = isFilterable && !accountStore.hasAccessToken() && isSiteAllSubfilter

    /*
     * Returns the empty state to show, or null if it shouldn't be shown
     */
    private fun buildEmptyState(): ReaderPostListEmptyState? {
        val emptyState = if (shouldShowEmptyState()) emptyStateBuilder.build(emptyStateInput()) else null
        if (emptyState == ReaderPostListEmptyState.NoConnection) {
            // no update can complete while offline
            updateActions.clear()
        }
        return emptyState
    }

    private fun shouldShowEmptyState(): Boolean {
        // don't show "no posts" before the first update unless an update is running or won't happen
        if (isSearch) {
            // the search screen shows suggestions until a query is submitted
            return !searchQuery.isNullOrEmpty() && posts.isEmpty() && (updateActions.isNotEmpty() || isLoaded)
        }
        val canShowEmpty = updateActions.isNotEmpty() || (isLoaded && (hasUpdatedPosts || autoUpdateSkipped))
        return isSelfHostedCta() || (posts.isEmpty() && canShowEmpty)
    }

    private fun updateUiState() {
        val emptyState = buildEmptyState()
        val isOnlyLoadingOlder = updateActions == setOf(UpdateAction.REQUEST_OLDER)
        _uiState.value = ReaderPostListUiState(
            items = buildItems(),
            isRefreshing = isPullRefreshing || (updateActions.isNotEmpty() && !isOnlyLoadingOlder),
            isLoadingMore = isOnlyLoadingOlder,
            isUpdating = updateActions.isNotEmpty(),
            newPostsBarVisible = isNewPostsBarVisible,
            emptyState = emptyState,
            scrollToPosition = scrollToPosition
        )
    }

    private fun sendUiEvent(event: ReaderPostListUiEvent) {
        // DataLoaded only updates what's on screen, so don't queue it up while the fragment is stopped - the
        // old fragment didn't receive it then either, and it reloads when it comes back
        if (isViewStarted || event !is ReaderPostListUiEvent.DataLoaded) {
            _uiEvents.trySend(event)
        }
    }

    private fun findPost(blogId: Long, postId: Long) =
        posts.firstOrNull { it.blogId == blogId && it.postId == postId }

    private fun ReaderPost.hasSameIds(other: ReaderPost) = blogId == other.blogId && postId == other.postId

    private fun onCardButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        val post = findPost(blogId, postId) ?: return
        closeMoreMenu()
        sendUiEvent(ReaderPostListUiEvent.PostButtonClicked(post, type))
    }

    private fun onCardClicked(postId: Long, blogId: Long) {
        findPost(blogId, postId)?.let { sendUiEvent(ReaderPostListUiEvent.PostSelected(it)) }
    }

    private fun onCardRendered(state: ReaderCardUiState) {
        val postState = state as? ReaderPostUiState ?: return
        val post = findPost(postState.blogId, postState.postId) ?: return
        // record the TrainTracks render event the first time a post with a "railcar" is shown
        if (post.hasRailcar() && renderedIds.add(post.pseudoId)) {
            readerTracker.trackRailcar(post.railcarJson)
        }
    }

    private fun onMoreButtonClicked(state: ReaderPostUiState) {
        readerTracker.track(AnalyticsTracker.Stat.POST_CARD_MORE_TAPPED)
        val post = findPost(state.blogId, state.postId) ?: return
        launch {
            val items = moreMenuBuilder.buildMoreMenuItems(post, true, cardButtonClicked)
            moreMenu = post to items
            updateUiState()
        }
    }

    private fun closeMoreMenu() {
        if (moreMenu != null) {
            moreMenu = null
            updateUiState()
        }
    }

    private fun onVideoOverlayClicked(postId: Long, blogId: Long) {
        findPost(blogId, postId)?.let { sendUiEvent(ReaderPostListUiEvent.VideoOverlayClicked(it)) }
    }

    private fun onPostHeaderClicked(postId: Long, blogId: Long) {
        findPost(blogId, postId)?.let { sendUiEvent(ReaderPostListUiEvent.PostHeaderClicked(it)) }
    }

    private fun onFollowStatusChanged(data: FollowStatusChanged) {
        val changed = posts.filter { it.blogId == data.blogId && it.isFollowedByCurrentUser != data.following }
        if (changed.isEmpty()) return
        changed.forEach { it.isFollowedByCurrentUser = data.following }
        // the site header shows the follow state too, so make it rebind (and re-read it)
        postsVersion++
        val postsSnapshot = posts
        launch {
            val newCardStates = buildCardStates(postsSnapshot)
            // a reload may have replaced the posts while the cards were being built
            if (posts === postsSnapshot) {
                cardStates = newCardStates
                updateUiState()
            }
        }
    }

    /**
     * Handles reblog button action
     *
     * @param post post to reblog
     */
    fun onReblogButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch {
            readerPostCardActionsHandler.onAction(
                post,
                REBLOG,
                bookmarksList,
                source = source
            )
        }
    }

    fun onBlockSiteButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch {
            readerPostCardActionsHandler.onAction(
                post,
                BLOCK_SITE,
                bookmarksList,
                source = source
            )
        }
    }

    fun onBlockUserButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch {
            readerPostCardActionsHandler.onAction(
                post,
                BLOCK_USER,
                bookmarksList,
                source = source
            )
        }
    }

    fun onBookmarkButtonClicked(
        blogId: Long,
        postId: Long,
        isBookmarkList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            ReaderPostTable.getBlogPost(blogId, postId, true)?.let {
                readerPostCardActionsHandler.onAction(
                    it,
                    BOOKMARK,
                    isBookmarkList,
                    source = source
                )
            }
        }
    }

    fun onFollowSiteClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            readerPostCardActionsHandler.onAction(
                post,
                FOLLOW,
                bookmarksList,
                source = source
            )
        }
    }

    fun onSiteNotificationMenuClicked(
        blogId: Long,
        postId: Long,
        isBookmarkList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            ReaderPostTable.getBlogPost(blogId, postId, true)?.let {
                readerPostCardActionsHandler.onAction(
                    it,
                    SITE_NOTIFICATIONS,
                    isBookmarkList,
                    source = source
                )
            }
        }
    }

    fun onLikeButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            readerPostCardActionsHandler.onAction(
                post,
                LIKE,
                bookmarksList,
                source = source
            )
        }
    }

    fun onReportPostButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            readerPostCardActionsHandler.onAction(
                post,
                REPORT_POST,
                bookmarksList,
                source = source
            )
        }
    }

    fun onReportUserButtonClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            readerPostCardActionsHandler.onAction(
                post,
                REPORT_USER,
                bookmarksList,
                source = source
            )
        }
    }

    fun onToggleSeenStatusClicked(
        post: ReaderPost,
        bookmarksList: Boolean,
        source: String
    ) {
        launch(bgDispatcher) {
            readerPostCardActionsHandler.onAction(
                post,
                TOGGLE_SEEN_STATUS,
                bookmarksList,
                source = source
            )
        }
    }

    fun onExternalPostOpened(post: ReaderPost) {
        launch(bgDispatcher) {
            seenStatusToggleUseCase.markPostAsSeenIfNecessary(post)
        }
    }

    /**
     * Handles site selection
     *
     * @param site selected site to reblog to
     */
    fun onReblogSiteSelected(siteLocalId: Int) {
        launch {
            val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
            val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
            if (navigationTarget != null) {
                _navigationEvents.postValue(Event(navigationTarget))
            } else {
                _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.reader_reblog_error))))
            }
            pendingReblogPost = null
        }
    }

    // TODO this is related to tracking time spent in reader -
    //  we should move it to the parent but also keep it here for !isTopLevel :(
    fun onFragmentResume(
        isTopLevelFragment: Boolean,
        isSearch: Boolean,
        isFilterable: Boolean,
        subfilterListItem: SubfilterListItem?
    ) {
        AppLog.d(
            T.READER,
            "TRACK READER ReaderPostListFragment > START Count [mIsTopLevel = $isTopLevelFragment]"
        )
        if (!isTopLevelFragment && !isSearch) {
            // top level is tracked in ReaderFragment, search is tracked in ReaderSearchActivity
            readerTracker.start(ReaderTrackerType.FILTERED_LIST)
        }
        // TODO check if the subfilter is set to a value and uncomment this code

        if (isFilterable && subfilterListItem?.isTrackedItem == true) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > START Count SUBFILTERED_LIST")
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }

    fun onFragmentPause(isTopLevelFragment: Boolean, isSearch: Boolean, isFilterable: Boolean) {
        AppLog.d(
            T.READER,
            "TRACK READER ReaderPostListFragment > STOP Count [mIsTopLevel = $isTopLevelFragment]"
        )
        if (!isTopLevelFragment && !isSearch) {
            // top level is tracked in ReaderFragment, search is tracked in ReaderSearchActivity
            readerTracker.stop(ReaderTrackerType.FILTERED_LIST)
        }

        if (isFilterable) {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }
}
