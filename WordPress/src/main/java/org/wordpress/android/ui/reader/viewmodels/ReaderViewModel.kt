package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.InitialUiState
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val UPDATE_TAGS_THRESHOLD = 1000 * 60 * 60

typealias TabPosition = Int

class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider,
    private val loadReaderTabsUseCase: LoadReaderTabsUseCase,
    private val readerTracker: ReaderTracker,
    private val accountStore: AccountStore
) : ScopedViewModel(mainDispatcher) {
    private var initialized: Boolean = false
    private var isReaderInterestsShown: Boolean = false

    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState.distinct()

    private val _updateTags = MutableLiveData<Event<Unit>>()
    val updateTags: LiveData<Event<Unit>> = _updateTags

    private val _selectTab = MutableLiveData<Event<TabPosition>>()
    val selectTab: LiveData<Event<TabPosition>> = _selectTab

    private val _showSearch = MutableLiveData<Event<Unit>>()
    val showSearch: LiveData<Event<Unit>> = _showSearch

    private val _showReaderInterests = MutableLiveData<Event<Unit>>()
    val showReaderInterests: LiveData<Event<Unit>> = _showReaderInterests

    private val _closeReaderInterests = MutableLiveData<Event<Unit>>()
    val closeReaderInterests: LiveData<Event<Unit>> = _closeReaderInterests

    init {
        EventBus.getDefault().register(this)
    }

    fun start() {
        if (appPrefsWrapper.isReaderImprovementsPhase2Enabled()) {
            if (isReaderInterestsShown) return
            isReaderInterestsShown = true
            _uiState.value = InitialUiState
            _showReaderInterests.value = Event(Unit)
        } else {
            if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
            if (initialized) return
            _uiState.value = InitialUiState
            loadTabs()
        }
    }

    private fun loadTabs() {
        launch {
            val tagList = loadReaderTabsUseCase.loadTabs()
            if (tagList.isNotEmpty()) {
                _uiState.value = ContentUiState(
                        tagList.map { it.label },
                        tagList,
                        searchIconVisible = isSearchSupported()
                )
                if (!initialized) {
                    initialized = true
                    initializeTabSelection(tagList)
                }
            }
        }
    }

    private suspend fun initializeTabSelection(tagList: ReaderTagList) {
        withContext(bgDispatcher) {
            val selectTab = { it: ReaderTag ->
                val index = tagList.indexOf(it)
                if (index != -1) {
                    _selectTab.postValue(Event(index))
                }
            }
            appPrefsWrapper.getReaderTag()?.let {
                selectTab.invoke(it)
            } ?: tagList.find { it.isDefaultSelectedTab() }?.let {
                selectTab.invoke(it)
            }
        }
    }

    fun onTagChanged(selectedTag: ReaderTag?) {
        // Store most recently selected tab so we can restore the selection after restart
        appPrefsWrapper.setReaderTag(selectedTag)
    }

    fun onCloseReaderInterests() {
        _closeReaderInterests.value = Event(Unit)
        if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
        loadTabs()
    }

    sealed class ReaderUiState(
        open val searchIconVisible: Boolean,
        val appBarExpanded: Boolean = false,
        val tabLayoutVisible: Boolean = false
    ) {
        object InitialUiState : ReaderUiState(
            searchIconVisible = false,
            appBarExpanded = false,
            tabLayoutVisible = false
        )

        data class ContentUiState(
            val tabTitles: List<String>,
            val readerTagList: ReaderTagList,
            override val searchIconVisible: Boolean
        ) : ReaderUiState(searchIconVisible = searchIconVisible, appBarExpanded = true, tabLayoutVisible = true)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun tagsRequireUpdate(): Boolean {
        val lastUpdated = appPrefsWrapper.readerTagsUpdatedTimestamp
        val now = dateProvider.getCurrentDate().time
        return now - lastUpdated > UPDATE_TAGS_THRESHOLD
    }

    fun selectedTabChange(tag: ReaderTag) {
        uiState.value?.let {
            val currentUiState = it as ContentUiState
            val position = currentUiState.readerTagList.indexOfTagName(tag.tagSlug)
            _selectTab.postValue(Event(position))
        }
    }

    fun onSearchActionClicked() {
        if (isSearchSupported()) {
            _showSearch.value = Event(Unit)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Search should be hidden when isSearchSupported returns false.")
        }
    }

    private fun ReaderTag.isDefaultSelectedTab(): Boolean = this.isDiscover

    @Subscribe(threadMode = MAIN)
    fun onTagsUpdated(event: ReaderEvents.FollowedTagsChanged) {
        loadTabs()
    }

    fun onScreenInForeground() {
        readerTracker.start(MAIN_READER)
    }

    fun onScreenInBackground() {
        readerTracker.stop(MAIN_READER)
    }

    private fun isSearchSupported() = accountStore.hasAccessToken()
}
