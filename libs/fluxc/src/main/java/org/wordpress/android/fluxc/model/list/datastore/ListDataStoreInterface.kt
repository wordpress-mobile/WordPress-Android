package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.list.ListDescriptor

interface ListDataStoreInterface<T> {
    fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long)
    fun fetchList(listDescriptor: ListDescriptor, offset: Int)
    fun getItemByRemoteId(listDescriptor: ListDescriptor, remoteItemId: Long): T?
    fun localItems(listDescriptor: ListDescriptor): List<T> = emptyList()
    fun getItemIdsToHide(listDescriptor: ListDescriptor): List<Pair<Int?, Long?>> = emptyList()
}
