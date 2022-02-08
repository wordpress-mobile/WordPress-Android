package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

private const val TODAYS_STATS_VIEWS = 10000
private const val TODAYS_STATS_VISITORS = 1000
private const val TODAYS_STATS_LIKES = 100
private const val TODAYS_STATS_COMMENTS = 1000

private const val TODAYS_STATS_VIEWS_FORMATTED_STRING = "10,000"
private const val TODAYS_STATS_VISITORS_FORMATTED_STRING = "1,000"
private const val TODAYS_STATS_LIKES_FORMATTED_STRING = "100"

@RunWith(MockitoJUnitRunner::class)
class TodaysStatsCardBuilderTest : BaseUnitTest() {
    @Mock private lateinit var statsUtils: StatsUtils

    private lateinit var builder: TodaysStatsCardBuilder
    private val todaysStatsCardModel = TodaysStatsCardModel(
            views = TODAYS_STATS_VIEWS,
            visitors = TODAYS_STATS_VISITORS,
            likes = TODAYS_STATS_LIKES,
            comments = TODAYS_STATS_COMMENTS
    )

    @Before
    fun setUp() {
        builder = TodaysStatsCardBuilder(statsUtils)
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(statsUtils.toFormattedString(TODAYS_STATS_VIEWS)).thenReturn(TODAYS_STATS_VIEWS_FORMATTED_STRING)
        whenever(statsUtils.toFormattedString(TODAYS_STATS_VISITORS)).thenReturn(TODAYS_STATS_VISITORS_FORMATTED_STRING)
        whenever(statsUtils.toFormattedString(TODAYS_STATS_LIKES)).thenReturn(TODAYS_STATS_LIKES_FORMATTED_STRING)
    }

    /* TODAY'S STATS CARD ERROR */

    @Test
    fun `given jetpack disconnected error, when card is built, then card not exists`() {
        val todaysStatsCardModel = TodaysStatsCardModel(
                error = TodaysStatsCardError(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)
        )

        val todaysStatsCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(todaysStatsCard).isNull()
    }

    @Test
    fun `given jetpack disabled error, when card is built, then card not exists`() {
        val todaysStatsCardModel = TodaysStatsCardModel(
                error = TodaysStatsCardError(TodaysStatsCardErrorType.JETPACK_DISABLED)
        )

        val todaysStatsCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(todaysStatsCard).isNull()
    }

    @Test
    fun `given today's stats unauth error, when card is built, then card not exists`() {
        val todaysStatsCardModel = TodaysStatsCardModel(
                error = TodaysStatsCardError(TodaysStatsCardErrorType.UNAUTHORIZED)
        )

        val todaysStatsCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(todaysStatsCard).isNull()
    }

    @Test
    fun `given today's stats generic error, when card is built, then error card exists`() {
        val todaysStatsCardModel = TodaysStatsCardModel(
                error = TodaysStatsCardError(TodaysStatsCardErrorType.GENERIC_ERROR)
        )

        val todaysStatsCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(todaysStatsCard).isInstanceOf(TodaysStatsCard.Error::class.java)
    }

    @Test
    fun `given no todays stats, when card is built, then return null`() {
        val statCard = buildTodaysStatsCard(null)

        assertThat(statCard).isNull()
    }

    @Test
    fun `given todays stats, when card is built, then return stat card`() {
        val statCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(statCard).isNotNull
    }

    @Test
    fun `given todays stats, when card is built, then stat count exists`() {
        val statCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat(statCard).isEqualTo(todaysStatsCard)
    }

    private fun buildTodaysStatsCard(todaysStatsCardModel: TodaysStatsCardModel?) = builder.build(
            TodaysStatsCardBuilderParams(todaysStatsCardModel, onTodaysStatsCardClick, onTodaysStatsCardFooterLinkClick)
    )

    private val onTodaysStatsCardFooterLinkClick: () -> Unit = { }
    private val onTodaysStatsCardClick: () -> Unit = { }

    private val todaysStatsCard = TodaysStatsCardWithData(
            views = UiStringText(TODAYS_STATS_VIEWS_FORMATTED_STRING),
            visitors = UiStringText(TODAYS_STATS_VISITORS_FORMATTED_STRING),
            likes = UiStringText(TODAYS_STATS_LIKES_FORMATTED_STRING),
            onCardClick = onTodaysStatsCardClick,
            footerLink = FooterLink(
                    label = UiStringRes(R.string.my_site_todays_stats_card_footer_link_go_to_stats),
                    onClick = onTodaysStatsCardFooterLinkClick
            )
    )
}
