package org.wordpress.android.ui.mysite.cards.quicklinksitem

import org.wordpress.android.ui.mysite.items.listitem.ListItemAction

/**
 * The quick links shown by default, in the order they should appear.
 *
 * Quick links are derived from the My Site item list, so without an explicit sort they would
 * inherit that list's order. These defaults lead the quick links card instead, matching iOS.
 */
val DEFAULT_QUICK_LINKS = listOf(
    ListItemAction.STATS,
    ListItemAction.POSTS,
    ListItemAction.PAGES,
    ListItemAction.MEDIA
)

/**
 * Sorts so the default quick links lead in the order declared by [DEFAULT_QUICK_LINKS]. Everything
 * else keeps the order it was built in, since sortedBy is stable.
 */
fun <T> List<T>.sortedByQuickLinkOrder(action: (T) -> ListItemAction): List<T> = sortedBy {
    val index = DEFAULT_QUICK_LINKS.indexOf(action(it))
    if (index >= 0) index else DEFAULT_QUICK_LINKS.size
}
