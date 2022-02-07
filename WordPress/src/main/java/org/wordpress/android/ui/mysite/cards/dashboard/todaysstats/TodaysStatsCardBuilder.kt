package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class TodaysStatsCardBuilder @Inject constructor(
    private val statsUtils: StatsUtils
) {
    fun build(params: TodaysStatsCardBuilderParams) = params.todaysStatsCard?.let {
        TodaysStatsCardWithData(
                views = statToUiString(it.views),
                visitors = statToUiString(it.visitors),
                likes = statToUiString(it.likes)
        )
    }

    private fun statToUiString(stat: Int) = UiStringText(statsUtils.toFormattedString(stat))
}
