package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardBuilder
import org.wordpress.android.ui.mysite.cards.quicklinksribbon.QuickLinkRibbonBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val quickLinkRibbonBuilder: QuickLinkRibbonBuilder,
    private val dashboardCardsBuilder: CardsBuilder,
    private val jetpackInstallFullPluginCardBuilder: JetpackInstallFullPluginCardBuilder,
) {
    @Suppress("LongParameterList")
    fun build(
        domainRegistrationCardBuilderParams: DomainRegistrationCardBuilderParams,
        quickStartCardBuilderParams: QuickStartCardBuilderParams,
        dashboardCardsBuilderParams: DashboardCardsBuilderParams,
        quickLinkRibbonBuilderParams: QuickLinkRibbonBuilderParams,
        jetpackInstallFullPluginCardBuilderParams: JetpackInstallFullPluginCardBuilderParams,
        isMySiteTabsEnabled: Boolean
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        if (isMySiteTabsEnabled) {
            cards.add(quickLinkRibbonBuilder.build(quickLinkRibbonBuilderParams))
        }
        jetpackInstallFullPluginCardBuilder.build(jetpackInstallFullPluginCardBuilderParams)?.let {
            cards.add(it)
        }
        if (domainRegistrationCardBuilderParams.isDomainCreditAvailable) {
            cards.add(trackAndBuildDomainRegistrationCard(domainRegistrationCardBuilderParams))
        }
        quickStartCardBuilderParams.quickStartCategories.takeIf { it.isNotEmpty() }?.let {
            cards.add(quickStartCardBuilder.build(quickStartCardBuilderParams))
        }
        cards.add(dashboardCardsBuilder.build(dashboardCardsBuilderParams))
        return cards
    }

    private fun trackAndBuildDomainRegistrationCard(
        params: DomainRegistrationCardBuilderParams
    ): DomainRegistrationCard {
        return DomainRegistrationCard(ListItemInteraction.create(params.domainRegistrationClick))
    }
}
