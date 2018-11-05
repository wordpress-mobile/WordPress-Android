package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

interface PagedListDataStoreInterface<T> {
    fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long)
    fun fetchList(listDescriptor: ListDescriptor, offset: Int)
    fun getItemByRemoteId(listDescriptor: ListDescriptor, remoteItemId: Long): T?
    fun localItems(listDescriptor: ListDescriptor): List<T> = emptyList()
}

class PagedListDataForPostStore(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val site: SiteModel?
) : PagedListDataStoreInterface<PostModel> {
    override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
        site?.let {
            val postToFetch = PostModel()
            postToFetch.remotePostId = remoteItemId
            val payload = RemotePostPayload(postToFetch, it)
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
        }
    }

    override fun fetchList(listDescriptor: ListDescriptor, offset: Int) {
        if (listDescriptor is PostListDescriptor) {
            val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
    }

    override fun localItems(listDescriptor: ListDescriptor): List<PostModel> {
        if (listDescriptor is PostListDescriptor) {
            return postStore.getLocalPostsForDescriptor(listDescriptor)
        }
        return emptyList()
    }

    override fun getItemByRemoteId(listDescriptor: ListDescriptor, remoteItemId: Long): PostModel? {
        if (listDescriptor is PostListDescriptor) {
            return postStore.getPostByRemotePostId(remoteItemId, site)
        }
        return null
    }
}
