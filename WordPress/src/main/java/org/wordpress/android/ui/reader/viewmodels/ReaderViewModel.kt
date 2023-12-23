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
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
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
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterSelectedItem
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.distinct
import org.wordpress.android.util.extensions.indexOrNull
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

    private val _topBarUiState = MutableLiveData<TopBarUiState>()
    val topBarUiState: LiveData<TopBarUiState> = _topBarUiState.distinct()

    private val _updateTags = MutableLiveData<Event<Unit>>()
    val updateTags: LiveData<Event<Unit>> = _updateTags

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

    private var readerTagsList = ReaderTagList()

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
                updateReaderTagsList(loadReaderTabsUseCase.loadTabs())
                _uiState.value = ContentUiState(
                    tabUiStates = tagList.map { TabUiState(label = UiStringText(it.label)) },
                    selectedReaderTag = selectedReaderTag(),
                    searchMenuItemUiState = MenuItemUiState(isVisible = isSearchSupported()),
                    settingsMenuItemUiState = MenuItemUiState(
                        isVisible = isSettingsSupported(),
                        showQuickStartFocusPoint =
                        currentContentUiState?.settingsMenuItemUiState?.showQuickStartFocusPoint ?: false
                    )
                )
                if (!initialized) {
                    initialized = true
                    initializeMenuSelection(tagList)
                    initializeTopBarUiState()
                }
            }
        }
    }

    private suspend fun initializeMenuSelection(tagList: ReaderTagList) {
        withContext(bgDispatcher) {
            val selectTab = { readerTag: ReaderTag ->
                val index = tagList.indexOf(readerTag)
                if (index != -1) {
                    updateSelectedContent(readerTag)
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

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun tagsRequireUpdate(): Boolean {
        val lastUpdated = appPrefsWrapper.readerTagsUpdatedTimestamp
        val now = dateProvider.getCurrentDate().time
        return now - lastUpdated > UPDATE_TAGS_THRESHOLD
    }

    fun updateSelectedContent(
        selectedTag: ReaderTag,
        // TODO replace with real logic
        filterUiState: TopBarUiState.FilterUiState? = null
    ) {
        getMenuItemFromReaderTag(selectedTag)?.let { newSelectedMenuItem ->
            // Update top bar UI state so menu is updated with new selected item
            _topBarUiState.value?.let {
                _topBarUiState.value = it.copy(
                    selectedItem = newSelectedMenuItem,
                    filterUiState = filterUiState,
                )
            }
            // Updated post list content
            val currentUiState = _uiState.value as? ContentUiState
            currentUiState?.let {
                _uiState.value = currentUiState.copy(
                    selectedReaderTag = selectedReaderTag()
                )
            }
        }
    }

    fun bookmarkTabRequested() {
        readerTagsList.find { it.isBookmarked }?.let {
            updateSelectedContent(it)
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
            readerTagsList.find { it.isDiscover }?.let {
                updateSelectedContent(it)
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
            )
        }
    }

    private fun selectedReaderTag(): ReaderTag? =
        _topBarUiState.value?.let {
            readerTagsList[getReaderTagIndexFromMenuItem(it.selectedItem)]
        }

    private fun initializeTopBarUiState() {
        val menuItems = mutableListOf<MenuElementData>().apply {
            readerTagsList.indexOrNull { it.isDiscover }?.let { discoverIndex ->
                add(
                    MenuElementData.Item.Single(
                        id = createMenuItemIdFromReaderTagIndex(discoverIndex),
                        text = UiString.UiStringRes(R.string.reader_dropdown_menu_discover),
                        leadingIcon = R.drawable.ic_reader_discover_24dp,
                    )
                )
            }
            readerTagsList.indexOrNull { it.isFollowedSites }?.let { followingIndex ->
                add(
                    MenuElementData.Item.Single(
                        id = createMenuItemIdFromReaderTagIndex(followingIndex),
                        text = UiString.UiStringRes(R.string.reader_dropdown_menu_subscriptions),
                        leadingIcon = R.drawable.ic_reader_subscriptions_24dp,
                    )
                )
            }
            readerTagsList.indexOrNull { it.isBookmarked }?.let { savedIndex ->
                add(
                    MenuElementData.Item.Single(
                        id = createMenuItemIdFromReaderTagIndex(savedIndex),
                        text = UiString.UiStringRes(R.string.reader_dropdown_menu_saved),
                        leadingIcon = R.drawable.ic_reader_saved_24dp,
                    )
                )
            }
            readerTagsList.indexOrNull { it.isPostsILike }?.let { likedIndex ->
                add(
                    MenuElementData.Item.Single(
                        id = createMenuItemIdFromReaderTagIndex(likedIndex),
                        text = UiString.UiStringRes(R.string.reader_dropdown_menu_liked),
                        leadingIcon = R.drawable.ic_reader_liked_24dp,
                    )
                )
            }
            readerTagsList.indexOrNull { it.isA8C }?.let { a8cIndex ->
                add(
                    MenuElementData.Item.Single(
                        id = createMenuItemIdFromReaderTagIndex(a8cIndex),
                        text = UiString.UiStringRes(R.string.reader_dropdown_menu_automattic),
                    )
                )
            }
            readerTagsList
                .mapIndexed { index, readerTag -> Pair(index, readerTag) }
                .filter { (_, readerTag) -> readerTag.tagType == ReaderTagType.CUSTOM_LIST }.let {
                    if (it.isNotEmpty()) {
                        add(MenuElementData.Divider)
                        val customLists = it.map { (index, readerTag) ->
                            MenuElementData.Item.Single(
                                id = createMenuItemIdFromReaderTagIndex(index),
                                text = UiStringText(readerTag.tagTitle),
                            )
                        }
                        add(
                            MenuElementData.Item.SubMenu(
                                // We don't need this ID since this menu item just opens the sub-menu. It doesn't
                                // change the content that is currently being displayed.
                                id = "custom-lists",
                                text = UiString.UiStringRes(R.string.reader_dropdown_menu_lists),
                                children = customLists,
                            )
                        )
                    }
                }
        }
        _topBarUiState.value = TopBarUiState(
            menuItems = menuItems,
            selectedItem = menuItems.first { it is MenuElementData.Item.Single } as MenuElementData.Item.Single,
            filterUiState = null,
        )
    }

    private fun createMenuItemIdFromReaderTagIndex(readerTagIndex: Int): String = "$readerTagIndex"

    private fun getMenuItemFromReaderTag(readerTag: ReaderTag): MenuElementData.Item.Single? =
        _topBarUiState.value?.menuItems
            // Selected menu item must be an Item.Single
            ?.filterSingleItems()
            // Find menu item based onn selected ReaderTag
            ?.find { getReaderTagIndexFromMenuItem(it) == readerTagsList.indexOf(readerTag) }

    private fun List<MenuElementData>.filterSingleItems(): List<MenuElementData.Item.Single> {
        val singleItems = mutableListOf<MenuElementData.Item.Single>()
        forEach {
            if (it is MenuElementData.Item.Single) {
                singleItems.add(it)
            } else if (it is MenuElementData.Item.SubMenu) {
                singleItems.addAll(it.children.filterIsInstance<MenuElementData.Item.Single>())
            }
        }
        return singleItems
    }

    private fun getReaderTagIndexFromMenuItem(menuItem: MenuElementData.Item.Single) =
        menuItem.id.toInt()

    private fun updateReaderTagsList(readerTags: List<ReaderTag>) {
        readerTagsList.clear()
        readerTagsList.addAll(readerTags)
    }

    fun onTopBarMenuItemClick(item: MenuElementData.Item.Single) {
        // TODO actual logic needs to be created
        //  The current logic is for initial implementation and UI review only.
        val filterUiState = TopBarUiState.FilterUiState(
            followedBlogsCount = 23,
            followedTagsCount = 41,
        ).takeIf { item.id == "0" }

        // Avoid reloading a content stream that is already loaded
        if (item.id != _topBarUiState.value?.selectedItem?.id) {
            readerTagsList[getReaderTagIndexFromMenuItem(item)]?.let { selectedReaderTag ->
                updateSelectedContent(selectedReaderTag, filterUiState)
            }
        }
    }

    fun onTopBarFilterClick(type: ReaderFilterType) {
        // TODO actual logic needs to be created (opening filter bottom sheet).
        //  The current logic is for initial implementation and UI review only.
        val itemText = when (type) {
            ReaderFilterType.BLOG -> UiStringText("Selected Blog")
            ReaderFilterType.TAG -> UiStringText("Selected Site")
        }

        val filterUiState = _topBarUiState.value?.filterUiState
            ?.copy(selectedItem = ReaderFilterSelectedItem(itemText, type))

        _topBarUiState.value = _topBarUiState.value
            ?.copy(filterUiState = filterUiState)
    }

    fun onTopBarClearFilterClick() {
        // TODO actual logic needs to be created (clearing filter).
        //  The current logic is for initial implementation and UI review only.
        val filterUiState = _topBarUiState.value?.filterUiState
            ?.copy(selectedItem = null)

        _topBarUiState.value = _topBarUiState.value
            ?.copy(filterUiState = filterUiState)
    }

    data class TopBarUiState(
        val menuItems: List<MenuElementData>,
        val selectedItem: MenuElementData.Item.Single,
        val filterUiState: FilterUiState? = null,
    ) {
        data class FilterUiState(
            val followedBlogsCount: Int,
            val followedTagsCount: Int,
            val selectedItem: ReaderFilterSelectedItem? = null,
        )
    }

    sealed class ReaderUiState(
        open val searchMenuItemUiState: MenuItemUiState,
        open val settingsMenuItemUiState: MenuItemUiState,
        val appBarExpanded: Boolean = false,
        val tabLayoutVisible: Boolean = false
    ) {
        data class ContentUiState(
            val tabUiStates: List<TabUiState>,
            val selectedReaderTag: ReaderTag?,
            override val searchMenuItemUiState: MenuItemUiState,
            override val settingsMenuItemUiState: MenuItemUiState,
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
