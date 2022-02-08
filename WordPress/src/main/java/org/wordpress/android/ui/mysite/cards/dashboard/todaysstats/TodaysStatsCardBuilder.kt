package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class TodaysStatsCardBuilder @Inject constructor(
    private val statsUtils: StatsUtils
) {
    fun build(params: TodaysStatsCardBuilderParams) = params.todaysStatsCard?.let {
        TodaysStatsCard(
                views = statToUiString(it.views),
                visitors = statToUiString(it.visitors),
                likes = statToUiString(it.likes),
                onCardClick = params.onCardClick
        )
    }

    private fun statToUiString(stat: Int) = UiStringText(statsUtils.toFormattedString(stat))
}
