package org.wordpress.android.ui.mysite.cards.plans

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardUtils
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlansCardViewModelSlice @Inject constructor(
    private val dashboardCardPlansUtils: PlansCardUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val plansCardBuilder: PlansCardBuilder,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.DashboardPlansCard?>()
    val uiModel = _uiModel.distinctUntilChanged()

    fun buildCard(site:SiteModel){
         _uiModel.postValue(plansCardBuilder.build(getParams(site)))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getParams(site:SiteModel) = MySiteCardAndItemBuilderParams.DashboardCardPlansBuilderParams(
        isEligible = dashboardCardPlansUtils.shouldShowCard(site),
        onClick = this::onDashboardCardPlansClick,
        onMoreMenuClick = this::onDashboardCardPlansMoreMenuClick,
        onHideMenuItemClick = this::onDashboardCardPlansHideMenuItemClick
    )

    private fun onDashboardCardPlansClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        dashboardCardPlansUtils.trackCardTapped()
        _onNavigation.value = Event(SiteNavigationAction.OpenFreeDomainSearch(selectedSite))
    }

    private fun onDashboardCardPlansMoreMenuClick() {
        dashboardCardPlansUtils.trackCardMoreMenuTapped()
    }

    private fun onDashboardCardPlansHideMenuItemClick() {
        dashboardCardPlansUtils.trackCardHiddenByUser()
        selectedSiteRepository.getSelectedSite()?.let {
            dashboardCardPlansUtils.hideCard(it.siteId)
        }
        _uiModel.postValue(null)
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }

    fun trackShown(position: Int) {
        dashboardCardPlansUtils.trackCardShown(position)
    }

    fun resetShown() {
        dashboardCardPlansUtils.resetShown()
    }
}
