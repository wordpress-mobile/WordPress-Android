package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload

class PostListDataSource(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val site: SiteModel?,
    private val trashedPostIds: List<Pair<Int, Long>>?,
    private val uploadedPostRemoteIds: List<Long>?
) : ListItemDataSource<PostModel> {
    /**
     * Tells [ListStore] how to fetch a post from remote for the given list descriptor and remote post id
     */
    override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
        site?.let {
            val postToFetch = PostModel()
            postToFetch.remotePostId = remoteItemId
            val payload = RemotePostPayload(postToFetch, it)
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
        }
    }

    /**
     * Tells [ListStore] how to fetch a list from remote for the given list descriptor and offset
     */
    override fun fetchList(listDescriptor: ListDescriptor, offset: Int) {
        if (listDescriptor is PostListDescriptor) {
            val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
    }

    /**
     * Tells [ListStore] how to get posts from [PostStore] for the given list descriptor and remote post ids
     */
    override fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, PostModel> {
        site?.let {
            return postStore.getPostsByRemotePostIds(remoteItemIds, it)
        }
        return emptyMap()
    }

    /**
     * Tells [ListStore] which local drafts should be included in the list. Since [ListStore] deals with
     * remote items, it needs our help to show local data.
     */
    override fun localItems(listDescriptor: ListDescriptor): List<PostModel>? {
        if (listDescriptor is PostListDescriptor) {
            trashedPostIds?.let { trashedIds ->
                // We should filter out the trashed posts from local drafts since they should be hidden
                val trashedLocalPostIds = trashedIds.map { it.first }
                return postStore.getLocalPostsForDescriptor(listDescriptor)
                        .filter { !trashedLocalPostIds.contains(it.id) }
            }
        }
        return null
    }

    /**
     * Tells [ListStore] which remote post ids must be included in the list. This is to workaround a case
     * where the local draft is uploaded to remote but the list has not been refreshed yet. If we don't
     * tell about this to [ListStore] that post will disappear until the next refresh.
     *
     * Please check out [OnPostUploaded] and [OnListChanged] for where [uploadedPostRemoteIds] is managed.
     */
    override fun remoteItemIdsToInclude(listDescriptor: ListDescriptor): List<Long>? {
        return uploadedPostRemoteIds
    }

    /**
     * Tells [ListStore] which remote post ids must be hidden from the list. In order to show an undo
     * snackbar when a post is trashed, we don't immediately delete/trash a post which means [ListStore]
     * doesn't know about this action and needs our help to determine which posts should be hidden until
     * delete/trash action is completed.
     *
     * Please check out [trashPost] for more details.
     */
    override fun remoteItemsToHide(listDescriptor: ListDescriptor): List<Long>? {
        return trashedPostIds?.map { it.second }
    }
}
