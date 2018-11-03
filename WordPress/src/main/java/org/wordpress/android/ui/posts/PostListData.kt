package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListState

class PostListData(
    private val items: List<PostAdapterItemType>?,
    private val listManager: ListManager<PostModel>?,
    listState: ListState?,
    val isAztecEditorEnabled: Boolean,
    val isPhotonCapable: Boolean,
    val hasCapabilityPublishPosts: Boolean
) {
    val isLoadingMore: Boolean = listState?.isLoadingMore() ?: false
    val isLoadingFirstPage: Boolean = if (items == null) {
        // If `items` is null, that means we haven't loaded the data yet, which means we are loading the first page
        true
    } else listState?.isFetchingFirstPage() ?: false

    val size: Int = items?.size ?: 0

    fun getItem(
        index: Int,
        shouldFetchIfNull: Boolean = false,
        shouldLoadMoreIfNecessary: Boolean = false
    ): PostAdapterItemType {
        // TODO: Rework fetch item in ListManager
        listManager?.let {
            if (index < it.size) {
                it.getItem(index, shouldFetchIfNull, shouldLoadMoreIfNecessary)
            }
        }
        return requireNotNull(items) { "Wrong item size is passed while items is null" }[index]
    }
}
