package org.wordpress.android.fluxc.model.list.datastore

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datastore.PostListItemIdentifier.EndListIndicator
import org.wordpress.android.fluxc.model.list.datastore.PostListItemIdentifier.LocalPostId
import org.wordpress.android.fluxc.model.list.datastore.PostListItemIdentifier.RemotePostId

// TODO: Move this file to WPAndroid

sealed class PostListItemIdentifier {
    data class LocalPostId(val id: LocalId) : PostListItemIdentifier()
    data class RemotePostId(val id: RemoteId) : PostListItemIdentifier()
    object EndListIndicator : PostListItemIdentifier()
}

class PostListDataStore(
    private val postListDataStoreHelper: PostListDataStoreHelper,
    private val performGetItemIdsToHide: ((ListDescriptor) -> Set<PostListItemIdentifier>)? = null
) : ListItemDataStoreInterface<PostListDescriptor, PostListItemIdentifier, PostModel> {
    override fun fetchList(listDescriptor: PostListDescriptor, offset: Long) {
        postListDataStoreHelper.fetchList(postListDescriptor = listDescriptor, offset = offset)
    }

    override fun getItemIdentifiers(
        listDescriptor: PostListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<PostListItemIdentifier> {
        val localItems = postListDataStoreHelper.getLocalPostIdsForDescriptor(listDescriptor)
                .map { LocalPostId(id = it) }
        val remoteItems = remoteItemIds.map { RemotePostId(id = it) }
        val actualItems = localItems + remoteItems

        val endListItem = if (isListFullyFetched && actualItems.isNotEmpty()) listOf(EndListIndicator) else emptyList()
        val itemsToHide = getItemIdsToHide(listDescriptor)
        // We only want to show the end list indicator if the list is fully fetched and it's not empty
        return (actualItems + endListItem).filter { !itemsToHide.contains(it) }
    }

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: PostListDescriptor,
        itemIdentifiers: List<PostListItemIdentifier>
    ): List<PostModel> {
        val localOrRemoteIds = itemIdentifiers.mapNotNull { identifier ->
            when (identifier) {
                is LocalPostId -> identifier.id
                is RemotePostId -> identifier.id
                else -> null
            }
        }
        val postMap = postListDataStoreHelper.getPosts(listDescriptor.site, localOrRemoteIds)
        // TODO: If the post is null the key has to be a `RemoteId` but what's to best way to ensure that?
        val postsToFetch = postMap.filter { it.value == null && it.key is RemoteId }.map { it.key as RemoteId }
        postListDataStoreHelper.fetchPosts(listDescriptor.site, postsToFetch)

        return itemIdentifiers.map { identifier ->
            when (identifier) {
                is LocalPostId -> postMap[identifier.id]
                is RemotePostId -> postMap[identifier.id]
                is EndListIndicator -> null
            }
        }.map {
            // TODO: We should do an actual transformation of the identifiers when this is moved to WPAndroid
            it!!
        }
    }

    private fun getItemIdsToHide(postListDescriptor: PostListDescriptor): Set<PostListItemIdentifier> {
        return performGetItemIdsToHide?.invoke(postListDescriptor) ?: emptySet()
    }
}
