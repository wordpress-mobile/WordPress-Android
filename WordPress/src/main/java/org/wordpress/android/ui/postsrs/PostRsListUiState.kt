package org.wordpress.android.ui.postsrs

import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostStatus
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState

data class PostTabUiState(
    val posts: List<PostRsUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null
)

data class PostRsUiModel(
    val remotePostId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val statusLabel: String,
    val isPlaceholder: Boolean = false,
    val isError: Boolean = false
)

sealed class PostRsListUiEvent {
    data class ShowError(val message: String) : PostRsListUiEvent()
    data class OpenPost(val remotePostId: Long) : PostRsListUiEvent()
}

fun PostItemState.toUiModel(postId: Long): PostRsUiModel {
    return when (this) {
        is PostItemState.Fresh -> data.toUiModel()
        is PostItemState.Stale -> data.toUiModel()
        is PostItemState.FetchingWithData -> data.toUiModel()
        is PostItemState.FailedWithData -> data.toUiModel()
        is PostItemState.Missing,
        is PostItemState.Fetching -> PostRsUiModel(
            remotePostId = postId,
            title = "",
            excerpt = "",
            date = "",
            statusLabel = "",
            isPlaceholder = true
        )
        is PostItemState.Failed -> PostRsUiModel(
            remotePostId = postId,
            title = "",
            excerpt = "",
            date = "",
            statusLabel = "",
            isError = true
        )
    }
}

private fun FullEntityAnyPostWithEditContext.toUiModel(): PostRsUiModel {
    val post: AnyPostWithEditContext = data
    return PostRsUiModel(
        remotePostId = post.id,
        title = post.title?.raw?.takeIf { it.isNotBlank() }
            ?: post.title?.rendered
            ?: "",
        excerpt = post.excerpt?.raw?.takeIf { it.isNotBlank() }
            ?: post.excerpt?.rendered
            ?: "",
        date = post.date ?: "",
        statusLabel = post.status.toLabel()
    )
}

private fun PostStatus?.toLabel(): String = when (this) {
    is PostStatus.Publish -> "Published"
    is PostStatus.Draft -> "Draft"
    is PostStatus.Pending -> "Pending"
    is PostStatus.Private -> "Private"
    is PostStatus.Future -> "Scheduled"
    is PostStatus.Trash -> "Trashed"
    is PostStatus.Custom -> "Custom"
    null -> ""
}
