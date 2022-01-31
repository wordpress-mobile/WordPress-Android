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
                views = UiStringText(statsUtils.toFormattedString(it.views)),
                visitors = UiStringText(statsUtils.toFormattedString(it.visitors)),
                likes = UiStringText(statsUtils.toFormattedString(it.likes))
        )
    }
}
