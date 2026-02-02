package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource

/**
 * Represents the configuration for stats cards on a per-site basis.
 * This is serialized to JSON for persistence.
 */
data class StatsCardsConfiguration(
    val visibleCards: List<StatsCardType> = StatsCardType.defaultCards(),
    val selectedPeriodType: String? = null,
    val customPeriodStartDate: Long? = null,
    val customPeriodEndDate: Long? = null,
    // Data sources for each MOST_VIEWED card instance (in order)
    // Default: one for Posts, one for Referrers
    val mostViewedDataSources: List<String> = listOf(
        MostViewedDataSource.POSTS_AND_PAGES.name,
        MostViewedDataSource.REFERRERS.name
    )
) {
    /**
     * Returns card types that are not currently visible (available to add).
     * For MOST_VIEWED, this is handled separately via hiddenMostViewedDataSources().
     */
    fun hiddenCards(): List<StatsCardType> {
        return StatsCardType.entries.filter { cardType ->
            cardType != StatsCardType.MOST_VIEWED && cardType !in visibleCards
        }
    }

    /**
     * Returns Most Viewed data sources that are not currently visible.
     */
    fun hiddenMostViewedDataSources(): List<MostViewedDataSource> {
        val visibleDataSources = mostViewedDataSources.mapNotNull { name ->
            runCatching { MostViewedDataSource.valueOf(name) }.getOrNull()
        }.toSet()
        return MostViewedDataSource.entries.filter { it !in visibleDataSources }
    }

    /**
     * Returns true if the given card type is currently visible.
     */
    fun isCardVisible(cardType: StatsCardType): Boolean {
        return cardType in visibleCards
    }

    /**
     * Returns the number of MOST_VIEWED cards currently visible.
     */
    fun mostViewedCardCount(): Int {
        return visibleCards.count { it == StatsCardType.MOST_VIEWED }
    }
}
