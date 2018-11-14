package org.wordpress.android.fluxc.model.list

/**
 * These are the different type of items that will be returned by the `PagedList` from `ListStore.getList`.
 */
sealed class PagedListItemType<T> {
    /**
     * Indicates the end of the list is reached.
     */
    class EndListIndicatorItem<T> : PagedListItemType<T>()

    /**
     * Indicates the item doesn't exist in the DB and will be loaded.
     */
    class LoadingItem<T>(val remoteItemId: Long) : PagedListItemType<T>()

    /**
     * Indicates the item is available in the DB and ready to be used.
     */
    class ReadyItem<T>(val item: T) : PagedListItemType<T>()
}
