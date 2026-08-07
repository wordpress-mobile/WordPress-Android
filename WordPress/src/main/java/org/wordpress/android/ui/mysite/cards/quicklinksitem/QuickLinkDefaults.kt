package org.wordpress.android.ui.mysite.cards.quicklinksitem

import org.wordpress.android.ui.mysite.items.listitem.ListItemAction

/**
 * Single source of truth for which quick links are shown by default and in which order.
 *
 * Quick links are derived from the My Site item list, so without an explicit sort they would
 * inherit that list's order. These defaults lead the quick links card instead, matching iOS.
 */
object QuickLinkDefaults {
    val DEFAULT_SHORTCUTS = listOf(
        ListItemAction.STATS,
        ListItemAction.POSTS,
        ListItemAction.PAGES,
        ListItemAction.MEDIA
    )

    /**
     * Sorts so the default shortcuts lead in the order declared above. Everything else keeps the
     * order it was built in, since [sortedBy] is stable.
     */
    fun <T> List<T>.sortedByQuickLinkOrder(action: (T) -> ListItemAction): List<T> = sortedBy {
        val index = DEFAULT_SHORTCUTS.indexOf(action(it))
        if (index >= 0) index else DEFAULT_SHORTCUTS.size
    }
}
