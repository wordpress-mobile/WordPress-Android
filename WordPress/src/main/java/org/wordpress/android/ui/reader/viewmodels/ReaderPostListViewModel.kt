package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.reblog.ReblogState
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderPostListViewModel @Inject constructor(
    private val reblogUseCase: ReblogUseCase,
    private val readerTracker: ReaderTracker
) : ViewModel() {
    private val _reblogState = MutableLiveData<Event<ReblogState>>()
    val reblogState: LiveData<Event<ReblogState>> = _reblogState

    private var isStarted = false

    private var readerViewModel: ReaderViewModel? = null

    fun start(readerViewModel: ReaderViewModel?) {
        this.readerViewModel = readerViewModel

        if (isStarted) {
            return
        }
        isStarted = true
    }

    /**
     * Handles reblog button action
     *
     * @param post post to reblog
     */
    fun onReblogButtonClicked(post: ReaderPost) {
        _reblogState.value = reblogUseCase.onReblogButtonClicked(post)
    }

    /**
     * Handles site selection
     *
     * @param site selected site to reblog to
     */
    fun onReblogSiteSelected(siteLocalId: Int) {
        _reblogState.value = reblogUseCase.onReblogSiteSelected(siteLocalId, _reblogState.value?.peekContent())
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
