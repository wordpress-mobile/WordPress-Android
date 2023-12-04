package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.BlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.bloganuary.BloganuaryNudgeCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.domaintransfer.DomainTransferCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val todaysStatsCardBuilder: TodaysStatsCardBuilder,
    private val postCardBuilder: PostCardBuilder,
    private val bloganuaryNudgeCardBuilder: BloganuaryNudgeCardBuilder,
    private val bloggingPromptCardBuilder: BloggingPromptCardBuilder,
    private val domainTransferCardBuilder: DomainTransferCardBuilder,
    private val blazeCardBuilder: BlazeCardBuilder,
    private val plansCardBuilder: PlansCardBuilder,
    private val pagesCardBuilder: PagesCardBuilder,
    private val activityCardBuilder: ActivityCardBuilder
) {
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ) = mutableListOf<MySiteCardAndItem.Card>().apply {
        if (dashboardCardsBuilderParams.showErrorCard) {
            add(createErrorCard(dashboardCardsBuilderParams.onErrorRetryClick))
        } else {
            bloganuaryNudgeCardBuilder.build(dashboardCardsBuilderParams.bloganuaryNudgeCardBuilderParams)
                ?.let { add(it) }

            bloggingPromptCardBuilder.build(dashboardCardsBuilderParams.bloggingPromptCardBuilderParams)
                ?.let { add(it) }

            if (dashboardCardsBuilderParams.blazeCardBuilderParams != null) {
                add(blazeCardBuilder.build(dashboardCardsBuilderParams.blazeCardBuilderParams))
            }

            plansCardBuilder.build(dashboardCardsBuilderParams.dashboardCardPlansBuilderParams)?.let {
                add(it)
            }

            todaysStatsCardBuilder.build(dashboardCardsBuilderParams.todaysStatsCardBuilderParams)
                ?.let { add(it) }

            addAll(postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams))

            pagesCardBuilder.build(dashboardCardsBuilderParams.pagesCardBuilderParams)?.let { add(it) }

            activityCardBuilder.build(dashboardCardsBuilderParams.activityCardBuilderParams)?.let { add(it) }

            domainTransferCardBuilder
                .build(dashboardCardsBuilderParams.domainTransferCardBuilderParams)
                ?.let { add(it) }
        }
    }.toList()

    private fun createErrorCard(onErrorRetryClick: () -> Unit) = MySiteCardAndItem.Card.ErrorCard(
        onRetryClick = ListItemInteraction.create(onErrorRetryClick)
    )
}
