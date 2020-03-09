package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.PostSummary
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.EndListIndicatorIdentifier
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.SectionHeaderIdentifier
import org.wordpress.android.viewmodel.posts.PostListItemType.EndListIndicatorItem
import org.wordpress.android.viewmodel.posts.PostListItemType.LoadingItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemType.SectionHeaderItem

sealed class PostListItemIdentifier {
    data class LocalPostId(val id: LocalId) : PostListItemIdentifier()
    data class RemotePostId(val id: RemoteId) : PostListItemIdentifier()
    object EndListIndicatorIdentifier : PostListItemIdentifier()
    data class SectionHeaderIdentifier(val type: PostListType) : PostListItemIdentifier()
}

/**
 * This is the post list data source to be used by `ListStore`. Before making any changes, it's important to know:
 *
 * 1. Lists managed by `ListStore` works by first fetching a smaller version of the models and then fetching the
 * actual model if necessary.
 * 2. During [getItemIdentifiers] the actual models might not be available and should not be relied upon. For post list
 * specifically, we have [PostSummary] that's always available. If a field that's not in the [PostSummary] is necessary
 * in [getItemIdentifiers], one should add that to [PostSummary] in FluxC and make sure that field is fetched
 * and updated in [PostStore].
 * 3. In [getItemsAndFetchIfNecessary], if the actual model is not available, this class is responsible for fetching
 * that model. For this post list specifically, when the actual model is fetched the list will update itself.
 *
 * // TODO: We can add a link to the wiki for ListStore when that's available.
 * For more information, please see the documentation for `ListStore` components.
 */
class PostListItemDataSource(
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val postFetcher: PostFetcher,
    private val transform: (PostModel) -> PostListItemUiState,
    private val postListType: PostListType
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
        val localPostIds = postStore.getLocalPostIdsForDescriptor(listDescriptor)
        val actualItems: List<PostListItemIdentifier> = if (postListType == SEARCH) {
            getGroupedItemIdentifiers(listDescriptor, localPostIds, remoteItemIds)
        } else {
            localPostIds.map { LocalPostId(id = it) } + remoteItemIds.map { RemotePostId(id = it) }
        }

        // We only want to show the end list indicator if the list is fully fetched and it's not empty
        return if (isListFullyFetched && actualItems.isNotEmpty()) {
            (actualItems + listOf(EndListIndicatorIdentifier))
        } else actualItems
    }

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: PostListDescriptor,
        itemIdentifiers: List<PostListItemIdentifier>
    ): List<PostListItemType> {
        val localOrRemoteIds = localOrRemoteIdsFromPostListItemIds(itemIdentifiers)
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
                is SectionHeaderIdentifier -> SectionHeaderItem(identifier.type)
            }
        }
    }

    private fun localOrRemoteIdsFromPostListItemIds(
        itemIdentifiers: List<PostListItemIdentifier>
    ): List<LocalOrRemoteId> {
        return itemIdentifiers.mapNotNull {
            when (it) {
                is LocalPostId -> it.id
                is RemotePostId -> it.id
                // We are creating a list of local and remote ids, so other type of identifiers don't matter
                EndListIndicatorIdentifier -> null
                is SectionHeaderIdentifier -> null
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
                val options = if (postListType == TRASHED) {
                    LoadingItemTrashedPost
                } else {
                    LoadingItemDefaultPost
                }
                LoadingItem(localOrRemoteId, options)
            } else {
                transform(post)
            }

    private fun getGroupedItemIdentifiers(
        listDescriptor: PostListDescriptor,
        localPostIds: List<LocalId>,
        remotePostIds: List<RemoteId>
    ): List<PostListItemIdentifier> {
        val listDrafts = mutableListOf<PostListItemIdentifier>()
        val listPublished = mutableListOf<PostListItemIdentifier>()
        val listScheduled = mutableListOf<PostListItemIdentifier>()
        val listTrashed = mutableListOf<PostListItemIdentifier>()

        val localIdentifiers = postStore.getPostsByLocalOrRemotePostIds(localPostIds, listDescriptor.site)
                .map { Pair(PostStatus.fromPost(it), LocalPostId(LocalId(it.id))) }
        val remoteIdentifiers = postStore.getPostSummaries(
                listDescriptor.site,
                remotePostIds.map { it.value }).map { Pair(it.status, RemotePostId(RemoteId(it.remoteId))) }

        (localIdentifiers + remoteIdentifiers).forEach {
            when (PostListType.fromPostStatus(it.first)) {
                PostListType.DRAFTS -> listDrafts.add(it.second)
                PostListType.PUBLISHED -> listPublished.add(it.second)
                PostListType.SCHEDULED -> listScheduled.add(it.second)
                TRASHED -> listTrashed.add(it.second)
                // We are grouping Post results into display groups. Search isn't a valid post type so it can be ignored.
                SEARCH -> {}
            }
        }

        val allItems = mutableListOf<PostListItemIdentifier>()
        if (listPublished.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(PostListType.PUBLISHED)) + listPublished
        }

        if (listDrafts.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(PostListType.DRAFTS)) + listDrafts
        }

        if (listScheduled.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(PostListType.SCHEDULED)) + listScheduled
        }

        if (listTrashed.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(TRASHED)) + listTrashed
        }

        return allItems
    }
}
