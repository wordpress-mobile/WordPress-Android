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
    val originCustomEndDate: Long? = null,
    // Which revision of the default card set this configuration has already been brought up to
    // date with, so that a card promoted to the defaults can be given once to sites configured
    // before the promotion. Configurations written before this was persisted read as [UNVERSIONED].
    val defaultCardsVersion: Int = UNVERSIONED
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

    companion object {
        /** A configuration persisted before [defaultCardsVersion] existed. */
        const val UNVERSIONED = 0

        /** The revision that promoted Referrers to the defaults (CMM-2583). */
        const val VERSION_REFERRERS = 1

        /**
         * Revision of [StatsCardType.defaultCards] this build ships. Bump it when a card is added
         * to the defaults and it should also reach sites that are already configured - and gate
         * the step that adds it on its own revision, so that bumping this again later cannot
         * re-run a step for configurations that have already been through it.
         *
         * 1: Referrers became a default card (CMM-2583).
         */
        const val CURRENT_DEFAULT_CARDS_VERSION = VERSION_REFERRERS
    }
}
