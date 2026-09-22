package org.wordpress.android.ui.rs

/**
 * The rows on screen right now, per tab, so work queued for rows scrolled past can be dropped.
 *
 * Keyed by tab rather than held as one set: the pager composes the neighbouring tab during a drag,
 * and its visible-row stream reports against that tab. A single set would be overwritten by the
 * neighbour, stranding the active tab's queued fetches - they re-check this set once a permit frees
 * and would find the wrong ids - and would later hand a retry another tab's ids after a refresh.
 *
 * Lives outside the view models so it can be tested on its own, like [RsTabLoading].
 */
internal class RsVisibleRows<TAB> {
    private val rows = mutableMapOf<TAB, Set<Long>>()

    fun record(tab: TAB, ids: List<Long>) {
        rows[tab] = ids.toSet()
    }

    fun visible(tab: TAB): Set<Long> = rows[tab].orEmpty()

    fun clear() {
        rows.clear()
    }
}
