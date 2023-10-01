package org.wordpress.android.ui.mysite.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
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
import org.wordpress.android.util.JetpackMigrationLanguageUtil
import org.wordpress.android.util.LocaleManagerWrapper
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

    private val _refreshAppLanguage = MutableLiveData<String>()
    val refreshAppLanguage: LiveData<String> = _refreshAppLanguage

    private val _uiState = MutableStateFlow(MenuViewState(items = emptyList()))

    val uiState: StateFlow<MenuViewState> = _uiState

    init {
        emitLanguageRefreshIfNeeded(localeManagerWrapper.getLanguage())
    }

    fun start() {
        val site = selectedSiteRepository.getSelectedSite()!!
        buildSiteMenu(site)
    }

    private fun buildSiteMenu(site: SiteModel) {
        _uiState.value = MenuViewState(items = siteItemsBuilder.build(
                MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                    enableFocusPoints = true,
                    site = site,
                    activeTask = null,
                    onClick = this::onClick,
                    isBlazeEligible = isSiteBlazeEligible()
                )
            ).filterIsInstance<MySiteCardAndItem.Item>().map {
                it.toMenuItemState()
            }.toList()
        )

        updateSiteItemsForJetpackCapabilities(site)
    }

    private fun updateSiteItemsForJetpackCapabilities(site: SiteModel) {
        launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                _uiState.value = MenuViewState(
                    items = siteItemsBuilder.build(
                        MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                            site = site,
                            enableFocusPoints = true,
                            activeTask = null,
                            onClick = this@MenuViewModel::onClick,
                            isBlazeEligible = isSiteBlazeEligible(),
                            backupAvailable = it.backup,
                            scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                        )
                    ).filterIsInstance<MySiteCardAndItem.Item>().map { item ->
                        item.toMenuItemState()
                    }.toList()
                )
            } // end collect
        }
    }

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)


    private fun onClick(action: ListItemAction) {
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
            // add the tracking logic here
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        }
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
