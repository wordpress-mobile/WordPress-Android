package org.wordpress.android.ui.newstats.subscribers

/**
 * Represents the configuration for subscriber cards
 * on a per-site basis.
 * This is serialized to JSON for persistence.
 */
data class SubscribersCardsConfiguration(
    val visibleCards: List<SubscribersCardType> =
        SubscribersCardType.defaultCards()
) {
    /**
     * Returns card types that are not currently visible
     * (available to add).
     */
    fun hiddenCards(): List<SubscribersCardType> {
        return SubscribersCardType.entries
            .filter { it !in visibleCards }
    }
}
