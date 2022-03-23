package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val todaysStatsCardBuilder: TodaysStatsCardBuilder,
    private val postCardBuilder: PostCardBuilder,
    private val bloggingPromptCardBuilder: BloggingPromptCardBuilder,
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
) {
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ): DashboardCards = DashboardCards(
            cards = mutableListOf<DashboardCard>().apply {
                if (dashboardCardsBuilderParams.showErrorCard) {
                    add(createErrorCard(dashboardCardsBuilderParams.onErrorRetryClick))
                } else {
                    todaysStatsCardBuilder.build(dashboardCardsBuilderParams.todaysStatsCardBuilderParams)
                            ?.let { add(it) }
                    addAll(postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams))
                    if (bloggingPromptsFeatureConfig.isEnabled()) {
                        add(
                                bloggingPromptCardBuilder.build(
                                        dashboardCardsBuilderParams.bloggingPromptCardBuilderParams
                                )
                        )
                    }
                }
            }.toList()
    )

    private fun createErrorCard(onErrorRetryClick: () -> Unit) = ErrorCard(
            onRetryClick = ListItemInteraction.create(onErrorRetryClick)
    )
}
