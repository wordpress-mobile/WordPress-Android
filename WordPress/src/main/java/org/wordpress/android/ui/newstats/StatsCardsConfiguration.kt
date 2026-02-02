package org.wordpress.android.ui.newstats

/**
 * Represents the configuration for stats cards on a per-site basis.
 * This is serialized to JSON for persistence.
 */
data class StatsCardsConfiguration(
    val visibleCards: List<StatsCardType> = StatsCardType.defaultCards(),
    val selectedPeriodType: String? = null,
    val customPeriodStartDate: Long? = null,
    val customPeriodEndDate: Long? = null
) {
    /**
     * Returns card types that are not currently visible (available to add).
     */
    fun hiddenCards(): List<StatsCardType> {
        return StatsCardType.entries.filter { it !in visibleCards }
    }

    /**
     * Returns true if the given card type is currently visible.
     */
    fun isCardVisible(cardType: StatsCardType): Boolean {
        return cardType in visibleCards
    }
}
