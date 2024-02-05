package org.wordpress.android.ui.mysite.items

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackBadge.JetpackBadgeViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackSwitchmenu.JetpackSwitchMenuViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackfeaturecard.JetpackFeatureCardViewModelSlice
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.merge
import javax.inject.Inject

class DashboardItemsViewModelSlice @Inject constructor(
    private val jetpackFeatureCardViewModelSlice: JetpackFeatureCardViewModelSlice,
    private val jetpackSwitchMenuViewModelSlice: JetpackSwitchMenuViewModelSlice,
    private val jetpackBadgeViewModelSlice: JetpackBadgeViewModelSlice,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val sotw2023NudgeCardViewModelSlice: WpSotw2023NudgeCardViewModelSlice,
    private val jetpackFeatureCardHelper: JetpackFeatureCardHelper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    private lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        sotw2023NudgeCardViewModelSlice.initialize(scope)
    }

    val onNavigation = merge(
        jetpackFeatureCardViewModelSlice.onNavigation,
        jetpackSwitchMenuViewModelSlice.onNavigation,
        jetpackBadgeViewModelSlice.onNavigation,
        siteItemsViewModelSlice.onNavigation,
        sotw2023NudgeCardViewModelSlice.onNavigation
    )

    val uiModel: MutableLiveData<List<MySiteCardAndItem>> = merge(
        jetpackFeatureCardViewModelSlice.uiModel,
        jetpackSwitchMenuViewModelSlice.uiModel,
        jetpackBadgeViewModelSlice.uiModel,
        siteItemsViewModelSlice.uiModel,
        sotw2023NudgeCardViewModelSlice.uiModel
    ) {
       jetpackFeatureCard, jetpackSwitchMenu, jetpackBadge, siteItems, sotw2023NudgeCard ->
        mergeUiModels(
            jetpackFeatureCard,
            jetpackSwitchMenu,
            jetpackBadge,
            siteItems,
            sotw2023NudgeCard
        )
    }.distinctUntilChanged() as MutableLiveData<List<MySiteCardAndItem>>

    val onSnackbarMessage = merge(
        siteItemsViewModelSlice.onSnackbarMessage,
    )

    private fun mergeUiModels(
        jetpackFeatureCard: MySiteCardAndItem.Card.JetpackFeatureCard?,
        jetpackSwitchMenu: MySiteCardAndItem.Card.JetpackSwitchMenu?,
        jetpackBadge: MySiteCardAndItem.JetpackBadge?,
        siteItems: List<MySiteCardAndItem>?,
        sotw2023NudgeCard: MySiteCardAndItem.Card.WpSotw2023NudgeCardModel?
    ): List<MySiteCardAndItem> {
        val dasbhboardSiteItems = mutableListOf<MySiteCardAndItem>().apply {
            sotw2023NudgeCard?.let { add(it) }
            siteItems?.let { addAll(siteItems) }
            jetpackSwitchMenu?.let { add(jetpackSwitchMenu) }
            if (jetpackFeatureCardHelper.shouldShowFeatureCardAtTop())
                jetpackFeatureCard?.let { add(0, jetpackFeatureCard) }
            else jetpackFeatureCard?.let { add(jetpackFeatureCard) }
            jetpackBadge?.let { add(jetpackBadge) }
        }.toList()
        return dasbhboardSiteItems
    }

    fun onResume(site: SiteModel) {
        if (shouldShowSiteItems(site)) buildCards(site)
        else uiModel.postValue(emptyList())
    }

    fun onRefresh(site: SiteModel) {
        if (shouldShowSiteItems(site)) buildCards(site)
        else uiModel.postValue(emptyList())
    }

    private fun buildCards(site: SiteModel) {
        scope.launch {
            jetpackFeatureCardViewModelSlice.buildJetpackFeatureCard()
            jetpackSwitchMenuViewModelSlice.buildJetpackSwitchMenu()
            jetpackBadgeViewModelSlice.buildJetpackBadge()
            siteItemsViewModelSlice.buildSiteItems(site)
            sotw2023NudgeCardViewModelSlice.buildCard()
        }
    }

    private fun shouldShowSiteItems(site: SiteModel) =
        (buildConfigWrapper.isJetpackApp && site.isUsingWpComRestApi).not()


    fun onCleared() {
        scope.cancel()
    }
}
