package org.wordpress.android.ui.postsrs

import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
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
    val isPlaceholder: Boolean = false,
    val isError: Boolean = false
)

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
            isPlaceholder = true
        )
        is PostItemState.Failed -> PostRsUiModel(
            remotePostId = postId,
            title = "",
            excerpt = "",
            date = "",
            isError = true
        )
    }
}

private fun FullEntityAnyPostWithEditContext.toUiModel():
        PostRsUiModel {
    val post: AnyPostWithEditContext = data
    return PostRsUiModel(
        remotePostId = post.id,
        title = post.title?.raw?.takeIf { it.isNotBlank() }
            ?: post.title?.rendered
            ?: "",
        excerpt = (
            post.excerpt?.raw?.takeIf { it.isNotBlank() }
                ?: post.excerpt?.rendered
                ?: ""
            ).let { HtmlUtils.fastStripHtml(it).trim() },
        date = PostRsDateFormatter.format(
            post.dateGmt, post.status
        )
    )
}
