package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DashboardCardType.POST_CARD_WITHOUT_POST_ITEMS
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.blaze.PromoteWithBlazeCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val todaysStatsCardBuilder: TodaysStatsCardBuilder,
    private val postCardBuilder: PostCardBuilder,
    private val bloggingPromptCardBuilder: BloggingPromptCardBuilder,
    private val promoteWithBlazeCardBuilder: PromoteWithBlazeCardBuilder
) {
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ): DashboardCards = DashboardCards(
        cards = mutableListOf<DashboardCard>().apply {
            if (dashboardCardsBuilderParams.showErrorCard) {
                add(createErrorCard(dashboardCardsBuilderParams.onErrorRetryClick))
            } else {
                var bloggingPromptCardAdded = false
                bloggingPromptCardBuilder.build(dashboardCardsBuilderParams.bloggingPromptCardBuilderParams)
                    ?.let {
                        bloggingPromptCardAdded = true
                        add(it)
                    }

                promoteWithBlazeCardBuilder.build(dashboardCardsBuilderParams.promoteWithBlazeCardBuilderParams)?.let {
                    add(it)
                }

                todaysStatsCardBuilder.build(dashboardCardsBuilderParams.todaysStatsCardBuilderParams)
                    ?.let { add(it) }

                // if blogging prompt card is visible and the post card is "Write first/next post" we only show
                // blogging prompt, since they are very similar
                val postCards = postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams)
                val hasNextPostPrompt = postCards.find {
                    it.dashboardCardType == POST_CARD_WITHOUT_POST_ITEMS
                } != null
                val showPostCards = !hasNextPostPrompt || !bloggingPromptCardAdded

                if (showPostCards) {
                    addAll(postCards)
                }
            }
        }.toList()
    )

    private fun createErrorCard(onErrorRetryClick: () -> Unit) = ErrorCard(
        onRetryClick = ListItemInteraction.create(onErrorRetryClick)
    )
}
