package org.wordpress.android.ui.postsrs

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.util.HtmlUtils
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
    val link: String = "",
    val hasPassword: Boolean = false,
    val status: PostStatus? = null,
    @StringRes val statusLabelResId: Int = 0,
    val actions: List<PostRsMenuAction> = emptyList(),
    val isPlaceholder: Boolean = false,
    val isError: Boolean = false
)

enum class PostRsMenuAction(@StringRes val labelResId: Int) {
    VIEW(R.string.button_view),
    READ(R.string.button_read),
    MOVE_TO_DRAFT(R.string.button_move_to_draft),
    DUPLICATE(R.string.button_copy),
    SHARE(R.string.button_share),
    BLAZE(R.string.button_promote_with_blaze),
    STATS(R.string.button_stats),
    COMMENTS(R.string.button_comments),
    TRASH(R.string.button_trash),
    DELETE_PERMANENTLY(R.string.button_delete_permanently),
}

fun PostItemState.toUiModel(
    postId: Long,
    showStatus: Boolean = false
): PostRsUiModel {
    return when (this) {
        is PostItemState.Fresh ->
            data.toUiModel(showStatus)
        is PostItemState.Stale ->
            data.toUiModel(showStatus)
        is PostItemState.FetchingWithData ->
            data.toUiModel(showStatus)
        is PostItemState.FailedWithData ->
            data.toUiModel(showStatus)
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

private fun FullEntityAnyPostWithEditContext.toUiModel(
    showStatus: Boolean
): PostRsUiModel {
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
        ),
        link = post.link,
        hasPassword = !post.password.isNullOrEmpty(),
        status = post.status,
        statusLabelResId = if (showStatus) {
            post.status.toLabel()
        } else {
            0
        }
    )
}

@StringRes
private fun PostStatus?.toLabel(): Int = when (this) {
    is PostStatus.Publish ->
        R.string.post_status_post_published
    is PostStatus.Draft -> R.string.post_status_draft
    is PostStatus.Pending ->
        R.string.post_status_pending_review
    is PostStatus.Private ->
        R.string.post_status_post_private
    is PostStatus.Future ->
        R.string.post_status_post_scheduled
    is PostStatus.Trash ->
        R.string.post_status_post_trashed
    is PostStatus.Custom -> 0
    null -> 0
}
