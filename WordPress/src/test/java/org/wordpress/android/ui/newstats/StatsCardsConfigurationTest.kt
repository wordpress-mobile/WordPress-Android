package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsCardsConfigurationTest {
    @Test
    fun `when default configuration, then all default cards are visible`() {
        val config = StatsCardsConfiguration()

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
    }

    @Test
    fun `when hiddenCards is called, then non-visible cards are returned`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.VIEWS_STATS)
        )

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.COUNTRIES,
            StatsCardType.AUTHORS
        )
    }

    @Test
    fun `when all cards visible, then hiddenCards returns empty list`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(
                StatsCardType.TODAYS_STATS,
                StatsCardType.VIEWS_STATS,
                StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
                StatsCardType.MOST_VIEWED_REFERRERS,
                StatsCardType.COUNTRIES,
                StatsCardType.AUTHORS
            )
        )

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).isEmpty()
    }

    @Test
    fun `when no cards visible, then hiddenCards returns all cards`() {
        val config = StatsCardsConfiguration(visibleCards = emptyList())

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            StatsCardType.TODAYS_STATS,
            StatsCardType.VIEWS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.COUNTRIES,
            StatsCardType.AUTHORS
        )
    }

    @Test
    fun `when isCardVisible is called for visible card, then returns true`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.VIEWS_STATS)
        )

        assertThat(config.isCardVisible(StatsCardType.TODAYS_STATS)).isTrue()
        assertThat(config.isCardVisible(StatsCardType.VIEWS_STATS)).isTrue()
    }

    @Test
    fun `when isCardVisible is called for hidden card, then returns false`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS)
        )

        assertThat(config.isCardVisible(StatsCardType.VIEWS_STATS)).isFalse()
        assertThat(config.isCardVisible(StatsCardType.COUNTRIES)).isFalse()
    }

    @Test
    fun `when custom period dates are set, then they are stored correctly`() {
        val config = StatsCardsConfiguration(
            selectedPeriodType = "custom",
            customPeriodStartDate = 19000L,
            customPeriodEndDate = 19007L
        )

        assertThat(config.selectedPeriodType).isEqualTo("custom")
        assertThat(config.customPeriodStartDate).isEqualTo(19000L)
        assertThat(config.customPeriodEndDate).isEqualTo(19007L)
    }
}
