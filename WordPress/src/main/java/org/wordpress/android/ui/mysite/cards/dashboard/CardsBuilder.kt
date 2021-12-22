package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val postCardBuilder: PostCardBuilder
) {
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ): DashboardCards = DashboardCards(
            cards = mutableListOf<DashboardCard>().apply {
                if (dashboardCardsBuilderParams.showErrorCard) {
                    add(createErrorCard())
                } else {
                    addAll(postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams))
                }
            }.toList()
    )

    private fun createErrorCard() = ErrorCard()
}
