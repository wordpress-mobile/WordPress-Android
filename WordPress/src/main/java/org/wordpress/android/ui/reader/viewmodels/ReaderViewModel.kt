package org.wordpress.android.ui.reader.viewmodels

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.BundleCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
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
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackOverlayConnectedFeature.READER
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem
import org.wordpress.android.ui.reader.tracker.ReaderTab
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType.MAIN_READER
import org.wordpress.android.ui.reader.usecases.LoadReaderItemsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.utils.ReaderTopBarMenuHelper
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState.TabUiState
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterSelectedItem
import org.wordpress.android.ui.reader.views.compose.filter.ReaderFilterType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.config.ReaderTagsFeedFeatureConfig
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

const val UPDATE_TAGS_THRESHOLD = 1000 * 60 * 60 // 1 hr
const val TRACK_TAB_CHANGED_THROTTLE = 100L
const val ONE_HOUR_MILLIS = 1000 * 60 * 60

@Suppress("ForbiddenComment")
class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider,
    private val loadReaderItemsUseCase: LoadReaderItemsUseCase,
    private val readerTracker: ReaderTracker,
    private val accountStore: AccountStore,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackBrandingUtils: JetpackBrandingUtils,
    private val snackbarSequencer: SnackbarSequencer,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val readerTopBarMenuHelper: ReaderTopBarMenuHelper,
    private val urlUtilsWrapper: UrlUtilsWrapper,
    private val readerTagsFeedFeatureConfig: ReaderTagsFeedFeatureConfig,
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

    fun start(savedInstanceState: Bundle? = null) {
        if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
        if (initialized) return
        loadTabs(savedInstanceState)
        if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) showJetpackPoweredBottomSheet()
    }

    fun onSaveInstanceState(out: Bundle) {
        _topBarUiState.value?.let {
            out.putString(KEY_TOP_BAR_UI_STATE_SELECTED_ITEM_ID, it.selectedItem.id)
            out.putParcelable(KEY_TOP_BAR_UI_STATE_FILTER_UI_STATE, it.filterUiState)
        }
    }

    private fun showJetpackPoweredBottomSheet() {
//        _showJetpackPoweredBottomSheet.value = Event(true)
    }

    @JvmOverloads
    fun loadTabs(savedInstanceState: Bundle? = null) {
        launch {
            val tagList = loadReaderItemsUseCase.load()
            if (tagList.isNotEmpty() && readerTagsList != tagList) {
                updateReaderTagsList(tagList)
                updateTopBarUiState(savedInstanceState)
                _uiState.value = ContentUiState(
                    tabUiStates = tagList.map { TabUiState(label = UiStringText(it.label)) },
                    selectedReaderTag = selectedReaderTag(),
                )
                if (!initialized) {
                    initialized = true
                }
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

    fun updateSelectedContent(selectedTag: ReaderTag) {
        getMenuItemFromReaderTag(selectedTag)?.let { newSelectedMenuItem ->
            // Persist selected feed to app prefs
            appPrefsWrapper.readerTopBarSelectedFeedItemId = newSelectedMenuItem.id
            // Update top bar UI state so menu is updated with new selected item
            _topBarUiState.value?.let {
                _topBarUiState.value = it.copy(
                    selectedItem = newSelectedMenuItem,
                    filterUiState = null,
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

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = MAIN)
    fun onTagsUpdated(event: ReaderEvents.FollowedTagsFetched) {
        loadTabs()
        // Determine if analytics should be bumped either due to tags changed or time elapsed since last bump
        val now = DateProvider().getCurrentDate().time
        val shouldBumpAnalytics = event.didChange()
                || (now - appPrefsWrapper.readerAnalyticsCountTagsTimestamp > ONE_HOUR_MILLIS)

        if (shouldBumpAnalytics) {
            readerTracker.trackFollowedTagsCount(event.totalTags)
            appPrefsWrapper.readerAnalyticsCountTagsTimestamp = now
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = MAIN)
    fun onSubscribedSitesUpdated(event: ReaderEvents.FollowedBlogsFetched) {
        // Determine if analytics should be bumped either due to sites changed or time elapsed since last bump
        val now = DateProvider().getCurrentDate().time
        val shouldBumpAnalytics = event.didChange()
                || (now - AppPrefs.getReaderAnalyticsCountSitesTimestamp() > ONE_HOUR_MILLIS)

        if (shouldBumpAnalytics) {
            readerTracker.trackSubscribedSitesCount(event.totalSubscriptions)
            AppPrefs.setReaderAnalyticsCountSitesTimestamp(now)
        }
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
        if (event.task == getFollowSiteTask()) startQuickStartFollowSiteTask()
    }

    private fun startQuickStartFollowSiteTask() {
        val shortMessagePrompt = if (isSettingsSupported()) {
            R.string.quick_start_dialog_follow_sites_message_short_discover_and_subscriptions
        } else {
            R.string.quick_start_dialog_follow_sites_message_short_discover
        }
        isQuickStartPromptShown = true
        _quickStartPromptEvent.value = Event(
            QuickStartReaderPrompt(
                getFollowSiteTask(),
                shortMessagePrompt,
                QuickStartUtils.ICON_NOT_SET,
            )
        )
        completeQuickStartFollowSiteTaskIfNeeded()
    }

    private fun completeQuickStartFollowSiteTaskIfNeeded() {
        if (quickStartRepository.isPendingTask(getFollowSiteTask())) {
            selectedSiteRepository.getSelectedSite()?.let {
                quickStartRepository.completeTask(getFollowSiteTask())
            }
        }
    }

    fun dismissQuickStartSnackbarIfNeeded() {
        if (isQuickStartPromptShown) snackbarSequencer.dismissLastSnackbar()
        isQuickStartPromptShown = false
    }

    private fun getFollowSiteTask() =
        quickStartRepository.quickStartType.getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL)

    private fun selectedReaderTag(): ReaderTag? =
        _topBarUiState.value?.let {
            readerTagsList[readerTopBarMenuHelper.getReaderTagIndexFromMenuItem(it.selectedItem)]
        }

    private suspend fun updateTopBarUiState(savedInstanceState: Bundle? = null) {
        withContext(bgDispatcher) {
            val menuItems = readerTopBarMenuHelper.createMenu(readerTagsList)

            // if menu is exactly the same as before, don't update
            if (_topBarUiState.value?.menuItems == menuItems) return@withContext

            // choose selected item, either from current, saved state, or persisted, falling back to first item
            val savedStateSelectedId = savedInstanceState?.getString(KEY_TOP_BAR_UI_STATE_SELECTED_ITEM_ID)

            val persistedSelectedId = appPrefsWrapper.readerTopBarSelectedFeedItemId

            val selectedItem = _topBarUiState.value?.selectedItem
                ?: menuItems.filterSingleItems()
                    .let { singleItems ->
                        singleItems.firstOrNull { it.id == savedStateSelectedId }
                            ?: singleItems.firstOrNull { it.id == persistedSelectedId }
                            ?: singleItems.first()
                    }

            // if there's a selected item and filter state, also use the filter state, also try to use the saved state
            val savedStateFilterUiState = savedInstanceState
                ?.let {
                    BundleCompat.getParcelable(
                        it,
                        KEY_TOP_BAR_UI_STATE_FILTER_UI_STATE,
                        TopBarUiState.FilterUiState::class.java
                    )
                }
                ?.takeIf { selectedItem.id == savedStateSelectedId }

            val filterUiState = _topBarUiState.value?.filterUiState
                ?.takeIf { _topBarUiState.value?.selectedItem != null }
                ?: savedStateFilterUiState

            _topBarUiState.postValue(
                TopBarUiState(
                    menuItems = menuItems,
                    selectedItem = selectedItem,
                    filterUiState = filterUiState,
                    onDropdownMenuClick = ::onDropdownMenuClick,
                    isSearchActionVisible = isSearchSupported(),
                )
            )
        }
    }

    private fun onDropdownMenuClick() {
        readerTracker.trackDropdownMenuOpened()
    }

    private fun getMenuItemFromReaderTag(readerTag: ReaderTag): MenuElementData.Item.Single? =
        _topBarUiState.value?.menuItems
            // Selected menu item must be an Item.Single
            ?.filterSingleItems()
            // Find menu item based onn selected ReaderTag
            ?.find { readerTopBarMenuHelper.getReaderTagIndexFromMenuItem(it) == readerTagsList.indexOf(readerTag) }

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

    private fun updateReaderTagsList(readerTags: List<ReaderTag>) {
        readerTagsList.clear()
        readerTagsList.addAll(readerTags)
    }

    fun onTopBarMenuItemClick(item: MenuElementData.Item.Single) {
        val selectedReaderTag = readerTagsList[readerTopBarMenuHelper.getReaderTagIndexFromMenuItem(item)]

        // Avoid reloading a content stream that is already loaded
        if (item.id != _topBarUiState.value?.selectedItem?.id) {
            selectedReaderTag?.let { updateSelectedContent(it) }
        }
        if (selectedReaderTag != null) {
            readerTracker.trackDropdownMenuItemTapped(selectedReaderTag)
        }
    }

    fun onSubFilterItemSelected(item: SubfilterListItem) {
        when (item) {
            is SubfilterListItem.SiteAll -> clearTopBarFilter()
            is SubfilterListItem.Site -> updateTopBarFilter(item.blog.name
                .ifEmpty { urlUtilsWrapper.removeScheme(item.blog.url.ifEmpty { "" }) }, ReaderFilterType.BLOG
            )

            is SubfilterListItem.Tag -> updateTopBarFilter(item.tag.tagDisplayName, ReaderFilterType.TAG)
            else -> Unit // do nothing
        }
    }

    private fun tryWaitNonNullTopBarUiStateThenRun(
        initialDelay: Long = 0L,
        retryTime: Long = 50L,
        maxRetries: Int = 10,
        runContext: CoroutineContext = mainDispatcher,
        block: suspend CoroutineScope.(topBarUiState: TopBarUiState) -> Unit
    ) {
        launch(bgDispatcher) {
            if (initialDelay > 0L) delay(initialDelay)

            var remainingTries = maxRetries
            while (_topBarUiState.value == null && remainingTries > 0) {
                delay(retryTime)
                remainingTries--
            }

            // only run the block if the topBarUiState is not null, otherwise do nothing
            _topBarUiState.value?.let { topBarUiState ->
                withContext(runContext) {
                    block(topBarUiState)
                }
            }
        }
    }

    private fun clearTopBarFilter() {
        // small delay to achieve a fluid animation since other UI updates are happening
        tryWaitNonNullTopBarUiStateThenRun(initialDelay = FILTER_UPDATE_DELAY) { topBarUiState ->
            val filterUiState = topBarUiState.filterUiState?.copy(selectedItem = null)
            _topBarUiState.postValue(topBarUiState.copy(filterUiState = filterUiState))
        }
    }

    private fun updateTopBarFilter(itemName: String, type: ReaderFilterType) {
        // small delay to achieve a fluid animation since other UI updates are happening
        tryWaitNonNullTopBarUiStateThenRun(initialDelay = FILTER_UPDATE_DELAY) { topBarUiState ->
            val filterUiState = topBarUiState.filterUiState
                ?.copy(selectedItem = ReaderFilterSelectedItem(UiStringText(itemName), type))
            _topBarUiState.postValue(topBarUiState.copy(filterUiState = filterUiState))
        }
    }

    fun hideTopBarFilterGroup(readerTab: ReaderTag) = tryWaitNonNullTopBarUiStateThenRun { topBarUiState ->
        val readerTagIndex = readerTopBarMenuHelper.getReaderTagIndexFromMenuItem(topBarUiState.selectedItem)
        val selectedReaderTag = readerTagsList[readerTagIndex]

        if (readerTab != selectedReaderTag) return@tryWaitNonNullTopBarUiStateThenRun

        _topBarUiState.postValue(topBarUiState.copy(filterUiState = null))
    }

    fun showTopBarFilterGroup(
        readerTab: ReaderTag,
        subFilterItems: List<SubfilterListItem>
    ) = tryWaitNonNullTopBarUiStateThenRun { topBarUiState ->
        val readerTagIndex = readerTopBarMenuHelper.getReaderTagIndexFromMenuItem(topBarUiState.selectedItem)
        val selectedReaderTag = readerTagsList[readerTagIndex]

        if (readerTab != selectedReaderTag) return@tryWaitNonNullTopBarUiStateThenRun

        val blogsFilterCount = subFilterItems.filterIsInstance<SubfilterListItem.Site>().size
        val tagsFilterCount = subFilterItems.filterIsInstance<SubfilterListItem.Tag>().size

        val filterState = topBarUiState.filterUiState
            ?.copy(
                blogsFilterCount = blogsFilterCount,
                tagsFilterCount = tagsFilterCount,
                showBlogsFilter = shouldShowBlogsFilter(selectedReaderTag),
                showTagsFilter = shouldShowTagsFilter(selectedReaderTag),
            )
            ?: TopBarUiState.FilterUiState(
                blogsFilterCount = blogsFilterCount,
                tagsFilterCount = tagsFilterCount,
                showBlogsFilter = shouldShowBlogsFilter(selectedReaderTag),
                showTagsFilter = shouldShowTagsFilter(selectedReaderTag),
            )

        _topBarUiState.postValue(
            topBarUiState.copy(filterUiState = filterState)
        )
    }

    private fun shouldShowBlogsFilter(readerTag: ReaderTag): Boolean {
        return readerTag.isFilterable && !readerTag.isTags
    }

    private fun shouldShowTagsFilter(readerTag: ReaderTag): Boolean {
        val showForFollowedSites = readerTag.isFollowedSites && !readerTagsFeedFeatureConfig.isEnabled()
        val showForTags = readerTag.isTags

        return readerTag.isFilterable && (showForFollowedSites || showForTags)
    }

    data class TopBarUiState(
        val menuItems: List<MenuElementData>,
        val selectedItem: MenuElementData.Item.Single,
        val filterUiState: FilterUiState? = null,
        val onDropdownMenuClick: () -> Unit,
        val isSearchActionVisible: Boolean = false,
    ) {
        @Parcelize
        data class FilterUiState(
            val blogsFilterCount: Int,
            val tagsFilterCount: Int,
            val selectedItem: ReaderFilterSelectedItem? = null,
            val showBlogsFilter: Boolean = blogsFilterCount > 0,
            val showTagsFilter: Boolean = tagsFilterCount > 0,
        ) : Parcelable
    }

    sealed class ReaderUiState(
        val appBarExpanded: Boolean = false,
        val tabLayoutVisible: Boolean = false
    ) {
        data class ContentUiState(
            val tabUiStates: List<TabUiState>,
            val selectedReaderTag: ReaderTag?,
        ) : ReaderUiState(
            appBarExpanded = true,
            tabLayoutVisible = true
        ) {
            data class TabUiState(
                val label: UiString
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
        private const val QUICK_START_PROMPT_DURATION = 5000
        private const val FILTER_UPDATE_DELAY = 50L

        private const val KEY_TOP_BAR_UI_STATE_SELECTED_ITEM_ID = "topBarUiState_selectedItem_id"
        private const val KEY_TOP_BAR_UI_STATE_FILTER_UI_STATE = "topBarUiState_filterUiState"
    }
}

data class TabNavigation(val position: Int, val smoothAnimation: Boolean)
