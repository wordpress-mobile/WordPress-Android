package org.wordpress.android.ui.mysite.menu

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.CHECK_STATS as NEW_STATS
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask.UPLOAD_MEDIA
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask.CHECK_STATS
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
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

data class MenuViewState(
    val items: List<MySiteCardAndItem> // cards and or items
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val listItemActionHandler: ListItemActionHandler,
    private val blazeFeatureUtils: BlazeFeatureUtils
) : ScopedViewModel(bgDispatcher) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = _onNavigation

    private val _uiState = MutableStateFlow(MenuViewState(items = emptyList()))

    val uiState: StateFlow<MenuViewState> = _uiState
    private var quickStartEvent: QuickStartEvent? = null

    fun start(quickStartEvent: QuickStartEvent?) {
        val site = selectedSiteRepository.getSelectedSite()!!
        this.quickStartEvent = quickStartEvent
        buildSiteMenu(site)
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
        )

        val items = applyFocusPointIfNeeded(currentItems)
        _uiState.value = MenuViewState(items = items)

        rebuildSiteItemsForJetpackCapabilities(site)
    }

    private fun rebuildSiteItemsForJetpackCapabilities(site: SiteModel) {
        launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                    val items = siteItemsBuilder.build(
                        MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                            site = site,
                            enableFocusPoints = true,
                            activeTask = null,
                            onClick = this@MenuViewModel::onClick,
                            isBlazeEligible = isSiteBlazeEligible(),
                            backupAvailable = it.backup,
                            scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                        )
                    )
                 _uiState.value = MenuViewState(items = applyFocusPointIfNeeded(items))
                }
            } // end collect
        }

    private fun applyFocusPointIfNeeded(items: List<MySiteCardAndItem>) : List<MySiteCardAndItem> {
        return quickStartEvent?.let {
            val showFocusPointOn = convertQuickStartTaskToListItemAction(it.task)
            items.map { item ->
                if (item is MySiteCardAndItem.Item.ListItem) {
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

    private fun convertQuickStartTaskToListItemAction(task: QuickStartTask): ListItemAction {
        return when (task) {
            REVIEW_PAGES -> ListItemAction.PAGES
            NEW_STATS -> ListItemAction.STATS
            ENABLE_POST_SHARING -> ListItemAction.SHARING
            UPLOAD_MEDIA -> ListItemAction.MEDIA
            CHECK_STATS -> ListItemAction.STATS
            else -> ListItemAction.MORE
        }
    }

    private fun clearQuickStartEvent() {
        quickStartEvent = null
    }

    override fun onCleared() {
        jetpackCapabilitiesUseCase.clear()
        super.onCleared()
    }
}
