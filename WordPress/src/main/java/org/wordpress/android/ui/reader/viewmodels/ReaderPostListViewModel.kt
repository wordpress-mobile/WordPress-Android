package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.reblog.NoSite
import org.wordpress.android.ui.reader.reblog.PostEditor
import org.wordpress.android.ui.reader.reblog.ReblogState
import org.wordpress.android.ui.reader.reblog.SitePicker
import org.wordpress.android.ui.reader.reblog.Unknown
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderPostListViewModel @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val siteStore: SiteStore
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
        val sites = siteStore.visibleSitesAccessedViaWPCom

        _reblogState.value = when (sites.count()) {
            0 -> Event(NoSite)
            1 -> {
                sites.firstOrNull()?.let {
                    Event(PostEditor(it, post))
                } ?: Event(Unknown)
            }
            else -> {
                sites.firstOrNull()?.let {
                    Event(SitePicker(it, post))
                } ?: Event(Unknown)
            }
        }
    }

    /**
     * Handles site selection
     *
     * @param site selected site to reblog to
     */
    fun onReblogSiteSelected(siteLocalId: Int) {
        val currentState: ReblogState? = _reblogState.value?.peekContent()
        if (currentState is SitePicker) {
            val site: SiteModel? = siteStore.getSiteByLocalId(siteLocalId)
            _reblogState.value = if (site != null) Event(PostEditor(site, currentState.post)) else Event(Unknown)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Site Selected without passing the SitePicker state")
        } else {
            AppLog.e(T.READER, "Site Selected without passing the SitePicker state")
            _reblogState.value = Event(Unknown)
        }
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
        if(!isTopLevelFragment && !isSearch) {
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
        if(!isTopLevelFragment && !isSearch) {
            // top level is tracked in ReaderFragment, search is tracked in ReaderSearchActivity
            readerTracker.stop(ReaderTrackerType.FILTERED_LIST)
        }

        if (isFollowing) {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }
}
