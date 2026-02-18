package org.wordpress.android.ui.postsrs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostStatus
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState

sealed interface PendingConfirmation {
    data class Trash(val postId: Long) : PendingConfirmation
    data class Delete(val postId: Long) : PendingConfirmation
}

data class ConfirmationDialogState(
    val pending: PendingConfirmation? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

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
    val commentsOpen: Boolean = false,
    val status: PostStatus? = null,
    @StringRes val statusLabelResId: Int = 0,
    val actions: List<PostRsMenuAction> = emptyList(),
    val isPlaceholder: Boolean = false,
    val isError: Boolean = false
)

enum class PostRsMenuAction(
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    val isDestructive: Boolean = false
) {
    VIEW(R.string.button_view, R.drawable.gb_ic_external),
    READ(
        R.string.button_read,
        R.drawable.ic_reader_glasses_white_24dp
    ),
    PUBLISH(
        R.string.button_publish,
        R.drawable.gb_ic_globe
    ),
    MOVE_TO_DRAFT(
        R.string.button_move_to_draft,
        R.drawable.gb_ic_move_to
    ),
    DUPLICATE(R.string.button_copy, R.drawable.gb_ic_copy),
    SHARE(R.string.button_share, R.drawable.gb_ic_share),
    BLAZE(
        R.string.button_promote_with_blaze,
        R.drawable.ic_blaze_flame_24dp
    ),
    STATS(R.string.button_stats, R.drawable.gb_ic_chart_bar),
    COMMENTS(
        R.string.button_comments,
        R.drawable.gb_ic_comment
    ),
    TRASH(
        R.string.button_trash,
        R.drawable.gb_ic_trash,
        isDestructive = true
    ),
    DELETE_PERMANENTLY(
        R.string.button_delete_permanently,
        R.drawable.gb_ic_trash,
        isDestructive = true
    ),
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
        commentsOpen =
            post.commentStatus is PostCommentStatus.Open,
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
