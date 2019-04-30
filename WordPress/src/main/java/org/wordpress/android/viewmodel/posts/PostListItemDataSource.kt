package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
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

class PostListItemDataSource(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val postFetcher: PostFetcher,
    private val transform: (PostModel) -> PostListItemUiState
) : ListItemDataSourceInterface<PostListDescriptor, PostListItemIdentifier, PostListItemType> {
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

        // We only want to show the end list indicator if the list is fully fetched and it's not empty
        return if (isListFullyFetched && actualItems.isNotEmpty()) {
            (actualItems + listOf(EndListIndicatorIdentifier))
        } else actualItems
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

        fetchMissingRemotePosts(listDescriptor.site, localOrRemoteIds, remotePostMap)

        return itemIdentifiers.map { identifier ->
            when (identifier) {
                is LocalPostId -> transformToPostListItemType(identifier.id, localPostMap[identifier.id])
                is RemotePostId -> transformToPostListItemType(identifier.id, remotePostMap[identifier.id])
                EndListIndicatorIdentifier -> EndListIndicatorItem
            }
        }
    }

    private fun fetchMissingRemotePosts(
        site: SiteModel,
        localOrRemoteIds: List<LocalOrRemoteId>,
        remotePostMap: Map<RemoteId, PostModel>
    ) {
        val remoteIdsToFetch: List<RemoteId> = localOrRemoteIds.mapNotNull { it as? RemoteId }
                .filter { !remotePostMap.containsKey(it) }
        postFetcher.fetchPosts(site, remoteIdsToFetch)
    }

    private fun transformToPostListItemType(localOrRemoteId: LocalOrRemoteId, post: PostModel?): PostListItemType =
            if (post == null) {
                // If the post is not in cache, that means we'll be loading it
                LoadingItem(localOrRemoteId)
            } else {
                transform(post)
            }
}
