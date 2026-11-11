package org.wordpress.android.ui.rs

/**
 * Loading-state rules for a single tab of a list backed by an rs observable collection.
 *
 * These live outside the view models so they can be tested on their own: the loading state is
 * driven by an rs observable collection, which a unit test can neither create nor fake.
 *
 * Together they enforce one invariant - a tab only claims to be empty once a fetch has resolved.
 * Otherwise the "nothing here" message shows while items are still on their way.
 */
internal object RsTabLoading {
    /**
     * Whether a tab shows its placeholders when a background refresh starts.
     *
     * Only when there is nothing to display and no completed fetch to confirm the tab really is
     * empty. A tab with cached items keeps showing them, and a tab already known to be empty
     * keeps its empty message.
     */
    fun onRefreshStarted(hasItems: Boolean, hasFetched: Boolean): Boolean = !hasItems && !hasFetched

    /**
     * Whether a tab stays loading after items are read from the cache. Loading ends as soon as
     * there is something to show, and an empty read mid-fetch leaves the placeholders in place.
     */
    fun onItemsLoaded(wasLoading: Boolean, hasItems: Boolean): Boolean = wasLoading && !hasItems

    /**
     * Whether a tab stays loading after its list info changes.
     *
     * The fetch is over once the collection is no longer fetching the first page, but list info
     * can resolve ahead of the data it describes - both observers hop to a background thread and
     * reading list info is cheaper than reading every item. So a tab with nothing to show and no
     * completed fetch keeps waiting rather than briefly claiming to be empty.
     */
    fun onListInfoChanged(
        wasLoading: Boolean,
        isFetchingFirstPage: Boolean,
        hasItems: Boolean,
        hasFetched: Boolean
    ): Boolean = wasLoading && (isFetchingFirstPage || (!hasItems && !hasFetched))
}
