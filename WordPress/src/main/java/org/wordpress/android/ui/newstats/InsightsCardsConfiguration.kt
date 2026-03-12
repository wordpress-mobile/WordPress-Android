package org.wordpress.android.ui.newstats

data class InsightsCardsConfiguration(
    val visibleCards: List<InsightsCardType> =
        InsightsCardType.defaultCards(),
    val hiddenCards: List<InsightsCardType> = emptyList()
) {
    fun computeHiddenCards(): List<InsightsCardType> {
        return InsightsCardType.entries
            .filter { it !in visibleCards }
    }

    fun isCardVisible(cardType: InsightsCardType): Boolean {
        return cardType in visibleCards
    }
}
