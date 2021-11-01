package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.cards.post.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import javax.inject.Inject

@Suppress("LongParameterList")
class CardsBuilder @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val siteInfoCardBuilder: SiteInfoCardBuilder,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val quickActionsCardBuilder: QuickActionsCardBuilder,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val postCardBuilder: PostCardBuilder,
    private val mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
) {
    fun build(
        domainRegistrationCardBuilderParams: DomainRegistrationCardBuilderParams,
        postCardBuilderParams: PostCardBuilderParams,
        quickActionsCardBuilderParams: QuickActionsCardBuilderParams,
        quickStartCardBuilderParams: QuickStartCardBuilderParams,
        siteInfoCardBuilderParams: SiteInfoCardBuilderParams
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
            cards.addAll(postCardBuilder.build(postCardBuilderParams))
        }
        return cards
    }

    private fun trackAndBuildDomainRegistrationCard(
        params: DomainRegistrationCardBuilderParams
    ): DomainRegistrationCard {
        analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN)
        return DomainRegistrationCard(ListItemInteraction.create(params.domainRegistrationClick))
    }
}
