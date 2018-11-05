package org.wordpress.android.ui.posts

import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore

class PostPositionalDataSource(
    private val postStore: PostStore,
    private val site: SiteModel,
    listStore: ListStore,
    listDescriptor: ListDescriptor,
    private val transform: (Long, PostModel?) -> PostAdapterItemType
) : PositionalDataSource<PostAdapterItemType>() {
    private val remoteItemIds = listStore.getList(listDescriptor)
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<PostAdapterItemType>) {
        val items = getItems(params.startPosition, params.loadSize)
        callback.onResult(items)
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<PostAdapterItemType>) {
        val startPosition = if (params.requestedStartPosition < remoteItemIds.size) params.requestedStartPosition else 0
        val items = getItems(startPosition, params.requestedLoadSize)
        callback.onResult(items, startPosition, remoteItemIds.size)
    }

    private fun getItems(startPosition: Int, loadSize: Int): List<PostAdapterItemType> {
        return (startPosition..(startPosition + loadSize - 1)).map { index ->
            val remotePostId = remoteItemIds[index]
            val post = postStore.getPostByRemotePostId(remotePostId, site)
            transform(remotePostId, post)
        }
    }
}
