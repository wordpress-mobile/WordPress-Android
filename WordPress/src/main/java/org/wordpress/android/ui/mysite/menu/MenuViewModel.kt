package org.wordpress.android.ui.mysite.menu

import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LONG_DURATION_MS
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.SHORT_DURATION_MS
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

const val MENU_ITEM_TRACKING_PARAMETER = "item"

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val jetpackMigrationLanguageUtil: JetpackMigrationLanguageUtil,
    private val listItemActionHandler: ListItemActionHandler,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val contextProvider: ContextProvider,
    private val uiHelpers: UiHelpers,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(bgDispatcher) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = merge(_onSnackbarMessage, quickStartRepository.onSnackbar)

    val onQuickStartMySitePrompts = quickStartRepository.onQuickStartMySitePrompts

    private val _refreshAppLanguage = MutableLiveData<String>()
    val refreshAppLanguage: LiveData<String> = _refreshAppLanguage

    private val _uiState = MutableStateFlow(MenuViewState(items = emptyList()))
    val uiState: StateFlow<MenuViewState> = _uiState

    private val _snackbar = MutableSharedFlow<SnackbarMessage>()
    val snackBar = _snackbar.asSharedFlow()

    private var quickStartEvent: QuickStartEvent? = null
    private var isStarted = false

    init {
        emitLanguageRefreshIfNeeded(localeManagerWrapper.getLanguage())
    }

    fun start(quickStartEvent: QuickStartEvent? = null) {
        if (isStarted) {
            return
        }
        val site = selectedSiteRepository.getSelectedSite()!!
        this.quickStartEvent = quickStartEvent
        if (quickStartEvent != null) {
            quickStartRepository.setActiveTask(quickStartEvent.task, true)
        }
        buildSiteMenu(site)
        isStarted = true
    }

    private fun buildSiteMenu(site: SiteModel) {
        val currentItems = siteItemsBuilder.build(
            MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                enableFocusPoints = true,
                site = site,
                activeTask = quickStartEvent?.task,
                onClick = this::onClick,
                isBlazeEligible = isSiteBlazeEligible()
            )
        ).filterIsInstance<MySiteCardAndItem.Item>().map {
            it.toMenuItemState()
        }.toList()

        _uiState.value = MenuViewState(
            items = applyFocusPointIfNeeded(currentItems)
        )

        rebuildSiteItemsForJetpackCapabilities(site)
    }

    private fun rebuildSiteItemsForJetpackCapabilities(site: SiteModel) {
        launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                val currentItems = siteItemsBuilder.build(
                    MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                        enableFocusPoints = true,
                        site = site,
                        activeTask = quickStartEvent?.task,
                        onClick = this@MenuViewModel::onClick,
                        isBlazeEligible = isSiteBlazeEligible(),
                        scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                    )
                ).filterIsInstance<MySiteCardAndItem.Item>().map { item ->
                    item.toMenuItemState()
                }.toList()

                _uiState.value = MenuViewState(items = applyFocusPointIfNeeded(currentItems))
            } // end collect
        }
    }

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)

    private fun onClick(action: ListItemAction) {
        clearQuickStartEvent()
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            when (action) {
                ListItemAction.PAGES -> {
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES)
                }
                ListItemAction.SHARING -> {
                    quickStartRepository.requestNextStepOfTask(
                        QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
                    )
                }

                ListItemAction.STATS -> {
                    quickStartRepository.completeTask(
                        quickStartRepository.quickStartType.getTaskFromString(
                            QuickStartStore.QUICK_START_CHECK_STATS_LABEL
                        )
                    )
                }

                ListItemAction.MEDIA -> {
                    quickStartRepository.requestNextStepOfTask(
                        quickStartRepository.quickStartType.getTaskFromString(
                            QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL
                        )
                    )
                }

                else -> {}
            }
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.MORE_MENU_ITEM_TAPPED,
                mapOf(MENU_ITEM_TRACKING_PARAMETER to action.trackingLabel)
            )
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        }
    }

    private fun applyFocusPointIfNeeded(items: List<MenuItemState>): List<MenuItemState> {
        return quickStartEvent?.let {
            val showFocusPointOn = convertQuickStartTaskToListItemAction(it.task)
            items.map { item ->
                if (item is MenuItemState.MenuListItem) {
                    if (item.listItemAction == showFocusPointOn) {
                        item.copy(showFocusPoint = true)
                    } else {
                        item
                    }
                } else {
                    item
                }
            }.toList()
        } ?: items
    }

    private fun convertQuickStartTaskToListItemAction(task: QuickStartTask): ListItemAction {
        return when (task) {
            QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES -> ListItemAction.PAGES
            QuickStartStore.QuickStartNewSiteTask.CHECK_STATS -> ListItemAction.STATS
            QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING -> ListItemAction.SHARING
            QuickStartStore.QuickStartExistingSiteTask.UPLOAD_MEDIA -> ListItemAction.MEDIA
            QuickStartStore.QuickStartExistingSiteTask.CHECK_STATS -> ListItemAction.STATS
            else -> ListItemAction.MORE
        }
    }

    fun showSnackbarRequest(item: SnackbarItem) {
        launch(bgDispatcher) {
            handleShowSnackbarRequest(item)
        }
    }

    private fun removeFocusPoints() {
        if (quickStartEvent == null) {
            val items = _uiState.value.items.map { item ->
                if (item is MenuItemState.MenuListItem) {
                    item.copy(showFocusPoint = false)
                } else {
                    item
                }
            }.toList()
            _uiState.value = MenuViewState(items = items)
        }
    }

    private fun clearQuickStartEvent() {
        quickStartEvent = null
    }

    /*
    * This creates a very lightweight snackbar messages for quick start. No action events and no icons. At the
    * point of this function execution, the snackbar is already created and ready to be shown. If in the future,
    * the entire snackbar creation process should be refactored to be handle both compose and non-compose.
     */
    private suspend fun handleShowSnackbarRequest(item: SnackbarItem) {
        val snackbarMessage = SnackbarMessage(
            message = uiHelpers.getTextOfUiString(contextProvider.getContext(), item.info.textRes).toString(),
            actionLabel = item.action?.let {
                uiHelpers.getTextOfUiString(contextProvider.getContext(), it.textRes).toString()
            },
            // these values are set when the snackbar is created in SnackbarItem, this just reverses that
            duration = when ((item.info.duration).toLong()) {
                LONG_DURATION_MS -> SnackbarDuration.Long
                SHORT_DURATION_MS -> SnackbarDuration.Short
                else -> SnackbarDuration.Short
            }
        )
        _snackbar.emit(snackbarMessage)
    }

    private fun emitLanguageRefreshIfNeeded(languageCode: String) {
        if (languageCode.isNotEmpty()) {
            val shouldEmitLanguageRefresh = !localeManagerWrapper.isSameLanguage(languageCode)
            if (shouldEmitLanguageRefresh) {
                _refreshAppLanguage.value = languageCode
            }
        }
    }

    fun setAppLanguage(locale: Locale) {
        jetpackMigrationLanguageUtil.applyLanguage(locale.language)
    }

    override fun onCleared() {
        jetpackCapabilitiesUseCase.clear()
        super.onCleared()
    }

    fun onResume() {
        removeFocusPoints()
    }

    data class SnackbarMessage(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration
    )
}
