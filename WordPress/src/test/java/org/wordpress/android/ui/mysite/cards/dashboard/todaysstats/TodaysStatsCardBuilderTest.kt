package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder.Companion.URL_GET_MORE_VIEWS_AND_TRAFFIC
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

private const val TODAYS_STATS_VIEWS = 10000
private const val TODAYS_STATS_VISITORS = 1000
private const val TODAYS_STATS_LIKES = 100
private const val TODAYS_STATS_COMMENTS = 1000

private const val TODAYS_STATS_VIEWS_FORMATTED_STRING = "10,000"
private const val TODAYS_STATS_VISITORS_FORMATTED_STRING = "1,000"
private const val TODAYS_STATS_LIKES_FORMATTED_STRING = "100"

private const val GET_MORE_VIEWS_MSG_WITH_CLICKABLE_LINK =
        "If you want to try get more views and traffic check out our " +
                "<a href=\"${URL_GET_MORE_VIEWS_AND_TRAFFIC}\">top tips</a>."

@RunWith(MockitoJUnitRunner::class)
class TodaysStatsCardBuilderTest : BaseUnitTest() {
    @Mock private lateinit var statsUtils: StatsUtils
    @Mock private lateinit var appLogWrapper: AppLogWrapper
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils

    private lateinit var builder: TodaysStatsCardBuilder
    private val todaysStatsCardModel = TodaysStatsCardModel(
            views = TODAYS_STATS_VIEWS,
            visitors = TODAYS_STATS_VISITORS,
            likes = TODAYS_STATS_LIKES,
            comments = TODAYS_STATS_COMMENTS
    )

    @Before
    fun setUp() {
        builder = TodaysStatsCardBuilder(statsUtils, appLogWrapper, htmlMessageUtils)
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

    /* TODAY'S STATS CARD - CONTENT */

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

    @Test
    fun `given empty todays stats, when card is built, then get more views message exists`() {
        val zeroCount = 0
        whenever(statsUtils.toFormattedString(zeroCount)).thenReturn("$zeroCount")

        val todaysStatsCard = buildTodaysStatsCard(
                TodaysStatsCardModel(
                        views = zeroCount,
                        visitors = zeroCount,
                        likes = zeroCount
                )
        )

        assertThat((todaysStatsCard as TodaysStatsCardWithData).message?.text)
                .isEqualTo(UiStringText(GET_MORE_VIEWS_MSG_WITH_CLICKABLE_LINK))
    }

    @Test
    fun `given non empty todays stats, when card is built, then get more views message not exists`() {
        val todaysStatsCard = buildTodaysStatsCard(todaysStatsCardModel)

        assertThat((todaysStatsCard as TodaysStatsCardWithData).message).isNull()
    }

    private fun buildTodaysStatsCard(todaysStatsCardModel: TodaysStatsCardModel?): TodaysStatsCard? {
        whenever(
                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                        R.string.my_site_todays_stats_get_more_views_message,
                        URL_GET_MORE_VIEWS_AND_TRAFFIC
                )
        ).thenReturn(GET_MORE_VIEWS_MSG_WITH_CLICKABLE_LINK)
        return builder.build(
                TodaysStatsCardBuilderParams(
                        todaysStatsCardModel,
                        onTodaysStatsCardClick,
                        onGetMoreViewsClick,
                        onTodaysStatsCardFooterLinkClick
                )
        )
    }

    private val onGetMoreViewsClick: () -> Unit = { }
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
