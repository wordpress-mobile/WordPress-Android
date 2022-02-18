package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class TodaysStatsCardBuilder @Inject constructor(
    private val statsUtils: StatsUtils,
    private val appLogWrapper: AppLogWrapper
) {
    fun build(params: TodaysStatsCardBuilderParams) = params.todaysStatsCard?.let {
        val error = it.error
        if (error != null) {
            handleError(error)
        } else {
            createTodaysStatsCardWithData(it)
        }
    }

    private fun handleError(error: TodaysStatsCardError): TodaysStatsCard.Error? {
        error.message?.let { appLogWrapper.e(AppLog.T.MY_SITE_DASHBOARD, "Today's Stats Error: $it") }
        return if (shouldShowError(error)) createTodaysStatsCardWithError() else null
    }

    private fun createTodaysStatsCardWithError() = TodaysStatsCard.Error(
            title = UiStringRes(R.string.my_site_todays_stat_card_title)
    )

    private fun createTodaysStatsCardWithData(model: TodaysStatsCardModel) = TodaysStatsCardWithData(
            views = statToUiString(model.views),
            visitors = statToUiString(model.visitors),
            likes = statToUiString(model.likes)
    )

    private fun shouldShowError(error: TodaysStatsCardError) = error.type == TodaysStatsCardErrorType.GENERIC_ERROR

    private fun statToUiString(stat: Int) = UiStringText(statsUtils.toFormattedString(stat))
}
