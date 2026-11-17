package org.wordpress.android.ui.newstats.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.CardMoveDirection.BOTTOM
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.CardMoveDirection.DOWN
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.CardMoveDirection.TOP
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.CardMoveDirection.UP

/**
 * The reordering menu offers all four moves on every card, including the ones that do nothing at
 * the ends of the list. Those are the taps that must not be reported: a card already at the top
 * reporting a `move_up` would read as a reorder that never happened.
 */
class CardMoveIndicesTest {
    private val cards = listOf(
        StatsCardType.TODAYS_STATS,
        StatsCardType.VIEWS_STATS,
        StatsCardType.MOST_VIEWED_REFERRERS,
        StatsCardType.LOCATIONS
    )

    @Test
    fun `moving a middle card up or down steps one position`() {
        assertThat(cards.cardMoveIndices(StatsCardType.MOST_VIEWED_REFERRERS, UP))
            .isEqualTo(2 to 1)
        assertThat(cards.cardMoveIndices(StatsCardType.MOST_VIEWED_REFERRERS, DOWN))
            .isEqualTo(2 to 3)
    }

    @Test
    fun `moving a middle card to an end lands at that end`() {
        assertThat(cards.cardMoveIndices(StatsCardType.MOST_VIEWED_REFERRERS, TOP))
            .isEqualTo(2 to 0)
        assertThat(cards.cardMoveIndices(StatsCardType.VIEWS_STATS, BOTTOM))
            .isEqualTo(1 to 3)
    }

    @Test
    fun `the first card has nowhere to go up`() {
        assertThat(cards.cardMoveIndices(StatsCardType.TODAYS_STATS, UP)).isNull()
        assertThat(cards.cardMoveIndices(StatsCardType.TODAYS_STATS, TOP)).isNull()
    }

    @Test
    fun `the last card has nowhere to go down`() {
        assertThat(cards.cardMoveIndices(StatsCardType.LOCATIONS, DOWN)).isNull()
        assertThat(cards.cardMoveIndices(StatsCardType.LOCATIONS, BOTTOM)).isNull()
    }

    @Test
    fun `a card that is not visible reports nothing`() {
        assertThat(cards.cardMoveIndices(StatsCardType.AUTHORS, UP)).isNull()
        assertThat(cards.cardMoveIndices(StatsCardType.AUTHORS, BOTTOM)).isNull()
    }

    /** A single card is both first and last, so every direction is inert. */
    @Test
    fun `the only card in the list cannot move in any direction`() {
        val single = listOf(StatsCardType.TODAYS_STATS)

        assertThat(
            listOf(UP, DOWN, TOP, BOTTOM)
                .mapNotNull { single.cardMoveIndices(StatsCardType.TODAYS_STATS, it) }
        ).isEmpty()
    }
}
