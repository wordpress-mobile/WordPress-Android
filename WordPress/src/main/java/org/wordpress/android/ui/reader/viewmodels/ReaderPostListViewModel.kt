package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.usecases.PreLoadPostContent
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderPostListViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val reblogUseCase: ReblogUseCase,
    private val readerTracker: ReaderTracker
) : ViewModel() {
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

    fun start(readerViewModel: ReaderViewModel?) {
        this.readerViewModel = readerViewModel

        if (isStarted) {
            return
        }
        isStarted = true

        init()
    }

    private fun init() {
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

    /**
     * Handles reblog button action
     *
     * @param post post to reblog
     */
    fun onReblogButtonClicked(post: ReaderPost, bookmarksList: Boolean) {
        readerPostCardActionsHandler.onAction(post, REBLOG, bookmarksList)
    }

    fun onBookmarkButtonClicked(blogId: Long, postId: Long, isBookmarkList: Boolean) {
        val post = ReaderPostTable.getBlogPost(blogId, postId, true)
        readerPostCardActionsHandler.onAction(post, BOOKMARK, isBookmarkList)
    }

    /**
     * Handles site selection
     *
     * @param site selected site to reblog to
     */
    fun onReblogSiteSelected(siteLocalId: Int) {
        val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
        val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
        if (navigationTarget != null) {
            _navigationEvents.postValue(Event(navigationTarget))
        } else {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(R.string.reader_reblog_error)))
        }
        pendingReblogPost = null
    }

    fun onEmptyStateButtonTapped(tag: ReaderTag) {
        readerViewModel?.selectedTabChange(tag)
    }

    // TODO this is related to tracking time spent in reader - we should move it to the parent but also keep it here for !isTopLevel :(
    fun onFragmentResume(
        isTopLevelFragment: Boolean,
        isSearch: Boolean,
        isFollowing: Boolean,
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

        if (isFollowing && subfilterListItem?.isTrackedItem == true) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > START Count SUBFILTERED_LIST")
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }

    fun onFragmentPause(isTopLevelFragment: Boolean, isSearch: Boolean, isFollowing: Boolean) {
        AppLog.d(
                T.READER,
                "TRACK READER ReaderPostListFragment > STOP Count [mIsTopLevel = $isTopLevelFragment]"
        )
        if (!isTopLevelFragment && !isSearch) {
            // top level is tracked in ReaderFragment, search is tracked in ReaderSearchActivity
            readerTracker.stop(ReaderTrackerType.FILTERED_LIST)
        }

        if (isFollowing) {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }
}
