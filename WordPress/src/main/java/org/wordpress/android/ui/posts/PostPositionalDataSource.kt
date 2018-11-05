package org.wordpress.android.ui.posts

import android.arch.paging.PositionalDataSource
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore

class PostPositionalDataSource(
    private val postStore: PostStore,
    private val site: SiteModel,
    listStore: ListStore,
    listDescriptor: PostListDescriptor,
    private val transform: (Long?, PostModel?) -> PostAdapterItemType,
    private val fetchPost: (Long) -> Unit
) : PositionalDataSource<PostAdapterItemType>() {
    private val localItems = postStore.getLocalPostsForDescriptor(listDescriptor)
    private val remoteItemIds = listStore.getList(listDescriptor)
    private val totalSize = localItems.size + remoteItemIds.size

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<PostAdapterItemType>) {
        val startPosition = if (params.requestedStartPosition < remoteItemIds.size) params.requestedStartPosition else 0
        val items = getItems(startPosition, params.requestedLoadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, remoteItemIds.size + localItems.size)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<PostAdapterItemType>) {
        CoroutineScope(Dispatchers.Default).launch {
            val items = getItems(params.startPosition, params.loadSize)
            callback.onResult(items)
        }
    }

    private fun getItems(startPosition: Int, loadSize: Int): List<PostAdapterItemType> {
        val normalizedStart = normalizedIndex(startPosition)
        val normalizedEnd = normalizedIndex(normalizedStart + loadSize)
        if (normalizedStart == normalizedEnd) {
            return emptyList()
        }
        return (normalizedStart..(normalizedEnd - 1)).map { index ->
            if (index < localItems.size) {
                return@map transform(null, localItems[index])
            }
            val remoteIndex = index - localItems.size
            val remotePostId = remoteItemIds[remoteIndex]
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
            index >= totalSize -> totalSize
            else -> index
        }
    }
}
