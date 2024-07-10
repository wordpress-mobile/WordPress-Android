package org.wordpress.android.ui.reader.subfilter

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetHidden
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetVisible
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named

class SubFilterViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val subfilterListItemMapper: SubfilterListItemMapper,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore,
    private val readerTracker: ReaderTracker,
    private val readerTagTableWrapper: ReaderTagTableWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
) : ScopedViewModel(bgDispatcher) {
    private val _subFilters = MutableLiveData<List<SubfilterListItem>>()
    val subFilters: LiveData<List<SubfilterListItem>> = _subFilters

    private val _currentSubFilter = MutableLiveData<SubfilterListItem>()
    val currentSubFilter: LiveData<SubfilterListItem> = _currentSubFilter

    private val _readerModeInfo = SingleLiveEvent<ReaderModeInfo>()
    val readerModeInfo: LiveData<ReaderModeInfo> = _readerModeInfo

    private val _bottomSheetUiState = MutableLiveData<Event<BottomSheetUiState>>()
    val bottomSheetUiState: LiveData<Event<BottomSheetUiState>> = _bottomSheetUiState

    private val _bottomSheetAction = MutableLiveData<Event<ActionType>>()
    val bottomSheetAction: LiveData<Event<ActionType>> = _bottomSheetAction

    private val _updateTagsAndSites = MutableLiveData<Event<EnumSet<UpdateTask>>>()
    val updateTagsAndSites: LiveData<Event<EnumSet<UpdateTask>>> = _updateTagsAndSites

    private val _isTitleContainerVisible = MutableLiveData<Boolean>(true)
    val isTitleContainerVisible: LiveData<Boolean> = _isTitleContainerVisible

    private var lastKnownUserId: Long? = null
    private var lastTokenAvailableStatus: Boolean? = null

    private var isStarted = false
    private var isFirstLoad = true
    private var mTagFragmentStartedWith: ReaderTag? = null

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(mTagFragmentStartedWith: ReaderTag?, tag: ReaderTag?, savedInstanceState: Bundle?) {
        if (isStarted) {
            return
        }

        isStarted = true

        this.mTagFragmentStartedWith = mTagFragmentStartedWith

        var subfilterJson: String? = null

        savedInstanceState?.let {
            isFirstLoad = it.getBoolean(ARG_IS_FIRST_LOAD)
            subfilterJson = it.getString(ARG_CURRENT_SUBFILTER_JSON)
        }

        eventBusWrapper.register(this)

        tag?.let {
            val currentSubfilter = subfilterJson?.let { json ->
                subfilterListItemMapper.fromJson(
                    json = json,
                    onClickAction = ::onSubfilterClicked,
                    isSelected = true
                )
            }
            updateSubfilter(currentSubfilter ?: getCurrentSubfilterValue())
            initSubfiltersTracking(tag.isFilterable)
        }
    }

    private fun getBlogNameForComparison(blog: ReaderBlog?): String {
        return if (blog == null) {
            ""
        } else if (blog.hasName()) {
            blog.name
        } else if (blog.hasUrl()) {
            StringUtils.notNullStr(UrlUtils.getHost(blog.url))
        } else {
            ""
        }
    }

    fun loadSubFilters() {
        launch {
            val filterList = ArrayList<SubfilterListItem>()

            if (accountStore.hasAccessToken()) {
                val organization = mTagFragmentStartedWith?.organization

                val followedBlogs = readerBlogTableWrapper.getFollowedBlogs().let { blogList ->
                    // Filtering out all blogs not belonging to this VM organization if valid
                    blogList.filter { blog ->
                        organization?.let {
                            blog.organizationId == organization.orgId
                        } ?: false
                    }
                }.sortedWith { blog1, blog2 ->
                    // sort followed blogs by name/domain to match display
                    val blogOneName = getBlogNameForComparison(blog1)
                    val blogTwoName = getBlogNameForComparison(blog2)
                    blogOneName.compareTo(blogTwoName, true)
                }

                filterList.addAll(
                    followedBlogs.map { blog ->
                        Site(
                            onClickAction = ::onSubfilterClicked,
                            blog = blog,
                            isSelected = false
                        )
                    }
                )
            }

            val tags = readerTagTableWrapper.getFollowedTags()

            for (tag in tags) {
                filterList.add(
                    Tag(
                        onClickAction = ::onSubfilterClicked,
                        tag = tag,
                        isSelected = false
                    )
                )
            }

            withContext(mainDispatcher) {
                _subFilters.value = filterList
            }
        }
    }

    private fun onSubfilterClicked(filter: SubfilterListItem) {
        _bottomSheetUiState.postValue(Event(BottomSheetHidden))
        updateSubfilter(filter)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun initSubfiltersTracking(show: Boolean) {
        val isCurrentSubfilterTracked = getCurrentSubfilterValue().isTrackedItem

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

    fun getCurrentSubfilterValue(): SubfilterListItem {
        return _currentSubFilter.value ?: SiteAll(
            onClickAction = ::onSubfilterClicked,
            isSelected = true
        )
    }

    private fun getCurrentSubfilterJson(): String {
        return subfilterListItemMapper.toJson(getCurrentSubfilterValue())
    }

    fun setSubfilterFromTag(tag: ReaderTag) {
        updateSubfilter(
            Tag(
                onClickAction = ::onSubfilterClicked,
                tag = tag,
                isSelected = true
            )
        )
    }

    fun setDefaultSubfilter(isClearingFilter: Boolean) {
        val filterItemType = FilterItemType.fromSubfilterListItem(getCurrentSubfilterValue())
        if (filterItemType != null) {
            readerTracker.track(
                Stat.READER_FILTER_SHEET_CLEARED,
                mutableMapOf(FilterItemType.trackingEntry(filterItemType))
            )
        } else {
            readerTracker.track(Stat.READER_FILTER_SHEET_CLEARED)
        }
        updateSubfilter(
            filter = SiteAll(
                onClickAction = ::onSubfilterClicked,
                isSelected = true,
                isClearingFilter = isClearingFilter,
            ),
        )
    }

    fun onSubFiltersListButtonClicked(
        category: SubfilterCategory,
    ) {
        updateTagsAndSites()
        loadSubFilters()
        _bottomSheetUiState.value = Event(
            BottomSheetVisible(
                UiStringRes(category.titleRes),
                category
            )
        )
        val source = when (category) {
            SubfilterCategory.SITES -> "blogs"
            SubfilterCategory.TAGS -> "tags"
        }
        readerTracker.track(Stat.READER_FILTER_SHEET_DISPLAYED, source)
    }

    fun updateTagsAndSites() {
        _updateTagsAndSites.value = Event(
            EnumSet.of(
                UpdateTask.TAGS,
                UpdateTask.FOLLOWED_BLOGS
            )
        )
    }

    fun onBottomSheetCancelled() {
        readerTracker.track(Stat.READER_FILTER_SHEET_DISMISSED)
        _bottomSheetUiState.value = Event(BottomSheetHidden)
    }

    private fun changeSubfilter(
        subfilterListItem: SubfilterListItem,
        requestNewerPosts: Boolean,
        streamTag: ReaderTag?
    ) {
        when (subfilterListItem.type) {
            SubfilterListItem.ItemType.SECTION_TITLE,
            SubfilterListItem.ItemType.DIVIDER -> {
                // nop
            }

            SubfilterListItem.ItemType.SITE_ALL -> _readerModeInfo.value = (ReaderModeInfo(
                streamTag ?: ReaderUtils.getDefaultTag(),
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
                val currentBlogId = if (subfilterListItem.blog.hasFeedUrl()) {
                    currentFeedId
                } else {
                    subfilterListItem.blog.blogId
                }

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

    fun onSubfilterSelected(subfilterListItem: SubfilterListItem) {
        // We should only track the selection of a subfilter if it's a tracked item (meaning it's a valid tag or site)
        if (subfilterListItem.isTrackedItem) {
            val filterItemType = FilterItemType.fromSubfilterListItem(subfilterListItem)
            if (filterItemType != null) {
                readerTracker.track(
                    Stat.READER_FILTER_SHEET_ITEM_SELECTED,
                    mutableMapOf(FilterItemType.trackingEntry(filterItemType))
                )
            } else {
                readerTracker.track(Stat.READER_FILTER_SHEET_ITEM_SELECTED)
            }
        }
        changeSubfilter(subfilterListItem, true, mTagFragmentStartedWith)
    }

    fun onSubfilterReselected() {
        changeSubfilter(getCurrentSubfilterValue(), false, mTagFragmentStartedWith)
    }

    fun onBottomSheetActionClicked(action: ActionType) {
        _bottomSheetUiState.postValue(Event(BottomSheetHidden))
        _bottomSheetAction.postValue(Event(action))
    }

    private fun updateSubfilter(filter: SubfilterListItem) {
        if (filter.isTrackedItem) {
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        } else {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
        _currentSubFilter.value = filter
        onSubfilterSelected(filter)
    }

    fun onUserComesToReader() {
        // TODO I think this method could be simplified, I don't think we need to store the data in the sharedPref
        if (lastKnownUserId == null) {
            lastKnownUserId = appPrefsWrapper.getLastReaderKnownUserId()
        }

        if (lastTokenAvailableStatus == null) {
            lastTokenAvailableStatus = appPrefsWrapper.getLastReaderKnownAccessTokenStatus()
        }

        val userIdChanged = accountStore.hasAccessToken() && accountStore.account != null &&
                accountStore.account.userId != lastKnownUserId
        val accessTokenStatusChanged = accountStore.hasAccessToken() != lastTokenAvailableStatus

        if (userIdChanged) {
            lastKnownUserId = accountStore.account.userId
            appPrefsWrapper.setLastReaderKnownUserId(accountStore.account.userId)
        }

        if (accessTokenStatusChanged) {
            lastTokenAvailableStatus = accountStore.hasAccessToken()
            appPrefsWrapper.setLastReaderKnownAccessTokenStatus(accountStore.hasAccessToken())
        }

        if (userIdChanged || accessTokenStatusChanged) {
            _updateTagsAndSites.value = Event(
                EnumSet.of(
                    UpdateTask.TAGS,
                    UpdateTask.FOLLOWED_BLOGS
                )
            )

            setDefaultSubfilter(false)
        }
    }

    fun trackOnPageSelected(tab: String) {
        readerTracker.track(Stat.READER_FILTER_SHEET_TAB_SELECTED, mutableMapOf(TRACK_TAB to tab))
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            ARG_CURRENT_SUBFILTER_JSON, getCurrentSubfilterJson()
        )
        outState.putBoolean(ARG_IS_FIRST_LOAD, isFirstLoad)
    }

    fun setTitleContainerVisibility(isVisible: Boolean) {
        _isTitleContainerVisible.value = isVisible
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedTagsFetched) {
        AppLog.d(T.READER, "Subfilter bottom sheet > followed tags changed")
        loadSubFilters()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedBlogsFetched) {
        if (event.didChange()) {
            AppLog.d(T.READER, "Subfilter bottom sheet > followed blogs changed")
            loadSubFilters()
        }
    }

    override fun onCleared() {
        super.onCleared()
        eventBusWrapper.unregister(this)
    }

    companion object {
        const val SUBFILTER_VM_BASE_KEY = "SUBFILTER_VIEW_MODEL_BASE_KEY"

        const val ARG_CURRENT_SUBFILTER_JSON = "current_subfilter_json"
        const val ARG_IS_FIRST_LOAD = "is_first_load"

        const val TRACK_TAB = "tab"

        @JvmStatic
        fun getViewModelKeyForTag(tag: ReaderTag): String {
            return SUBFILTER_VM_BASE_KEY + tag.keyString
        }
    }

    sealed class FilterItemType(val trackingValue: String) {
        data object Tag : FilterItemType("topic")

        data object Blog : FilterItemType("site")

        companion object {
            fun fromSubfilterListItem(subfilterListItem: SubfilterListItem): FilterItemType? =
                when (subfilterListItem.type) {
                    SubfilterListItem.ItemType.SITE -> Blog
                    SubfilterListItem.ItemType.TAG -> Tag
                    else -> null
                }

            fun trackingEntry(filterItemType: FilterItemType): Pair<String, String> =
                "type" to filterItemType.trackingValue
        }
    }
}
