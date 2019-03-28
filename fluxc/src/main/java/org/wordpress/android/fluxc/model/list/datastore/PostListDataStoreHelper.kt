package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

// This is used for a temporary solution for preventing duplicate fetch requests for posts. This workaround should
// be removed when we have a better way to prevent duplicate requests and keep track of fetch statuses.
private data class SitePostId(val localSiteId: Int, val remotePostId: Long)

// TODO: Add unit tests
class PostListDataStoreHelper(private val dispatcher: Dispatcher, private val postStore: PostStore) {
    /*
    // TODO: Use a different solution to keep track of posts already being fetched. Here are some details:
    Unless we use a singleton, this approach of keeping track of items being fetched will not work and
    we never want to have to use a singleton. However, this is a problem worth fixing with a reusable solution,
    so it's left like this for now.
     */
    private val fetchingSet = HashSet<SitePostId>()

    fun getLocalPostIdsForDescriptor(postListDescriptor: PostListDescriptor): List<LocalId> {
        return postStore.getLocalPostIdsForDescriptor(postListDescriptor)
    }

    fun fetchList(postListDescriptor: PostListDescriptor, offset: Long) {
        val fetchPostListPayload = FetchPostListPayload(postListDescriptor, offset)
        dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
    }

    fun fetchPosts(site: SiteModel, remoteItemIds: List<RemoteId>) {
        remoteItemIds.map {
            SitePostId(localSiteId = site.id, remotePostId = it.value)
        }.filter {
            // Only fetch the post if there is no request going on
            !fetchingSet.contains(it)
        }.forEach { sitePostId ->
            fetchingSet.add(sitePostId)

            // TODO: We should implement batch fetching when it's available in the API
            val postToFetch = PostModel()
            postToFetch.remotePostId = sitePostId.remotePostId
            val payload = RemotePostPayload(postToFetch, site)
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
        }
    }

    fun getPosts(site: SiteModel, postIds: List<LocalOrRemoteId>): Map<LocalOrRemoteId, PostModel?> {
        return postStore.getPostsByLocalOrRemotePostIds(postIds, site)
    }
}
