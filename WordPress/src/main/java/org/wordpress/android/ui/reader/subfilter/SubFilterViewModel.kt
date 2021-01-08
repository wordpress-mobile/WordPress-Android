package org.wordpress.android.ui.reader.subfilter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.Organization
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.SITES
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory.TAGS
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class SubFilterViewModel @Inject constructor(
    private val readerTracker: ReaderTracker
) : ViewModel() {
    private val _currentSubFilter = MutableLiveData<SubfilterListItem>()
    val currentSubFilter: LiveData<SubfilterListItem> = _currentSubFilter

    private val _readerModeInfo = SingleLiveEvent<ReaderModeInfo>()
    val readerModeInfo: LiveData<ReaderModeInfo> = _readerModeInfo

    private var isStarted = false
    private var isFirstLoad = true

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(tag: ReaderTag?, organization: Organization) {
        if (isStarted) {
            return
        }

        isStarted = true

        tag?.let {
            updateSubfilter(getCurrentSubfilterValue(organization))
            initSubfiltersTracking(tag.isFilterable, organization)
        }

        isStarted = true
    }

    fun initSubfiltersTracking(show: Boolean, organization: Organization) {
        val isCurrentSubfilterTracked = getCurrentSubfilterValue(organization).isTrackedItem

        if (show) {
            if (isCurrentSubfilterTracked) {
                readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
            } else {
                readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
            }
        } else {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }

    fun getCurrentSubfilterValue(organization: Organization): SubfilterListItem {
        return _currentSubFilter.value ?: SiteAll(
                isSelected = true,
                organization = organization
        )
    }

    fun getCurrentSubfilterPage(organization: Organization): Int {
        return when (getCurrentSubfilterValue(organization)) {
            is Tag -> TAGS.ordinal
            else -> SITES.ordinal
        }
    }

    fun setSubfilter(filter: SubfilterListItem) {
        updateSubfilter(filter)
    }

    private fun changeSubfilter(
        subfilterListItem: SubfilterListItem,
        requestNewerPosts: Boolean,
        initialTag: ReaderTag?
    ) {
        when (subfilterListItem.type) {
            SubfilterListItem.ItemType.SECTION_TITLE,
            SubfilterListItem.ItemType.DIVIDER -> {
                // nop
            }
            SubfilterListItem.ItemType.SITE_ALL -> _readerModeInfo.value = (ReaderModeInfo(
                    initialTag ?: ReaderUtils.getDefaultTag(),
                    ReaderPostListType.TAG_FOLLOWED,
                    0,
                    0,
                    requestNewerPosts,
                    subfilterListItem.label,
                    isFirstLoad,
                    false
            ))
            SubfilterListItem.ItemType.SITE -> {
                val currentFeedId = (subfilterListItem as Site).blog.feedId
                val currentBlogId = if (subfilterListItem.blog.hasFeedUrl())
                    currentFeedId
                else
                    subfilterListItem.blog.blogId

                _readerModeInfo.value = (ReaderModeInfo(
                        null,
                        ReaderPostListType.BLOG_PREVIEW,
                        currentBlogId,
                        currentFeedId,
                        requestNewerPosts,
                        subfilterListItem.label,
                        isFirstLoad,
                        true
                ))
            }
            SubfilterListItem.ItemType.TAG -> _readerModeInfo.value = (ReaderModeInfo(
                    (subfilterListItem as Tag).tag,
                    ReaderPostListType.TAG_FOLLOWED,
                    0,
                    0,
                    requestNewerPosts,
                    subfilterListItem.label,
                    isFirstLoad,
                    true
            ))
        }
        isFirstLoad = false
    }

    fun onSubfilterSelected(subfilterListItem: SubfilterListItem, initialTag: ReaderTag?) {
        changeSubfilter(subfilterListItem, true, initialTag)
    }

    fun onSubfilterReselected(organization: Organization, initialTag: ReaderTag?) {
        changeSubfilter(getCurrentSubfilterValue(organization), false, initialTag)
    }

    private fun updateSubfilter(filter: SubfilterListItem) {
        if (filter.isTrackedItem) {
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        } else {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
        _currentSubFilter.value = filter
    }

    companion object {
        const val SUBFILTER_VM_BASE_KEY = "SUBFILTER_VIEW_MODEL_BASE_KEY"
    }
}
