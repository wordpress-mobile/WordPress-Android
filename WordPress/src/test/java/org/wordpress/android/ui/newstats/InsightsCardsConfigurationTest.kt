package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InsightsCardsConfigurationTest {
    @Test
    fun `when default configuration, then all default cards are visible`() {
        val config = InsightsCardsConfiguration()

        assertThat(config.visibleCards)
            .isEqualTo(InsightsCardType.defaultCards())
    }

    @Test
    fun `when hiddenCards is called, then non-visible cards are returned`() {
        val config = InsightsCardsConfiguration(
            visibleCards = emptyList()
        )

        val hiddenCards = config.computeHiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            InsightsCardType.ALL_TIME_STATS,
            InsightsCardType.MOST_POPULAR_DAY,
            InsightsCardType.MOST_POPULAR_TIME,
            InsightsCardType.YEAR_IN_REVIEW,
            InsightsCardType.TAGS_AND_CATEGORIES
        )
    }

    @Test
    fun `when all cards visible, then hiddenCards returns empty list`() {
        val config = InsightsCardsConfiguration(
            visibleCards = InsightsCardType.entries.toList()
        )

        val hiddenCards = config.computeHiddenCards()

        assertThat(hiddenCards).isEmpty()
    }

    @Test
    fun `when no cards visible, then hiddenCards returns all cards`() {
        val config = InsightsCardsConfiguration(
            visibleCards = emptyList()
        )

        val hiddenCards = config.computeHiddenCards()

        assertThat(hiddenCards).containsExactlyInAnyOrder(
            *InsightsCardType.entries.toTypedArray()
        )
    }

    @Test
    fun `when isCardVisible is called for visible card, then returns true`() {
        val config = InsightsCardsConfiguration(
            visibleCards = listOf(InsightsCardType.YEAR_IN_REVIEW)
        )

        assertThat(
            config.isCardVisible(InsightsCardType.YEAR_IN_REVIEW)
        ).isTrue()
    }

    @Test
    fun `when isCardVisible is called for hidden card, then returns false`() {
        val config = InsightsCardsConfiguration(
            visibleCards = emptyList()
        )

        assertThat(
            config.isCardVisible(InsightsCardType.YEAR_IN_REVIEW)
        ).isFalse()
    }
}
