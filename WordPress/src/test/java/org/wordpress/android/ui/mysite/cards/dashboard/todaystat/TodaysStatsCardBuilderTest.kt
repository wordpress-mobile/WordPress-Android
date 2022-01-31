package org.wordpress.android.ui.mysite.cards.dashboard.todaystat

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.ui.utils.UiString.UiStringText

private const val STAT_VIEWS = 10000
private const val STAT_VISITORS = 1000
private const val STAT_LIKES = 100
private const val STAT_COMMENTS = 1000

private const val STAT_VIEWS_FORMATTED_STRING = "10,000"
private const val STAT_VISITORS_FORMATTED_STRING = "1,000"
private const val STAT_LIKES_FORMATTED_STRING = "100"

@RunWith(MockitoJUnitRunner::class)
class TodaysStatsCardBuilderTest : BaseUnitTest() {
    @Mock private lateinit var statsUtils: StatsUtils

    private lateinit var builder: TodaysStatsCardBuilder
    private val todaysStatsCardModel = TodaysStatsCardModel(
            views = STAT_VIEWS,
            visitors = STAT_VISITORS,
            likes = STAT_LIKES,
            comments = STAT_COMMENTS
    )

    @Before
    fun setUp() {
        builder = TodaysStatsCardBuilder(statsUtils)
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(statsUtils.toFormattedString(STAT_VIEWS)).thenReturn(STAT_VIEWS_FORMATTED_STRING)
        whenever(statsUtils.toFormattedString(STAT_VISITORS)).thenReturn(STAT_VISITORS_FORMATTED_STRING)
        whenever(statsUtils.toFormattedString(STAT_LIKES)).thenReturn(STAT_LIKES_FORMATTED_STRING)
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

        assertThat(statCard).isEqualTo(todayStatCard)
    }

    private fun buildTodaysStatsCard(todaysStatsCardModel: TodaysStatsCardModel?) = builder.build(
            TodaysStatsCardBuilderParams(todaysStatsCardModel)
    )

    private val todayStatCard = TodaysStatsCard(
            UiStringText(STAT_VIEWS_FORMATTED_STRING),
            UiStringText(STAT_VISITORS_FORMATTED_STRING),
            UiStringText(STAT_LIKES_FORMATTED_STRING)
    )
}
