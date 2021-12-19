package org.wordpress.android.ui.mysite.cards.dashboard

import org.wordpress.android.ui.mysite.MySiteCardAndItem
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
    ): List<MySiteCardAndItem> = mutableListOf<MySiteCardAndItem>().apply {
        val cards = mutableListOf<MySiteCardAndItem>()
        if (mySiteDashboardPhase2FeatureConfig.isEnabled()) {
            cards.addAll(postCardBuilder.build(dashboardCardsBuilderParams.postCardBuilderParams))
        }
        return cards
    }
}
