package org.wordpress.android.fluxc.model.list

sealed class PagedListItemType<T> {
    class EndListIndicatorItem<T> : PagedListItemType<T>()
    class LoadingItem<T>(val remoteItemId: Long) : PagedListItemType<T>()
    class ReadyItem<T>(val item: T) : PagedListItemType<T>()
}
