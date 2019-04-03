package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.EndListIndicatorIdentifier
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListItemType.EndListIndicatorItem
import org.wordpress.android.viewmodel.posts.PostListItemType.LoadingItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

sealed class PostListItemIdentifier {
    data class LocalPostId(val id: LocalId) : PostListItemIdentifier()
    data class RemotePostId(val id: RemoteId) : PostListItemIdentifier()
    object EndListIndicatorIdentifier : PostListItemIdentifier()
}

class PostListDataStore(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val performGetItemIdsToHide: ((PostListDescriptor) -> Pair<LocalPostId, RemotePostId>?),
    private val transform: ((PostModel) -> PostListItemUiState)
) : ListItemDataStoreInterface<PostListDescriptor, PostListItemIdentifier, PostListItemType> {
    override fun fetchList(listDescriptor: PostListDescriptor, offset: Long) {
        val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
        dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
    }

    override fun getItemIdentifiers(
        listDescriptor: PostListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<PostListItemIdentifier> {
        val localItems = postStore.getLocalPostIdsForDescriptor(listDescriptor)
                .map { LocalPostId(id = it) }
        val remoteItems = remoteItemIds.map { RemotePostId(id = it) }
        val actualItems = localItems + remoteItems

        val endListItem = if (isListFullyFetched && actualItems.isNotEmpty()) {
            listOf(EndListIndicatorIdentifier)
        } else emptyList()
        val itemsToHide = getItemIdsToHide(listDescriptor)
        // We only want to show the end list indicator if the list is fully fetched and it's not empty
        return (actualItems + endListItem).filter { !itemsToHide.contains(it) }
    }

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: PostListDescriptor,
        itemIdentifiers: List<PostListItemIdentifier>
    ): List<PostListItemType> {
        val localOrRemoteIds = itemIdentifiers.mapNotNull { identifier ->
            when (identifier) {
                is LocalPostId -> identifier.id
                is RemotePostId -> identifier.id
                // We are creating a list of local and remote ids, so other type of identifiers don't matter
                EndListIndicatorIdentifier -> null
            }
        }

        val postList = postStore.getPostsByLocalOrRemotePostIds(localOrRemoteIds, listDescriptor.site)

        // Convert the post list into 2 maps with local and remote ids as keys
        val localPostMap = postList.associateBy { LocalId(it.id) }
        val remotePostMap = postList.filter { it.remotePostId != 0L }.associateBy { RemoteId(it.remotePostId) }

        // Fetch the missing posts
        val remoteIdsToFetch: List<RemoteId> = localOrRemoteIds.mapNotNull { it as? RemoteId }
                .filter { !remotePostMap.containsKey(it) }
        fetchPosts(listDescriptor.site, remoteIdsToFetch)

        val transformFromNullablePost = { localOrRemoteId: LocalOrRemoteId, post: PostModel? ->
            if (post == null) {
                LoadingItem(localOrRemoteId)
            } else {
                transform(post)
            }
        }
        return itemIdentifiers.map { identifier ->
            when (identifier) {
                is LocalPostId -> transformFromNullablePost(identifier.id, localPostMap[identifier.id])
                is RemotePostId -> transformFromNullablePost(identifier.id, remotePostMap[identifier.id])
                EndListIndicatorIdentifier -> EndListIndicatorItem
            }
        }
    }

    private fun getItemIdsToHide(postListDescriptor: PostListDescriptor): Set<PostListItemIdentifier> {
        return performGetItemIdsToHide.invoke(postListDescriptor)?.let { setOf(it.first, it.second) } ?: emptySet()
    }

    // TODO: We should implement batch fetching when it's available in the API
    // TODO IMPORTANT: We need to prevent duplicate requests
    private fun fetchPosts(site: SiteModel, remoteItemIds: List<RemoteId>) {
        remoteItemIds.map {
            val postToFetch = PostModel()
            postToFetch.remotePostId = it.value
            RemotePostPayload(postToFetch, site)
        }.forEach {
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(it))
        }
    }
}
