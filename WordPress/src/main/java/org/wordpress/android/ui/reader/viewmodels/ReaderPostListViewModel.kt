package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_POST
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_USER
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.TOGGLE_SEEN_STATUS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderPostListViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val reblogUseCase: ReblogUseCase,
    private val readerTracker: ReaderTracker,
    private val seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase,
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

    private val _refreshPosts = MediatorLiveData<Event<Unit>>()
    val refreshPosts: LiveData<Event<Unit>> = _refreshPosts

    private val _updateFollowStatus = MediatorLiveData<FollowStatusChanged>()
    val updateFollowStatus: LiveData<FollowStatusChanged> = _updateFollowStatus

    fun start(readerViewModel: ReaderViewModel?) {
        this.readerViewModel = readerViewModel

        if (isStarted) {
            return
        }
        isStarted = true

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

        _refreshPosts.addSource(readerPostCardActionsHandler.refreshPosts) { event ->
            _refreshPosts.value = event
        }

        _updateFollowStatus.addSource(readerPostCardActionsHandler.followStatusUpdated) { data ->
            _updateFollowStatus.value = data
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

    fun onEmptyStateButtonTapped(tag: ReaderTag) {
        readerViewModel?.selectedTabChange(tag)
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
