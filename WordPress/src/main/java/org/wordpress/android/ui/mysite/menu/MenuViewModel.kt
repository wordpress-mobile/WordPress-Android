package org.wordpress.android.ui.mysite.menu

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LONG_DURATION_MS
import org.wordpress.android.util.SHORT_DURATION_MS
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val MENU_ITEM_TRACKING_PARAMETER = "item"

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val listItemActionHandler: ListItemActionHandler,
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
    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage

    private val _uiState = MutableStateFlow(MenuViewState(items = emptyList()))
    val uiState: StateFlow<MenuViewState> = _uiState

    private val _snackbar = MutableSharedFlow<SnackbarMessage>()
    val snackBar = _snackbar.asSharedFlow()

    private val _onSelectedSiteMissing = MutableLiveData<Unit>()
    val onSelectedSiteMissing = _onSelectedSiteMissing as LiveData<Unit>

    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }

        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _onSelectedSiteMissing.value = Unit
            return
        }

        buildSiteMenu()
        isStarted = true
    }

    private fun buildSiteMenu() {
        val site = selectedSiteRepository.getSelectedSite() ?: return

        val currentItems = siteItemsBuilder.build(
            MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                enableFocusPoints = false,
                site = site,
                onClick = this::onClick,
                isBlazeEligible = isSiteBlazeEligible()
            )
        ).filterIsInstance<MySiteCardAndItem.Item>().map {
            it.toMenuItemState()
        }.toList()

        _uiState.value = MenuViewState(items = currentItems)

        rebuildSiteItemsForJetpackCapabilities()
    }

    private fun rebuildSiteItemsForJetpackCapabilities() {
        val site = selectedSiteRepository.getSelectedSite() ?: return

        launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                val currentItems = siteItemsBuilder.build(
                    MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                        enableFocusPoints = false,
                        site = site,
                        onClick = this@MenuViewModel::onClick,
                        isBlazeEligible = isSiteBlazeEligible(),
                        scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                    )
                ).filterIsInstance<MySiteCardAndItem.Item>().map { item ->
                    item.toMenuItemState()
                }.toList()

                _uiState.value = MenuViewState(items = currentItems)
            }
        }
    }

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)

    private fun onClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.MORE_MENU_ITEM_TAPPED,
                mapOf(MENU_ITEM_TRACKING_PARAMETER to action.trackingLabel)
            )
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        }
    }

    fun showSnackbarRequest(item: SnackbarItem) {
        launch(bgDispatcher) {
            handleShowSnackbarRequest(item)
        }
    }

    /*
    * This creates a very lightweight snackbar messages. No action events and no icons. At the
    * point of this function execution, the snackbar is already created and ready to be shown.
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

    override fun onCleared() {
        jetpackCapabilitiesUseCase.clear()
        super.onCleared()
    }

    fun handleSiteRemoved() {
        selectedSiteRepository.removeSite()
        _onSelectedSiteMissing.value = Unit
        return
    }

    data class SnackbarMessage(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration
    )
}
