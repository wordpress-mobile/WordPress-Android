package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.subfilter.SubfilterCategory
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterListItemMapper
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class ReaderPostListViewModel @Inject constructor(
    private val newsManager: NewsManager,
    private val newsTracker: NewsTracker,
    private val newsTrackerHelper: NewsTrackerHelper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val subfilterListItemMapper: SubfilterListItemMapper
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

    private val _shouldCollapseToolbar = MutableLiveData<Boolean>()
    val shouldCollapseToolbar: LiveData<Boolean> = _shouldCollapseToolbar

    private val _filtersMatchCount = MutableLiveData<HashMap<SubfilterCategory, Int>>()
    val filtersMatchCount: LiveData<HashMap<SubfilterCategory, Int>> = _filtersMatchCount

    /**
     * First tag for which the card was shown.
     */
    private var initialTag: ReaderTag? = null
    private var isStarted = false
    private var isFirstLoad = true

    /**
     * Tag may be null for Blog previews for instance.
     */
    fun start(tag: ReaderTag?, shouldShowSubfilter: Boolean, collapseToolbar: Boolean) {
        if (isStarted) {
            return
        }
        tag?.let {
            onTagChanged(tag)
            newsManager.pull()

            updateSubfilter(getCurrentSubfilterValue())
            _shouldShowSubFilters.value = shouldShowSubfilter
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
                        isSelected = (getCurrentSubfilterValue() is Site) &&
                                (getCurrentSubfilterValue() as Site).blog.isSameAs(blog)
                ))
            }

            val tags = ReaderTagTable.getFollowedTags()

            for (tag in tags) {
                filterList.add(Tag(
                        onClickAction = ::onSubfilterClicked,
                        tag = tag,
                        isSelected = (getCurrentSubfilterValue() is Tag) &&
                                (getCurrentSubfilterValue() as Tag).tag == tag
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

        updateSubfilter(filter)
    }

    fun setSubfiltersVisibility(show: Boolean) = _shouldShowSubFilters.postValue(show)

    fun getCurrentSubfilterValue(): SubfilterListItem {
        return if (!BuildConfig.INFORMATION_ARCHITECTURE_AVAILABLE) {
            _currentSubFilter.value ?: SiteAll(
                    onClickAction = ::onSubfilterClicked,
                    isSelected = true)
        } else {
            _currentSubFilter.value ?: appPrefsWrapper.getReaderSubfilter().let {
                subfilterListItemMapper.fromJson(
                        json = it,
                        onClickAction = ::onSubfilterClicked,
                        isSelected = true
                )
            }
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

    fun setCollapseToolbar(collapse: Boolean) {
        _shouldCollapseToolbar.value = collapse
    }

    fun updateTabTitle(category: SubfilterCategory, count: Int) {
        val currentValue = _filtersMatchCount.value

        currentValue?.let {
            it.put(category, count)
        }

        _filtersMatchCount.postValue(currentValue)
    }

    private fun updateSubfilter(filter: SubfilterListItem) {
        _currentSubFilter.postValue(filter)
        val json = subfilterListItemMapper.toJson(filter)
        appPrefsWrapper.setReaderSubfilter(json)
    }

    override fun onCleared() {
        super.onCleared()
        newsManager.stop()
    }
}
