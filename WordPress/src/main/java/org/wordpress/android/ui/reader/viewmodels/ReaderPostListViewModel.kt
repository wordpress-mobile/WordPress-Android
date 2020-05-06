package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.reblog.NoSite
import org.wordpress.android.ui.reader.reblog.PostEditor
import org.wordpress.android.ui.reader.reblog.Unknown
import org.wordpress.android.ui.reader.reblog.ReblogState
import org.wordpress.android.ui.reader.reblog.SitePicker
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItemMapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named

class ReaderPostListViewModel @Inject constructor(
    private val newsManager: NewsManager,
    private val newsTracker: NewsTracker,
    private val newsTrackerHelper: NewsTrackerHelper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val subfilterListItemMapper: SubfilterListItemMapper,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore,
    private val readerTracker: ReaderTracker,
    private val siteStore: SiteStore
) : ScopedViewModel(bgDispatcher) {
    private val newsItemSource = newsManager.newsItemSource()
    private val _newsItemSourceMediator = MediatorLiveData<NewsItem>()

    private val onTagChanged: Observer<NewsItem?> = Observer { _newsItemSourceMediator.value = it }

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

    private val _shouldCollapseToolbar = MutableLiveData<Boolean>()
    val shouldCollapseToolbar: LiveData<Boolean> = _shouldCollapseToolbar

    private val _filtersMatchCount = MutableLiveData<HashMap<SubfilterCategory, Int>>()
    val filtersMatchCount: LiveData<HashMap<SubfilterCategory, Int>> = _filtersMatchCount

    private val _bottomSheetEmptyViewAction = MutableLiveData<Event<ActionType>>()
    val bottomSheetEmptyViewAction: LiveData<Event<ActionType>> = _bottomSheetEmptyViewAction

    private val _updateTagsAndSites = MutableLiveData<Event<EnumSet<UpdateTask>>>()
    val updateTagsAndSites: LiveData<Event<EnumSet<UpdateTask>>> = _updateTagsAndSites

    private val _reblogState = MutableLiveData<Event<ReblogState>>()
    val reblogState: LiveData<Event<ReblogState>> = _reblogState

    /**
     * First tag for which the card was shown.
     */
    private var initialTag: ReaderTag? = null
    private var isStarted = false
    private var isFirstLoad = true

    private var lastKnownUserId: Long? = null
    private var lastTokenAvailableStatus: Boolean? = null

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(tag: ReaderTag?, shouldShowSubfilter: Boolean, collapseToolbar: Boolean) {
        if (isStarted) {
            return
        }

        eventBusWrapper.register(this)

        tag?.let {
            onTagChanged(tag)
            newsManager.pull()

            updateSubfilter(getCurrentSubfilterValue())
            changeSubfiltersVisibility(shouldShowSubfilter)
        }

        _shouldCollapseToolbar.value = collapseToolbar
        _filtersMatchCount.value = hashMapOf()

        isStarted = true
    }

    fun getNewsDataSource(): LiveData<NewsItem> {
        return _newsItemSourceMediator
    }

    fun onTagChanged(tag: ReaderTag?) {
        newsTrackerHelper.reset()
        tag?.let { newTag ->
            // show the card only when the initial tag is selected in the filter
            if (initialTag == null || newTag == initialTag) {
                _newsItemSourceMediator.addSource(newsItemSource, onTagChanged)
            } else {
                _newsItemSourceMediator.removeSource(newsItemSource)
                _newsItemSourceMediator.value = null
            }
        }
    }

    fun onNewsCardDismissed(item: NewsItem) {
        newsTracker.trackNewsCardDismissed(READER, item.version)
        newsManager.dismiss(item)
    }

    fun onNewsCardShown(
        item: NewsItem,
        currentTag: ReaderTag
    ) {
        initialTag = currentTag
        if (newsTrackerHelper.shouldTrackNewsCardShown(item.version)) {
            newsTracker.trackNewsCardShown(READER, item.version)
            newsTrackerHelper.itemTracked(item.version)
        }
        newsManager.cardShown(item)
    }

    fun onNewsCardExtendedInfoRequested(item: NewsItem) {
        newsTracker.trackNewsCardExtendedInfoRequested(READER, item.version)
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
        val isSubfilterListTrackerRunning = readerTracker.isRunning(ReaderTrackerType.SUBFILTERED_LIST)

        if (show) {
            if (isCurrentSubfilterTracked && !isSubfilterListTrackerRunning) {
                AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > START Count SUBFILTERED_LIST")
                readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
            } else if (!isCurrentSubfilterTracked && isSubfilterListTrackerRunning) {
                AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > STOP Count SUBFILTERED_LIST")
                readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
            }
        } else if (isSubfilterListTrackerRunning) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > STOP Count SUBFILTERED_LIST")
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
        val isSubfilterItemTracked = subfilterListItem.isTrackedItem
        val isSubfilterListTrackerRunning = readerTracker.isRunning(ReaderTrackerType.SUBFILTERED_LIST)

        if (isSubfilterItemTracked && !isSubfilterListTrackerRunning) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > START Count SUBFILTERED_LIST")
            readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
        } else if (!isSubfilterItemTracked && isSubfilterListTrackerRunning) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > STOP Count SUBFILTERED_LIST")
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

    fun onSearchMenuCollapse(collapse: Boolean) {
        _shouldCollapseToolbar.value = collapse
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

    fun onFragmentResume(isTopLevelFragment: Boolean, isFollowingTag: Boolean) {
        AppLog.d(
                T.READER,
                "TRACK READER ReaderPostListFragment > START Count [mIsTopLevel = $isTopLevelFragment]"
        )
        readerTracker.start(
                if (isTopLevelFragment) ReaderTrackerType.MAIN_READER else ReaderTrackerType.FILTERED_LIST
        )

        if (isTopLevelFragment) {
            if (isFollowingTag && getCurrentSubfilterValue().isTrackedItem) {
                AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > START Count SUBFILTERED_LIST")
                readerTracker.start(ReaderTrackerType.SUBFILTERED_LIST)
            }
        }
    }

    fun onFragmentPause(isTopLevelFragment: Boolean) {
        AppLog.d(
                T.READER,
                "TRACK READER ReaderPostListFragment > STOP Count [mIsTopLevel = $isTopLevelFragment]"
        )
        readerTracker.stop(
                if (isTopLevelFragment) ReaderTrackerType.MAIN_READER else ReaderTrackerType.FILTERED_LIST
        )

        if (isTopLevelFragment && readerTracker.isRunning(ReaderTrackerType.SUBFILTERED_LIST)) {
            AppLog.d(T.READER, "TRACK READER ReaderPostListFragment > STOP Count SUBFILTERED_LIST")
            readerTracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }
    }

    private fun updateSubfilter(filter: SubfilterListItem) {
        _currentSubFilter.value = filter
        val json = subfilterListItemMapper.toJson(filter)
        appPrefsWrapper.setReaderSubfilter(json)
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

    fun onUserComesToReader() {
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

    override fun onCleared() {
        super.onCleared()
        eventBusWrapper.unregister(this)
        newsManager.stop()
    }
}
