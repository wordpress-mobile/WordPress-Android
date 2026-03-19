package org.wordpress.android.ui.newstats

data class InsightsCardsConfiguration(
    val visibleCards: List<InsightsCardType> =
        InsightsCardType.defaultCards()
) {
    val hiddenCards: List<InsightsCardType>
        get() = InsightsCardType.entries
            .filter { it !in visibleCards }
}
