package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsCardsConfigurationTest {
    @Test
    fun `when default configuration, then only the five default cards are visible, in order`() {
        // Spelled out rather than compared against `defaultCards()`, which is what builds the
        // default configuration: adding a card to the defaults, dropping one, or reordering them
        // has to be a deliberate edit here. Archives in particular ships hidden.
        val config = StatsCardsConfiguration()

        assertThat(config.visibleCards).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.VIEWS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.LOCATIONS
        )
    }

    @Test
    fun `when default configuration, then referrers is visible under posts and pages`() {
        val config = StatsCardsConfiguration()

        assertThat(config.visibleCards).containsSequence(
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            StatsCardType.MOST_VIEWED_REFERRERS
        )
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
            StatsCardType.LOCATIONS,
            StatsCardType.AUTHORS,
            StatsCardType.CLICKS,
            StatsCardType.SEARCH_TERMS,
            StatsCardType.VIDEO_PLAYS,
            StatsCardType.FILE_DOWNLOADS,
            StatsCardType.DEVICES,
            StatsCardType.UTM,
            StatsCardType.ARCHIVES
        )
    }

    @Test
    fun `when all cards visible, then hiddenCards returns empty list`() {
        val config = StatsCardsConfiguration(
            visibleCards = StatsCardType.entries.toList()
        )

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).isEmpty()
    }

    @Test
    fun `when no cards visible, then hiddenCards returns all cards`() {
        val config = StatsCardsConfiguration(visibleCards = emptyList())

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            *StatsCardType.entries.toTypedArray()
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
        assertThat(config.isCardVisible(StatsCardType.LOCATIONS)).isFalse()
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
