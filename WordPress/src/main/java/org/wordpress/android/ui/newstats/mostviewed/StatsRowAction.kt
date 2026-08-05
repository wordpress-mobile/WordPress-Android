package org.wordpress.android.ui.newstats.mostviewed

/** What tapping a stats list row should do. Exactly one of these — see [statsRowAction]. */
internal sealed interface StatsRowAction {
    /** Reveal or hide the row's children. */
    data object Expand : StatsRowAction

    /** Invoke [onClick], e.g. a post row opening its detail stats. */
    data class Item(val onClick: () -> Unit) : StatsRowAction

    /** Open [url], e.g. a referrer, click or tag opening its page in a Custom Tab. */
    data class OpenUrl(val url: String) : StatsRowAction

    /** The row is inert. */
    data object None : StatsRowAction
}

/**
 * Decides what a stats row does when tapped. A row never both expands and navigates, matching the
 * old stats screen.
 *
 * The precedence is why this is a testable function rather than inline in a composable:
 *
 * 1. A row with children **expands** — a group header never navigates.
 * 2. [onItemClick] wins over [url], so a post row opens its detail stats rather than opening the
 *    post in a browser. Posts and referrers both carry a `url`, so without this a post row would
 *    navigate to the wrong place and show a misleading "open link" affordance.
 * 3. Otherwise a non-blank [url] opens.
 *
 * Each result carries what it needs to act, so callers can't reach a state where the row is
 * clickable but has nothing to invoke.
 */
internal fun statsRowAction(
    hasChildren: Boolean,
    url: String?,
    onItemClick: (() -> Unit)?
): StatsRowAction = when {
    hasChildren -> StatsRowAction.Expand
    onItemClick != null -> StatsRowAction.Item(onItemClick)
    !url.isNullOrBlank() -> StatsRowAction.OpenUrl(url)
    else -> StatsRowAction.None
}
