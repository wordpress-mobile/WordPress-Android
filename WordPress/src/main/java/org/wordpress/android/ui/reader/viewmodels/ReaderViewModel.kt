package org.wordpress.android.ui.reader.viewmodels

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.tracker.ReaderTab
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.TabUiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val UPDATE_TAGS_THRESHOLD = 1000 * 60 * 60 // 1 hr
const val TRACK_TAB_CHANGED_THROTTLE = 100L

class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider,
    private val loadReaderTabsUseCase: LoadReaderTabsUseCase,
    private val readerTracker: ReaderTracker,
    private val accountStore: AccountStore,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository
        // todo: annnmarie removed this private val getFollowedTagsUseCase: GetFollowedTagsUseCase
) : ScopedViewModel(mainDispatcher) {
    private var initialized: Boolean = false
    private var wasPaused: Boolean = false
    private var trackReaderTabJob: Job? = null

    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState.distinct()

    private val _updateTags = MutableLiveData<Event<Unit>>()
    val updateTags: LiveData<Event<Unit>> = _updateTags

    private val _selectTab = MutableLiveData<Event<TabNavigation>>()
    val selectTab: LiveData<Event<TabNavigation>> = _selectTab

    private val _showSearch = MutableLiveData<Event<Unit>>()
    val showSearch: LiveData<Event<Unit>> = _showSearch

    private val _showSettings = MutableLiveData<Event<Unit>>()
    val showSettings: LiveData<Event<Unit>> = _showSettings

    private val _showReaderInterests = MutableLiveData<Event<Unit>>()
    val showReaderInterests: LiveData<Event<Unit>> = _showReaderInterests

    private val _closeReaderInterests = MutableLiveData<Event<Unit>>()
    val closeReaderInterests: LiveData<Event<Unit>> = _closeReaderInterests

    private val _quickStartPromptEvent = MutableLiveData<Event<QuickStartReaderPrompt>>()
    val quickStartPromptEvent = _quickStartPromptEvent as LiveData<Event<QuickStartReaderPrompt>>

    init {
        EventBus.getDefault().register(this)
    }

    fun start() {
        if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
        if (initialized) return
        loadTabs()
    }

    private fun loadTabs() {
        launch {
            val tagList = loadReaderTabsUseCase.loadTabs()
            if (tagList.isNotEmpty()) {
                _uiState.value = ContentUiState(
                        tagList.map { TabUiState(label = UiStringText(it.label)) },
                        tagList,
                        searchIconVisible = isSearchSupported(),
                        settingsIconVisible = isSettingsSupported()
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
                    _selectTab.postValue(Event(TabNavigation(index, smoothAnimation = false)))
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
        selectedTag?.let {
            trackReaderTabShownIfNecessary(it)
        }
        // Store most recently selected tab so we can restore the selection after restart
        appPrefsWrapper.setReaderTag(selectedTag)

        if (quickStartRepository.isPendingTask(QuickStartTask.FOLLOW_SITE)) {
            val isDiscover = appPrefsWrapper.getReaderTag()?.isDiscover == true
            if (isDiscover) startQuickStartFollowSiteTaskSettingsStep()
        }
    }

    fun onCloseReaderInterests() {
        _closeReaderInterests.value = Event(Unit)
    }

    fun onShowReaderInterests() {
        _showReaderInterests.value = Event(Unit)
    }

    sealed class ReaderUiState(
        open val searchIconVisible: Boolean,
        open val settingsIconVisible: Boolean,
        val appBarExpanded: Boolean = false,
        val tabLayoutVisible: Boolean = false
    ) {
        data class ContentUiState(
            val tabUiStates: List<TabUiState>,
            val readerTagList: ReaderTagList,
            override val searchIconVisible: Boolean,
            override val settingsIconVisible: Boolean
        ) : ReaderUiState(
                searchIconVisible = searchIconVisible,
                settingsIconVisible = settingsIconVisible,
                appBarExpanded = true,
                tabLayoutVisible = true
        ) {
            data class TabUiState(
                val label: UiString,
                val showQuickStartFocusPoint: Boolean = false
            )
        }
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
            _selectTab.postValue(Event(TabNavigation(position, smoothAnimation = true)))
        }
    }

    fun bookmarkTabRequested() {
        (_uiState.value as? ContentUiState)?.readerTagList?.find { it.isBookmarked }?.let {
            selectedTabChange(it)
        }
    }

    fun onSearchActionClicked() {
        if (isSearchSupported()) {
            _showSearch.value = Event(Unit)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Search should be hidden when isSearchSupported returns false.")
        }
    }

    fun onSettingsActionClicked() {
        if (isSettingsSupported()) {
            _showSettings.value = Event(Unit)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Settings should be hidden when isSettingsSupported returns false.")
        }
    }

    private fun ReaderTag.isDefaultSelectedTab(): Boolean = this.isDiscover

    @Subscribe(threadMode = MAIN)
    fun onTagsUpdated(event: ReaderEvents.FollowedTagsChanged) {
        loadTabs()
    }

    fun onScreenInForeground() {
        readerTracker.start(MAIN_READER)
        appPrefsWrapper.getReaderTag()?.let {
            trackReaderTabShownIfNecessary(it)
        }
    }

    fun onScreenInBackground() {
        readerTracker.stop(MAIN_READER)
        wasPaused = true
    }

    private fun isSearchSupported() = accountStore.hasAccessToken()

    private fun isSettingsSupported() = accountStore.hasAccessToken()

    private fun trackReaderTabShownIfNecessary(it: ReaderTag) {
        trackReaderTabJob?.cancel()
        trackReaderTabJob = launch {
            // we need to add this throttle as it takes a few ms to select the last selected tab after app restart
            delay(TRACK_TAB_CHANGED_THROTTLE)
            readerTracker.trackReaderTabIfNecessary(ReaderTab.transformTagToTab(it))
        }
    }

    /* QUICK START */

    fun onQuickStartEventReceived(event: QuickStartEvent) {
        if (event.task == QuickStartTask.FOLLOW_SITE) { checkAndStartQuickStartFollowSiteTaskNextStep() }
    }

    private fun checkAndStartQuickStartFollowSiteTaskNextStep() {
        val isDiscover = appPrefsWrapper.getReaderTag()?.isDiscover == true
        if (isDiscover) {
            startQuickStartFollowSiteTaskSettingsStep()
        } else {
            startQuickStartFollowSiteTaskDiscoverStep()
        }
    }

    private fun startQuickStartFollowSiteTaskDiscoverStep() {
        _quickStartPromptEvent.value = Event(QuickStartReaderPrompt.FollowSiteDiscoverStepPrompt)
    }

    private fun startQuickStartFollowSiteTaskSettingsStep() {
        _quickStartPromptEvent.value = Event(QuickStartReaderPrompt.FollowSiteSettingsStepPrompt)
        selectedSiteRepository.getSelectedSite()?.let { completeQuickStartFollowSiteTask() }
    }

    private fun completeQuickStartFollowSiteTask() {
        quickStartRepository.completeTask(QuickStartTask.FOLLOW_SITE)
    }

    sealed class QuickStartReaderPrompt(
        val task: QuickStartTask,
        @StringRes val shortMessagePrompt: Int,
        @DrawableRes val iconId: Int
    ) {
        object FollowSiteDiscoverStepPrompt : QuickStartReaderPrompt(
                QuickStartTask.FOLLOW_SITE,
                R.string.quick_start_dialog_follow_sites_message_short_discover,
                QuickStartUtils.ICON_NOT_SET
        )

        object FollowSiteSettingsStepPrompt : QuickStartReaderPrompt(
                QuickStartTask.FOLLOW_SITE,
                R.string.quick_start_dialog_follow_sites_message_short_settings,
                R.drawable.ic_cog_white_24dp
        )
    }
}

data class TabNavigation(val position: Int, val smoothAnimation: Boolean)
