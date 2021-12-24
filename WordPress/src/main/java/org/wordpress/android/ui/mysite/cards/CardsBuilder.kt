package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import javax.inject.Inject

@Suppress("LongParameterList")
class CardsBuilder @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val siteInfoCardBuilder: SiteInfoCardBuilder,
    private val quickActionsCardBuilder: QuickActionsCardBuilder,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val dashboardCardsBuilder: CardsBuilder,
    private val mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
) {
    fun build(
        siteInfoCardBuilderParams: SiteInfoCardBuilderParams,
        quickActionsCardBuilderParams: QuickActionsCardBuilderParams,
        domainRegistrationCardBuilderParams: DomainRegistrationCardBuilderParams,
        quickStartCardBuilderParams: QuickStartCardBuilderParams,
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        cards.add(siteInfoCardBuilder.buildSiteInfoCard(siteInfoCardBuilderParams))
        if (!buildConfigWrapper.isJetpackApp) {
            cards.add(quickActionsCardBuilder.build(quickActionsCardBuilderParams))
        }
        if (domainRegistrationCardBuilderParams.isDomainCreditAvailable) {
            cards.add(trackAndBuildDomainRegistrationCard(domainRegistrationCardBuilderParams))
        }
        if (!quickStartDynamicCardsFeatureConfig.isEnabled()) {
            quickStartCardBuilderParams.quickStartCategories.takeIf { it.isNotEmpty() }?.let {
                cards.add(quickStartCardBuilder.build(quickStartCardBuilderParams))
            }
        }
        if (mySiteDashboardPhase2FeatureConfig.isEnabled()) {
            cards.add(dashboardCardsBuilder.build(dashboardCardsBuilderParams))
        }
        return cards
    }

    private fun trackAndBuildDomainRegistrationCard(
        params: DomainRegistrationCardBuilderParams
    ): DomainRegistrationCard {
        return DomainRegistrationCard(ListItemInteraction.create(params.domainRegistrationClick))
    }
}
