package org.wordpress.android.ui.mysite.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

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
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
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

        _uiState.value = MenuViewState(items = applyFocusPointIfNeeded(currentItems))

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
            when(action){
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
            // todo: add the tracking logic here
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        }
    }

    private fun applyFocusPointIfNeeded(items: List<MenuItemState>) : List<MenuItemState> {
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

    fun onResume() {
        removeFocusPoints()
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
}
