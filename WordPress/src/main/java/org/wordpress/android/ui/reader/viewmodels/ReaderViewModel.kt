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
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.READER
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
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.MenuItemUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.TabUiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val UPDATE_TAGS_THRESHOLD = 1000 * 60 * 60 // 1 hr
const val TRACK_TAB_CHANGED_THROTTLE = 100L

@Suppress("ForbiddenComment")
class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider,
    private val loadReaderTabsUseCase: LoadReaderTabsUseCase,
    private val readerTracker: ReaderTracker,
    private val accountStore: AccountStore,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackBrandingUtils: JetpackBrandingUtils,
    private val snackbarSequencer: SnackbarSequencer,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
        // todo: annnmarie removed this private val getFollowedTagsUseCase: GetFollowedTagsUseCase
) : ScopedViewModel(mainDispatcher) {
    private var initialized: Boolean = false
    private var wasPaused: Boolean = false
    private var trackReaderTabJob: Job? = null
    private var isQuickStartPromptShown: Boolean = false

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

    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    init {
        EventBus.getDefault().register(this)
    }

    fun start() {
        if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
        if (initialized) return
        loadTabs()
        if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) showJetpackPoweredBottomSheet()
    }

    private fun showJetpackPoweredBottomSheet() {
//        _showJetpackPoweredBottomSheet.value = Event(true)
    }

    private fun loadTabs() {
        launch {
            val currentContentUiState = _uiState.value as? ContentUiState
            val tagList = loadReaderTabsUseCase.loadTabs()
            if (tagList.isNotEmpty()) {
                _uiState.value = ContentUiState(
                        tagList.map { TabUiState(label = UiStringText(it.label)) },
                        tagList,
                        shouldUpdateViewPager = currentContentUiState?.readerTagList?.equals(tagList) == false,
                        searchMenuItemUiState = MenuItemUiState(isVisible = isSearchSupported()),
                        settingsMenuItemUiState = MenuItemUiState(
                                isVisible = isSettingsSupported(),
                                showQuickStartFocusPoint =
                                currentContentUiState?.settingsMenuItemUiState?.showQuickStartFocusPoint ?: false
                        )
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
            val selectTab = { readerTag: ReaderTag ->
                val index = tagList.indexOf(readerTag)
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
    }

    fun onCloseReaderInterests() {
        _closeReaderInterests.value = Event(Unit)
    }

    fun onShowReaderInterests() {
        _showReaderInterests.value = Event(Unit)
    }

    sealed class ReaderUiState(
        open val searchMenuItemUiState: MenuItemUiState,
        open val settingsMenuItemUiState: MenuItemUiState,
        val appBarExpanded: Boolean = false,
        val tabLayoutVisible: Boolean = false
    ) {
        data class ContentUiState(
            val tabUiStates: List<TabUiState>,
            val readerTagList: ReaderTagList,
            val shouldUpdateViewPager: Boolean,
            override val searchMenuItemUiState: MenuItemUiState,
            override val settingsMenuItemUiState: MenuItemUiState
        ) : ReaderUiState(
                searchMenuItemUiState = searchMenuItemUiState,
                settingsMenuItemUiState = settingsMenuItemUiState,
                appBarExpanded = true,
                tabLayoutVisible = true
        ) {
            data class TabUiState(
                val label: UiString
            )

            data class MenuItemUiState(
                val isVisible: Boolean,
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

    @Suppress("UseCheckOrError")
    fun onSearchActionClicked() {
        if (isSearchSupported()) {
            _showSearch.value = Event(Unit)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Search should be hidden when isSearchSupported returns false.")
        }
    }

    @Suppress("UseCheckOrError")
    fun onSettingsActionClicked() {
        if (isSettingsSupported()) {
            completeQuickStartFollowSiteTaskIfNeeded()
            _showSettings.value = Event(Unit)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Settings should be hidden when isSettingsSupported returns false.")
        }
    }

    private fun ReaderTag.isDefaultSelectedTab(): Boolean = this.isDiscover

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = MAIN)
    fun onTagsUpdated(event: ReaderEvents.FollowedTagsChanged) {
        loadTabs()
    }

    fun onScreenInForeground() {
        readerTracker.start(MAIN_READER)
        appPrefsWrapper.getReaderTag()?.let {
            trackReaderTabShownIfNecessary(it)
        }
        if (jetpackFeatureRemovalOverlayUtil.shouldShowFeatureSpecificJetpackOverlay(READER)) showJetpackOverlay()
    }

    private fun showJetpackOverlay() {
        _showJetpackOverlay.value = Event(true)
    }

    fun onScreenInBackground(isChangingConfigurations: Boolean) {
        readerTracker.stop(MAIN_READER)
        wasPaused = true
        if (!isChangingConfigurations) {
            hideQuickStartFocusPointIfNeeded()
            dismissQuickStartSnackbarIfNeeded()
            if (quickStartRepository.isPendingTask(getFollowSiteTask())) {
                quickStartRepository.clearPendingTask()
            }
        }
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

    fun onQuickStartPromptDismissed() {
        isQuickStartPromptShown = false
    }

    fun onQuickStartEventReceived(event: QuickStartEvent) {
        if (event.task == getFollowSiteTask()) checkAndStartQuickStartFollowSiteTaskNextStep()
    }

    private fun checkAndStartQuickStartFollowSiteTaskNextStep() {
        val isDiscover = appPrefsWrapper.getReaderTag()?.isDiscover == true
        if (isDiscover) {
            startQuickStartFollowSiteTaskDiscoverTabStep()
        } else {
            autoSwitchToDiscoverTab()
        }
    }

    private fun autoSwitchToDiscoverTab() {
        launch {
            if (!initialized) delay(QUICK_START_DISCOVER_TAB_STEP_DELAY)
            (_uiState.value as? ContentUiState)?.readerTagList?.find { it.isDiscover }?.let {
                selectedTabChange(it)
            }
            startQuickStartFollowSiteTaskDiscoverTabStep()
        }
    }

    private fun startQuickStartFollowSiteTaskDiscoverTabStep() {
        val shortMessagePrompt = if (isSettingsSupported()) {
            R.string.quick_start_dialog_follow_sites_message_short_discover_and_settings
        } else {
            R.string.quick_start_dialog_follow_sites_message_short_discover
        }
        isQuickStartPromptShown = true
        _quickStartPromptEvent.value = Event(
                QuickStartReaderPrompt(
                        getFollowSiteTask(),
                        shortMessagePrompt,
                        R.drawable.ic_cog_white_24dp
                )
        )
        updateContentUiState(showQuickStartFocusPoint = isSettingsSupported())
    }

    fun completeQuickStartFollowSiteTaskIfNeeded() {
        if (quickStartRepository.isPendingTask(getFollowSiteTask())) {
            selectedSiteRepository.getSelectedSite()?.let {
                hideQuickStartFocusPointIfNeeded()
                quickStartRepository.completeTask(getFollowSiteTask())
            }
        }
    }

    fun dismissQuickStartSnackbarIfNeeded() {
        if (isQuickStartPromptShown) snackbarSequencer.dismissLastSnackbar()
        isQuickStartPromptShown = false
    }

    private fun hideQuickStartFocusPointIfNeeded() {
        val currentUiState = _uiState.value as? ContentUiState
        if (currentUiState?.settingsMenuItemUiState?.showQuickStartFocusPoint == true) {
            updateContentUiState(showQuickStartFocusPoint = false)
        }
    }

    private fun getFollowSiteTask() =
        quickStartRepository.quickStartType.getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL)

    private fun updateContentUiState(
        showQuickStartFocusPoint: Boolean
    ) {
        val currentUiState = _uiState.value as? ContentUiState
        currentUiState?.let {
            _uiState.value = currentUiState.copy(
                    settingsMenuItemUiState = it.settingsMenuItemUiState.copy(
                            isVisible = isSettingsSupported(),
                            showQuickStartFocusPoint = showQuickStartFocusPoint
                    ),
                    shouldUpdateViewPager = false
            )
        }
    }

    data class QuickStartReaderPrompt(
        val task: QuickStartTask,
        @StringRes val shortMessagePrompt: Int,
        @DrawableRes val iconId: Int,
        val duration: Int = QUICK_START_PROMPT_DURATION
    )

    companion object {
        private const val QUICK_START_DISCOVER_TAB_STEP_DELAY = 2000L
        private const val QUICK_START_PROMPT_DURATION = 5000
    }
}

data class TabNavigation(val position: Int, val smoothAnimation: Boolean)
