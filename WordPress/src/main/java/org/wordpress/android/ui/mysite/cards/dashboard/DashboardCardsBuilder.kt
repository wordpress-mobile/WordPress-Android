package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import javax.inject.Inject

class DashboardCardsBuilder @Inject constructor(
    private val postCardBuilder: PostCardBuilder,
    private val mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
) {
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams
    ): DashboardCards = DashboardCards(
            cards = mutableListOf<DashboardCard>().apply {
                if (mySiteDashboardPhase2FeatureConfig.isEnabled()) {
                    if (dashboardCardsBuilderParams.showErrorCard) {
                        add(createErrorCard())
                    } else {
                        addAll(postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams))
                    }
                }
            }.toList()
    )

    private fun createErrorCard() = ErrorCard()
}
