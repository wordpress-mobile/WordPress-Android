package org.wordpress.android.ui.mysite.cards.dashboard.todaystat

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

@Suppress("TooManyFunctions")
class TodaysStatsCardBuilder @Inject constructor(
    private val statsUtils: StatsUtils
) {
    fun build(params: TodaysStatsCardBuilderParams) = params.todaysStatsCard?.let {
        TodaysStatsCard(
                views = statToUIString(it.views),
                visitors = statToUIString(it.visitors),
                likes = statToUIString(it.likes)
        )
    }

    private fun statToUIString(stat: Int) = UiStringText(statsUtils.toFormattedString(stat))
}
