package org.wordpress.android.ui.mysite

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.Result
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.FetchCampaignListUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.CampaignWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.blaze.MostRecentCampaignUseCase
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BlazeCardViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardsTracker: CardsTracker,
    private val blazeCardBuilder: BlazeCardBuilder,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val fetchCampaignListUseCase: FetchCampaignListUseCase,
    private val mostRecentCampaignUseCase: MostRecentCampaignUseCase,
) {
    private lateinit var scope: CoroutineScope

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.BlazeCard?>()
    val uiModel = _uiModel.distinctUntilChanged()

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun buildCard(site: SiteModel) {
        _isRefreshing.postValue(true)
        scope.launch(bgDispatcher){
            if (blazeFeatureUtils.shouldShowBlazeCardEntryPoint(site)) {
                if (blazeFeatureUtils.shouldShowBlazeCampaigns()) {
                    fetchCampaigns(site)
                } else {
                    // show blaze promo card if campaign feature is not available
                    showPromoteWithBlazeCard()
                }
            } else {
                postState(false)
            }
        }
    }

    private suspend fun fetchCampaigns(site: SiteModel) {
        if (networkUtilsWrapper.isNetworkAvailable().not()) {
            getMostRecentCampaignFromDb(site)
        } else {
            when (fetchCampaignListUseCase.execute(site = site, offset = 0)) {
                is Result.Success -> getMostRecentCampaignFromDb(site)
                // there are no campaigns or if there is an error , show blaze promo card
                is Result.Failure -> showPromoteWithBlazeCard()
            }
        }
    }

    private suspend fun getMostRecentCampaignFromDb(site: SiteModel) {
        when(val result = mostRecentCampaignUseCase.execute(site)) {
            is Result.Success -> postState(true, campaign = result.value)
            is Result.Failure -> showPromoteWithBlazeCard()
        }
    }

    private fun showPromoteWithBlazeCard() {
        postState(true)
    }

    private fun postState(isBlazeEligible: Boolean, campaign: BlazeCampaignModel? = null) {
        _isRefreshing.postValue(false)
        if(isBlazeEligible) {
            buildBlazeCard(campaign)?.let {
                _uiModel.postValue(it)
            }
        } else {
            _uiModel.postValue(null)
        }
    }


    private fun buildBlazeCard(campaign: BlazeCampaignModel? = null): MySiteCardAndItem.Card.BlazeCard? {
        return getBlazeCardBuilderParams(campaign).let { blazeCardBuilder.build(it) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getBlazeCardBuilderParams(campaign: BlazeCampaignModel? = null): BlazeCardBuilderParams {
        return campaign?.let {
                    getCampaignWithBlazeCardBuilderParams(campaign)
                } ?: getPromoteWithBlazeCardBuilderParams()
    }

    private fun getCampaignWithBlazeCardBuilderParams(campaign: BlazeCampaignModel) =
        CampaignWithBlazeCardBuilderParams(
            campaign = campaign,
            onCreateCampaignClick = this::onCreateCampaignClick,
            onCampaignClick = this::onCampaignClick,
            onCardClick = this::onCampaignsCardClick,
            moreMenuParams = CampaignWithBlazeCardBuilderParams.MoreMenuParams(
                viewAllCampaignsItemClick = this::onViewAllCampaignsClick,
                onLearnMoreClick = this::onCampaignCardLearnMoreClick,
                onHideThisCardItemClick = this::onCampaignCardHideMenuItemClick,
                onMoreMenuClick = this::onCampaignCardMoreMenuClick
            )
        )

    private fun onViewAllCampaignsClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.VIEW_ALL_CAMPAIGNS.label
        )
        _onNavigation.value =
            Event(SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.DASHBOARD_CARD))
    }

    private fun onCampaignCardLearnMoreClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.LEARN_MORE.label
        )
        onLearnMoreClick()
    }

    private fun onCampaignCardHideMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLAZE_CAMPAIGNS.label,
            CampaignCardMenuItem.HIDE_THIS.label
        )
        blazeFeatureUtils.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_HIDE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        onHideCardClick()
    }

    private fun onCampaignCardMoreMenuClick() {
        cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.BLAZE_CAMPAIGNS.label)
        blazeFeatureUtils.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_MENU_ACCESSED,
            BlazeFlowSource.DASHBOARD_CARD
        )
    }


    private fun getPromoteWithBlazeCardBuilderParams() =
        PromoteWithBlazeCardBuilderParams(
            onClick = this::onPromoteWithBlazeCardClick,
            moreMenuParams = PromoteWithBlazeCardBuilderParams.MoreMenuParams(
                onLearnMoreClick = this::onPromoteCardLearnMoreClick,
                onHideThisCardItemClick = this::onPromoteCardHideMenuItemClick,
                onMoreMenuClick = this::onPromoteCardMoreMenuClick
            )
        )

    private fun onPromoteCardLearnMoreClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.PROMOTE_WITH_BLAZE.label,
            PromoteWithBlazeCardMenuItem.LEARN_MORE.label
        )
        onLearnMoreClick()
    }

    private fun onLearnMoreClick() {
        blazeFeatureUtils.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_LEARN_MORE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        _onNavigation.value =
            Event(
                SiteNavigationAction.OpenPromoteWithBlazeOverlay(
                    source = BlazeFlowSource.DASHBOARD_CARD,
                    shouldShowBlazeOverlay = true
                )
            )
    }

    private fun onPromoteCardHideMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.PROMOTE_WITH_BLAZE.label,
            PromoteWithBlazeCardMenuItem.HIDE_THIS.label
        )
        blazeFeatureUtils.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_HIDE_TAPPED,
            BlazeFlowSource.DASHBOARD_CARD
        )
        onHideCardClick()
    }

    private fun onHideCardClick() {
        selectedSiteRepository.getSelectedSite()?.let {
            blazeFeatureUtils.hideBlazeCard(it.siteId)
        }
        _uiModel.postValue(null)
    }

    private fun onPromoteCardMoreMenuClick() {
        cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.PROMOTE_WITH_BLAZE.label)
        blazeFeatureUtils.track(
            AnalyticsTracker.Stat.BLAZE_ENTRY_POINT_MENU_ACCESSED,
            BlazeFlowSource.DASHBOARD_CARD
        )
    }

    private fun onCreateCampaignClick() {
        blazeFeatureUtils.trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
        _onNavigation.value =
            Event(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
    }

    private fun onCampaignClick(campaignId: String) {
        _onNavigation.value =
            Event(SiteNavigationAction.OpenCampaignDetailPage(campaignId, CampaignDetailPageSource.DASHBOARD_CARD))
    }

    private fun onCampaignsCardClick() {
        _onNavigation.value =
            Event(SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.DASHBOARD_CARD))
    }

    private fun onPromoteWithBlazeCardClick() {
        blazeFeatureUtils.trackEntryPointTapped(BlazeFlowSource.DASHBOARD_CARD)
        _onNavigation.value =
            Event(SiteNavigationAction.OpenPromoteWithBlazeOverlay(source = BlazeFlowSource.DASHBOARD_CARD))
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}

enum class CampaignCardMenuItem(val label: String) {
    VIEW_ALL_CAMPAIGNS("view_all_campaigns"),
    LEARN_MORE("learn_more"),
    HIDE_THIS("hide_this")
}

enum class PromoteWithBlazeCardMenuItem(val label: String) {
    LEARN_MORE("learn_more"),
    HIDE_THIS("hide_this")
}

