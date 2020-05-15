package org.wordpress.android.ui.reader.subfilter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EventBusWrapper
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
    private val readerTracker: ReaderTracker
) : ScopedViewModel(bgDispatcher) {
    private val _subFilters = MutableLiveData<List<SubfilterListItem>>()
    val subFilters: LiveData<List<SubfilterListItem>> = _subFilters

    private val _currentSubFilter = MutableLiveData<SubfilterListItem>()
    val currentSubFilter: LiveData<SubfilterListItem> = _currentSubFilter

    private val _shouldShowSubFilters = MutableLiveData<Boolean>()
    val shouldShowSubFilters: LiveData<Boolean> = _shouldShowSubFilters

    private val _readerModeInfo = SingleLiveEvent<ReaderModeInfo>()
    val readerModeInfo: LiveData<ReaderModeInfo> = _readerModeInfo

    private val _changeBottomSheetVisibility = MutableLiveData<Event<Boolean>>()
    val changeBottomSheetVisibility: LiveData<Event<Boolean>> = _changeBottomSheetVisibility

    private val _filtersMatchCount = MutableLiveData<HashMap<SubfilterCategory, Int>>()
    val filtersMatchCount: LiveData<HashMap<SubfilterCategory, Int>> = _filtersMatchCount

    private val _bottomSheetEmptyViewAction = MutableLiveData<Event<ActionType>>()
    val bottomSheetEmptyViewAction: LiveData<Event<ActionType>> = _bottomSheetEmptyViewAction

    private val _updateTagsAndSites = MutableLiveData<Event<EnumSet<UpdateTask>>>()
    val updateTagsAndSites: LiveData<Event<EnumSet<UpdateTask>>> = _updateTagsAndSites

    private var lastKnownUserId: Long? = null
    private var lastTokenAvailableStatus: Boolean? = null

    private var isStarted = false
    private var isFirstLoad = true

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(tag: ReaderTag?) {
        if (isStarted) {
            return
        }

        eventBusWrapper.register(this)

        tag?.let {
            updateSubfilter(getCurrentSubfilterValue())
            changeSubfiltersVisibility(tag.isFollowedSites)
        }

        _filtersMatchCount.value = hashMapOf()

        isStarted = true
    }

    fun loadSubFilters() {
        launch {
            val filterList = ArrayList<SubfilterListItem>()

            if (accountStore.hasAccessToken()) {
                // Filtering Discover out
                val followedBlogs = ReaderBlogTable.getFollowedBlogs().let { blogList ->
                    blogList.filter { blog ->
                        !(blog.url.startsWith("https://discover.wordpress.com"))
                    }
                }

                for (blog in followedBlogs) {
                    filterList.add(Site(
                            onClickAction = ::onSubfilterClicked,
                            blog = blog,
                            isSelected = false
                    ))
                }
            }

            val tags = ReaderTagTable.getFollowedTags()

            for (tag in tags) {
                filterList.add(Tag(
                        onClickAction = ::onSubfilterClicked,
                        tag = tag,
                        isSelected = false
                ))
            }

            withContext(mainDispatcher) {
                _subFilters.value = filterList
            }
        }
    }

    private fun onSubfilterClicked(filter: SubfilterListItem) {
        _changeBottomSheetVisibility.postValue(Event(false))
        updateSubfilter(filter)
    }

    fun changeSubfiltersVisibility(show: Boolean) {
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

        _shouldShowSubFilters.postValue(show)
    }

    fun getCurrentSubfilterValue(): SubfilterListItem {
        return _currentSubFilter.value ?: appPrefsWrapper.getReaderSubfilter().let {
            subfilterListItemMapper.fromJson(
                    json = it,
                    onClickAction = ::onSubfilterClicked,
                    isSelected = true
            )
        }
    }

    fun setSubfilterFromTag(tag: ReaderTag) {
        updateSubfilter(
                Tag(
                        onClickAction = ::onSubfilterClicked,
                        tag = tag,
                        isSelected = true
                ))
    }

    fun setDefaultSubfilter() {
        updateSubfilter(
                SiteAll(
                        onClickAction = ::onSubfilterClicked,
                        isSelected = true
                ))
    }

    fun onSubFiltersListButtonClicked() {
        _updateTagsAndSites.value = Event(EnumSet.of(
                UpdateTask.TAGS,
                UpdateTask.FOLLOWED_BLOGS
        ))
        _changeBottomSheetVisibility.value = Event(true)
    }

    fun onBottomSheetCancelled() {
        _changeBottomSheetVisibility.value = Event(false)
    }

    private fun changeSubfilter(
        subfilterListItem: SubfilterListItem,
        requestNewerPosts: Boolean
    ) {
        if (subfilterListItem.isTrackedItem) {
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        } else {
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }

        when (subfilterListItem.type) {
            SubfilterListItem.ItemType.SECTION_TITLE,
            SubfilterListItem.ItemType.DIVIDER -> {
                // nop
            }
            SubfilterListItem.ItemType.SITE_ALL -> _readerModeInfo.value = (ReaderModeInfo(
                    ReaderUtils.getDefaultTag(),
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

    fun onSubfilterSelected(subfilterListItem: SubfilterListItem) {
        changeSubfilter(subfilterListItem, true)
    }

    fun onSubfilterReselected() {
        changeSubfilter(getCurrentSubfilterValue(), false)
    }

    fun onSubfilterPageUpdated(category: SubfilterCategory, count: Int) {
        val currentValue = _filtersMatchCount.value

        currentValue?.let {
            it.put(category, count)
        }

        _filtersMatchCount.postValue(currentValue)
    }

    fun onBottomSheetActionClicked(action: ActionType) {
        _changeBottomSheetVisibility.postValue(Event(false))
        _bottomSheetEmptyViewAction.postValue(Event(action))
    }

    private fun updateSubfilter(filter: SubfilterListItem) {
        _currentSubFilter.value = filter
        val json = subfilterListItemMapper.toJson(filter)
        appPrefsWrapper.setReaderSubfilter(json)
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
            _updateTagsAndSites.value = Event(EnumSet.of(
                    UpdateTask.TAGS,
                    UpdateTask.FOLLOWED_BLOGS
            ))

            setDefaultSubfilter()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedTagsChanged) {
        AppLog.d(T.READER, "Subfilter bottom sheet > followed tags changed")
        loadSubFilters()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedBlogsChanged) {
        AppLog.d(T.READER, "Subfilter bottom sheet > followed blogs changed")
        loadSubFilters()
    }

    override fun onCleared() {
        super.onCleared()
        eventBusWrapper.unregister(this)
    }
}
