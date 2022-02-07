package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

class TodaysStatsCardBuilder @Inject constructor(
    private val statsUtils: StatsUtils
) {
    fun build(params: TodaysStatsCardBuilderParams) = params.todaysStatsCard?.let {
        if (it.error != null) {
            createTodaysStatsCardWithError()
        } else {
            createTodaysStatsCardWithData(it)
        }
    }

    private fun createTodaysStatsCardWithError() = TodaysStatsCard.Error(
            title = UiStringRes(R.string.my_site_todays_stat_card_title)
    )

    private fun createTodaysStatsCardWithData(model: TodaysStatsCardModel) = TodaysStatsCardWithData(
            views = statToUiString(model.views),
            visitors = statToUiString(model.visitors),
            likes = statToUiString(model.likes)
    )

    private fun statToUiString(stat: Int) = UiStringText(statsUtils.toFormattedString(stat))
}
