package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import javax.inject.Inject

class CardsBuilder @Inject constructor(
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val dashboardCardsBuilder: CardsBuilder,
) {
    @Suppress("LongParameterList")
    fun build(
        quickStartCardBuilderParams: QuickStartCardBuilderParams,
        dashboardCardsBuilderParams: DashboardCardsBuilderParams,
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        quickStartCardBuilderParams.quickStartCategories.takeIf { it.isNotEmpty() }?.let {
            cards.add(quickStartCardBuilder.build(quickStartCardBuilderParams))
        }
        cards.addAll(dashboardCardsBuilder.build(dashboardCardsBuilderParams))
        return cards
    }
}
