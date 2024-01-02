package org.wordpress.android.ui.mysite.items

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackBadge.JetpackBadgeViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackSwitchmenu.JetpackSwitchMenuViewModelSlice
import org.wordpress.android.ui.mysite.items.jetpackfeaturecard.JetpackFeatureCardViewModelSlice
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.util.merge
import javax.inject.Inject

class DashboardItemsViewModelSlice @Inject constructor(
    private val jetpackFeatureCardViewModelSlice: JetpackFeatureCardViewModelSlice,
    private val jetpackSwitchMenuViewModelSlice: JetpackSwitchMenuViewModelSlice,
    private val jetpackBadgeViewModelSlice: JetpackBadgeViewModelSlice,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val sotw2023NudgeCardViewModelSlice: WpSotw2023NudgeCardViewModelSlice,
    private val jetpackFeatureCardHelper: JetpackFeatureCardHelper,
    private val selectedSiteRepository: SelectedSiteRepository
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

    val uiModel: LiveData<List<MySiteCardAndItem>> = merge(
        jetpackFeatureCardViewModelSlice.uiModel,
        jetpackSwitchMenuViewModelSlice.uiModel,
        jetpackBadgeViewModelSlice.uiModel,
        siteItemsViewModelSlice.uiModel,
        sotw2023NudgeCardViewModelSlice.uiModel
    ) { jetpackFeatureCard, jetpackSwitchMenu, jetpackBadge, siteItems, sotw2023NudgeCard ->
        mergeUiModels(
            jetpackFeatureCard,
            jetpackSwitchMenu,
            jetpackBadge,
            siteItems,
            sotw2023NudgeCard
        )
    }

    fun mergeUiModels(
        jetpackFeatureCard: MySiteCardAndItem.Card.JetpackFeatureCard?,
        jetpackSwitchMenu: MySiteCardAndItem.Card.JetpackSwitchMenu?,
        jetpackBadge: MySiteCardAndItem.JetpackBadge?,
        siteItems: List<MySiteCardAndItem>?,
        sotw2023NudgeCard: MySiteCardAndItem.Card.WpSotw2023NudgeCardModel?
    ): List<MySiteCardAndItem> {
        return mutableListOf<MySiteCardAndItem>().apply {
            sotw2023NudgeCard?.let { add(it) }
            siteItems?.all { addAll(siteItems) }
            jetpackSwitchMenu?.let { add(jetpackSwitchMenu) }
            if (jetpackFeatureCardHelper.shouldShowFeatureCardAtTop())
                jetpackFeatureCard?.let { add(0, jetpackFeatureCard) }
            else jetpackFeatureCard?.let { add(jetpackFeatureCard) }
            jetpackBadge?.let { add(jetpackBadge) }
        }.toList()
    }

    fun buildCards() {
       selectedSiteRepository.getSelectedSite()?.let { site ->
           scope.launch {
               jetpackFeatureCardViewModelSlice.buildJetpackFeatureCard()
               jetpackSwitchMenuViewModelSlice.buildJetpackSwitchMenu()
               jetpackBadgeViewModelSlice.buildJetpackBadge()
               siteItemsViewModelSlice.buildSiteItems(site)
               sotw2023NudgeCardViewModelSlice.buildCard()
           }
       }
    }

    fun onCleared() {
        scope.cancel()
    }
}
