package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quicklinksribbon.QuickLinkRibbonBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val quickActionsCardBuilder: QuickActionsCardBuilder,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val quickLinkRibbonBuilder: QuickLinkRibbonBuilder,
    private val dashboardCardsBuilder: CardsBuilder
) {
    @Suppress("LongParameterList")
    fun build(
        quickActionsCardBuilderParams: QuickActionsCardBuilderParams,
        domainRegistrationCardBuilderParams: DomainRegistrationCardBuilderParams,
        quickStartCardBuilderParams: QuickStartCardBuilderParams,
        dashboardCardsBuilderParams: DashboardCardsBuilderParams,
        quickLinkRibbonBuilderParams: QuickLinkRibbonBuilderParams,
        isMySiteTabsEnabled: Boolean
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        if (isMySiteTabsEnabled) {
            cards.add(quickLinkRibbonBuilder.build(quickLinkRibbonBuilderParams))
        }
        if (shouldShowQuickActionsCard(isMySiteTabsEnabled)) {
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
        cards.add(dashboardCardsBuilder.build(dashboardCardsBuilderParams))
        return cards
    }

    private fun shouldShowQuickActionsCard(isMySiteTabsEnabled: Boolean): Boolean {
        return buildConfigWrapper.isQuickActionEnabled && !isMySiteTabsEnabled
    }

    private fun trackAndBuildDomainRegistrationCard(
        params: DomainRegistrationCardBuilderParams
    ): DomainRegistrationCard {
        return DomainRegistrationCard(ListItemInteraction.create(params.domainRegistrationClick))
    }
}
