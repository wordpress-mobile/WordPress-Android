package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource

class StatsCardsConfigurationTest {

    @Test
    fun `when default configuration, then all default cards are visible`() {
        val config = StatsCardsConfiguration()

        assertThat(config.visibleCards).isEqualTo(StatsCardType.defaultCards())
    }

    @Test
    fun `when default configuration, then default most viewed data sources are set`() {
        val config = StatsCardsConfiguration()

        assertThat(config.mostViewedDataSources).containsExactly(
            MostViewedDataSource.POSTS_AND_PAGES.name,
            MostViewedDataSource.REFERRERS.name
        )
    }

    @Test
    fun `when hiddenCards is called, then non-visible cards are returned excluding MOST_VIEWED`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.MOST_VIEWED)
        )

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            StatsCardType.VIEWS_STATS,
            StatsCardType.COUNTRIES
        )
        assertThat(hiddenCards).doesNotContain(StatsCardType.MOST_VIEWED)
    }

    @Test
    fun `when all cards visible, then hiddenCards returns empty list`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(
                StatsCardType.TODAYS_STATS,
                StatsCardType.VIEWS_STATS,
                StatsCardType.MOST_VIEWED,
                StatsCardType.COUNTRIES
            )
        )

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).isEmpty()
    }

    @Test
    fun `when no cards visible, then hiddenCards returns all except MOST_VIEWED`() {
        val config = StatsCardsConfiguration(visibleCards = emptyList())

        val hiddenCards = config.hiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            StatsCardType.TODAYS_STATS,
            StatsCardType.VIEWS_STATS,
            StatsCardType.COUNTRIES
        )
    }

    @Test
    fun `when hiddenMostViewedDataSources is called, then non-visible data sources are returned`() {
        val config = StatsCardsConfiguration(
            mostViewedDataSources = listOf(MostViewedDataSource.POSTS_AND_PAGES.name)
        )

        val hiddenDataSources = config.hiddenMostViewedDataSources()

        assertThat(hiddenDataSources).containsExactly(MostViewedDataSource.REFERRERS)
    }

    @Test
    fun `when all data sources visible, then hiddenMostViewedDataSources returns empty`() {
        val config = StatsCardsConfiguration(
            mostViewedDataSources = listOf(
                MostViewedDataSource.POSTS_AND_PAGES.name,
                MostViewedDataSource.REFERRERS.name
            )
        )

        val hiddenDataSources = config.hiddenMostViewedDataSources()

        assertThat(hiddenDataSources).isEmpty()
    }

    @Test
    fun `when no data sources visible, then hiddenMostViewedDataSources returns all`() {
        val config = StatsCardsConfiguration(mostViewedDataSources = emptyList())

        val hiddenDataSources = config.hiddenMostViewedDataSources()

        assertThat(hiddenDataSources).containsExactlyInAnyOrder(
            MostViewedDataSource.POSTS_AND_PAGES,
            MostViewedDataSource.REFERRERS
        )
    }

    @Test
    fun `when invalid data source name in list, then it is ignored`() {
        val config = StatsCardsConfiguration(
            mostViewedDataSources = listOf(
                MostViewedDataSource.POSTS_AND_PAGES.name,
                "INVALID_DATA_SOURCE"
            )
        )

        val hiddenDataSources = config.hiddenMostViewedDataSources()

        assertThat(hiddenDataSources).containsExactly(MostViewedDataSource.REFERRERS)
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
    fun `when mostViewedCardCount is called, then returns count of MOST_VIEWED cards`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(
                StatsCardType.TODAYS_STATS,
                StatsCardType.MOST_VIEWED,
                StatsCardType.MOST_VIEWED,
                StatsCardType.COUNTRIES
            )
        )

        assertThat(config.mostViewedCardCount()).isEqualTo(2)
    }

    @Test
    fun `when no MOST_VIEWED cards, then mostViewedCardCount returns zero`() {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.COUNTRIES)
        )

        assertThat(config.mostViewedCardCount()).isEqualTo(0)
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
