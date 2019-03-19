package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

// This is used for a temporary solution for preventing duplicate fetch requests for posts. This workaround should
// be moved in a later minor rework of how we fetch individual posts for the paged list.
private data class SitePostId(val localSiteId: Int, val remotePostId: Long)

class PostListDataStore(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val site: SiteModel?,
    private val performGetItemIdsToHide: ((ListDescriptor) -> List<Pair<Int?, Long?>>)? = null
) : ListDataStoreInterface<PostModel> {
    private val fetchingSet = HashSet<SitePostId>()

    override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
        site?.let {
            val sitePostId = SitePostId(localSiteId = it.id, remotePostId = remoteItemId)
            // Only fetch the post if there is no request going on
            if (!fetchingSet.contains(sitePostId)) {
                fetchingSet.add(sitePostId)

                val postToFetch = PostModel()
                postToFetch.remotePostId = remoteItemId
                val payload = RemotePostPayload(postToFetch, it)
                dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
            }
        }
    }

    override fun fetchList(listDescriptor: ListDescriptor, offset: Long) {
        if (listDescriptor is PostListDescriptor) {
            val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
    }

    override fun localItems(listDescriptor: ListDescriptor): List<PostModel> {
        if (listDescriptor is PostListDescriptor) {
            val localPostIdsToHide = getItemIdsToHide(listDescriptor).mapNotNull { it.first }
            return postStore.getLocalPostsForDescriptor(listDescriptor).filter { !localPostIdsToHide.contains(it.id) }
        }
        return emptyList()
    }

    override fun getItemByRemoteId(listDescriptor: ListDescriptor, remoteItemId: Long): PostModel? {
        if (listDescriptor is PostListDescriptor) {
            val post = postStore.getPostByRemotePostId(remoteItemId, site)
            if (post != null) {
                val sitePostId = SitePostId(localSiteId = listDescriptor.site.id, remotePostId = remoteItemId)
                fetchingSet.remove(sitePostId)
            }
            return post
        }
        return null
    }

    override fun getItemIdsToHide(listDescriptor: ListDescriptor): List<Pair<Int?, Long?>> {
        return performGetItemIdsToHide?.invoke(listDescriptor) ?: emptyList()
    }
}
