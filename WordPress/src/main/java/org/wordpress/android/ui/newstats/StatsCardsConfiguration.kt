package org.wordpress.android.ui.newstats

/**
 * Represents the configuration for stats cards on a per-site basis.
 * This is serialized to JSON for persistence.
 */
data class StatsCardsConfiguration(
    val visibleCards: List<StatsCardType> = StatsCardType.defaultCards(),
    val selectedPeriodType: String? = null,
    val customPeriodStartDate: Long? = null,
    val customPeriodEndDate: Long? = null,
    val selectedChartType: String? = null,
    val selectedMetric: String? = null,
    // The period the user last picked themselves, stored alongside the range they paged to so that
    // navigation resumes against the picked preset after a restart rather than against a concrete
    // range that several presets could describe (CMM-2415). Null for configurations written before
    // this was persisted.
    val originPeriodType: String? = null,
    val originCustomStartDate: Long? = null,
    val originCustomEndDate: Long? = null
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
