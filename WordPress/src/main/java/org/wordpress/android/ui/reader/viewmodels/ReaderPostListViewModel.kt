package org.wordpress.android.ui.reader.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Divider
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SectionTitle
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtils
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
    private val accountStore: AccountStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
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

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    /**
     * First tag for which the card was shown.
     */
    private var initialTag: ReaderTag? = null
    private var isStarted = false
    private var isFirstLoad = true
    private var lastKnownAccount: AccountModel? = null
    private var lastTokenAvailableStatus: Boolean? = null

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(tag: ReaderTag?, shouldShowSubfilter: Boolean) {
        if (isStarted) {
            return
        }
        tag?.let {
            onTagChanged(tag)
            newsManager.pull()

            _currentSubFilter.value = getCurrentSubfilterValue()
            _shouldShowSubFilters.value = shouldShowSubfilter
        }
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
                filterList.add(SectionTitle(UiStringRes(R.string.reader_filter_sites_title)))
                filterList.add(
                        SiteAll(
                            label = UiStringRes(R.string.reader_filter_all_sites),
                            onClickAction = ::onSubfilterClicked,
                            isSelected = (getCurrentSubfilterValue() is SiteAll)
                        )
                )

                // Filtering Discover out
                val followedBlogs = ReaderBlogTable.getFollowedBlogs().let { blogList ->
                    blogList.filter { blog ->
                        !(blog.url.startsWith("https://discover.wordpress.com"))
                    }
                }

                for (blog in followedBlogs) {
                    filterList.add(
                            Site(
                                label = if (blog.name.isNotEmpty()) UiStringText(blog.name) else UiStringRes(
                                        R.string.reader_untitled_post
                                ),
                                onClickAction = ::onSubfilterClicked,
                                blog = blog,
                                isSelected = (getCurrentSubfilterValue() is Site) &&
                                        (getCurrentSubfilterValue() as Site).blog.name == blog.name
                            )
                    )
                }

                filterList.add(Divider)
            }

            filterList.add(SectionTitle(UiStringRes(R.string.reader_filter_tags_title)))

            val tags = ReaderTagTable.getFollowedTags()

            for (tag in tags) {
                filterList.add(Tag(
                        label = UiStringText(tag.tagTitle),
                        onClickAction = ::onSubfilterClicked,
                        tag = tag,
                        isSelected = (getCurrentSubfilterValue() is Tag) &&
                                (getCurrentSubfilterValue() as Tag).tag.tagTitle == tag.tagTitle
                ))
            }

            _subFilters.postValue(filterList)
        }
    }

    private fun onSubfilterClicked(filter: SubfilterListItem) {
        _isBottomSheetShowing.postValue(Event(false))

        _subFilters.postValue(_subFilters.value?.map {
            it.isSelected = it.isSameItem(filter)
            it
        })

        _currentSubFilter.postValue(filter)
    }

    fun setSubfiltersVisibility(show: Boolean) = _shouldShowSubFilters.postValue(show)

    fun getCurrentSubfilterValue(): SubfilterListItem {
        return _currentSubFilter.value ?: SiteAll(
                label = UiStringRes(R.string.reader_filter_all_sites),
                onClickAction = ::onSubfilterClicked,
                isSelected = true)
    }

    fun setSubfilterFromTag(tag: ReaderTag) {
        _currentSubFilter.postValue(
                Tag(
                    label = UiStringText(tag.tagTitle),
                    onClickAction = ::onSubfilterClicked,
                    tag = tag,
                    isSelected = true
                ))
    }

    private fun getDefaultLabelResId() = if (accountStore.hasAccessToken())
        R.string.reader_filter_all_sites
    else
        R.string.reader_filter_select_filter

    fun setDefaultSubfilter() {
        _currentSubFilter.postValue(
                SiteAll(
                        label = UiStringRes(getDefaultLabelResId()),
                        onClickAction = ::onSubfilterClicked,
                        isSelected = true
                ))
    }

    fun setIsBottomSheetShowing(showing: Boolean) {
        _isBottomSheetShowing.value = Event(showing)
    }

    fun applySubfilter(
        subfilterListItem: SubfilterListItem,
        requestNewerPosts: Boolean
    ) {
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
                    isFirstLoad
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
                        isFirstLoad
                ))
            }
            SubfilterListItem.ItemType.TAG -> _readerModeInfo.value = (ReaderModeInfo(
                    (subfilterListItem as Tag).tag,
                    ReaderPostListType.TAG_FOLLOWED,
                    0,
                    0,
                    requestNewerPosts,
                    subfilterListItem.label,
                    isFirstLoad
            ))
        }
        isFirstLoad = false
    }

    fun performUpdate(context: Context?, tasks: EnumSet<UpdateTask>) {
        context?.let {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return
            }

            ReaderUpdateServiceStarter.startService(context, tasks)
        }
    }

    fun onUserChangedUpdate(context: Context?) {
        context?.let {
            val accountChanged = lastKnownAccount == null ||
                    (accountStore.account != null && accountStore.account != lastKnownAccount) ||
                    (lastTokenAvailableStatus == null) ||
                    (lastTokenAvailableStatus != null && lastTokenAvailableStatus != accountStore.hasAccessToken())

            if (accountChanged) {
                lastKnownAccount = accountStore.account
                lastTokenAvailableStatus = accountStore.hasAccessToken()

                performUpdate(context, EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS/*,
                        UpdateTask.RECOMMENDED_BLOGS*/)
                )

                setDefaultSubfilter()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        newsManager.stop()
    }
}
