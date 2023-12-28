package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val dashboardCardsBuilder: CardsBuilder,
) {
    @Suppress("LongParameterList")
    fun build(
        dashboardCardsBuilderParams: DashboardCardsBuilderParams,
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        cards.addAll(dashboardCardsBuilder.build(dashboardCardsBuilderParams))
        return cards
    }
}
