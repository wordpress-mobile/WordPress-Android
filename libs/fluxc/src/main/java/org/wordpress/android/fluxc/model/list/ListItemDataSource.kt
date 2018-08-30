package org.wordpress.android.fluxc.model.list

interface ListItemDataSource<T> {
    fun fetchItem(remoteItemId: Long)
    fun getItem(remoteItemId: Long): T?
}
