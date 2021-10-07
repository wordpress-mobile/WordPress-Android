package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import javax.inject.Inject

class CardsBuilder
@Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val siteInfoCardBuilder: SiteInfoCardBuilder,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val quickActionsCardBuilder: QuickActionsCardBuilder,
    private val quickStartCardBuilder: QuickStartCardBuilder
) {
    @Suppress("LongParameterList")
    fun build(
        site: SiteModel,
        showSiteIconProgressBar: Boolean,
        activeTask: QuickStartTask?,
        isDomainCreditAvailable: Boolean,
        quickStartCategories: List<QuickStartCategory>,
        titleClick: () -> Unit,
        iconClick: () -> Unit,
        urlClick: () -> Unit,
        switchSiteClick: () -> Unit,
        quickActionStatsClick: () -> Unit,
        quickActionPagesClick: () -> Unit,
        quickActionPostsClick: () -> Unit,
        quickActionMediaClick: () -> Unit,
        domainRegistrationClick: () -> Unit,
        onQuickStartBlockRemoveMenuItemClick: () -> Unit,
        onQuickStartTaskTypeItemClick: (type: QuickStartTaskType) -> Unit
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        cards.add(
                siteInfoCardBuilder.buildSiteInfoCard(
                        site,
                        showSiteIconProgressBar,
                        titleClick,
                        iconClick,
                        urlClick,
                        switchSiteClick,
                        activeTask == QuickStartTask.UPDATE_SITE_TITLE,
                        activeTask == QuickStartTask.UPLOAD_SITE_ICON
                )
        )
        if (!buildConfigWrapper.isJetpackApp) {
            cards.add(
                    quickActionsCardBuilder.build(
                            quickActionStatsClick,
                            quickActionPagesClick,
                            quickActionPostsClick,
                            quickActionMediaClick,
                            site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                            activeTask == QuickStartTask.CHECK_STATS,
                            activeTask == QuickStartTask.EDIT_HOMEPAGE || activeTask == QuickStartTask.REVIEW_PAGES
                    )
            )
        }
        if (isDomainCreditAvailable) {
            analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_PROMPT_SHOWN)
            cards.add(DomainRegistrationCard(ListItemInteraction.create(domainRegistrationClick)))
        }

        if (!quickStartDynamicCardsFeatureConfig.isEnabled()) {
            quickStartCategories.takeIf { it.isNotEmpty() }?.let {
                cards.add(
                        quickStartCardBuilder.build(
                                quickStartCategories,
                                onQuickStartBlockRemoveMenuItemClick,
                                onQuickStartTaskTypeItemClick
                        )
                )
            }
        }
        return cards
    }
}
