package org.wordpress.android.ui.media.library

/** What the media library screen asks of its view model */
@Suppress("TooManyFunctions")
interface MediaLibraryActions {
    fun onFilterSelected(filter: MediaFilter)
    fun onRefresh()
    fun onLoadMore()
    fun onSearchOpen()
    fun onSearchQueryChanged(query: String)
    fun onSearchClose()
    fun onItemClick(localId: Int)
    fun onItemLongClick(localId: Int)
    fun onItemRetryClick(localId: Int)
    fun onItemTrashClick(localId: Int)
    fun onClearSelection()
    fun onDeleteSelectedClick()
    fun onConfirmDelete()
    fun onDismissDelete()
    suspend fun canLoadVideoFrame(url: String): Boolean
}
