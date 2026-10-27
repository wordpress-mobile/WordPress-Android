package org.wordpress.android.ui.rs

/**
 * The rows on screen, per tab. Keyed by tab because the pager composes the neighbouring tab during
 * a drag, and a single set would be overwritten by it.
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
