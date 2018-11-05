package org.wordpress.android.ui.posts

import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class PostPositionalDataSource(
    private val postStore: PostStore,
    private val site: SiteModel,
    listStore: ListStore,
    listDescriptor: ListDescriptor,
    private val transform: (Long, PostModel?) -> PostAdapterItemType,
    private val fetchPost: (Long) -> Unit
) : PositionalDataSource<PostAdapterItemType>() {
    private val remoteItemIds = listStore.getList(listDescriptor)
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<PostAdapterItemType?>) {
        val items = getItems(params.startPosition, params.loadSize)
        callback.onResult(items)
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<PostAdapterItemType?>) {
        val startPosition = if (params.requestedStartPosition < remoteItemIds.size) params.requestedStartPosition else 0
        val items = getItems(startPosition, params.requestedLoadSize)
        callback.onResult(items, 0, remoteItemIds.size)
    }

    private fun getItems(startPosition: Int, loadSize: Int): List<PostAdapterItemType?> {
        AppLog.e(T.POSTS, "PostPositionalDataSource: Get items $startPosition - $loadSize")
        return (normalizedIndex(startPosition)..normalizedIndex(startPosition + loadSize - 1)).map { index ->
            AppLog.e(T.POSTS, "Loading index: $index")
            val remotePostId = remoteItemIds[index]
            val post = postStore.getPostByRemotePostId(remotePostId, site)
            if (post == null) {
                fetchPost(remotePostId)
            }
            transform(remotePostId, post)
        }
    }

    private fun normalizedIndex(index: Int): Int {
        return when {
            index <= 0 -> 0
            index >= remoteItemIds.size -> remoteItemIds.size - 1
            else -> index
        }
    }
}
