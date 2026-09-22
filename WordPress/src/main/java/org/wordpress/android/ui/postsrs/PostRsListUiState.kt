package org.wordpress.android.ui.postsrs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.rs.contentlist.ContentDisplayState
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.contentlist.ContentListRowUiState
import org.wordpress.android.ui.rs.contentlist.RsMenuAction

/** A post as the rs list renders it, carrying the posts menu's own actions. */
internal typealias PostRsUiModel = ContentItemUiModel<PostRsMenuAction>

sealed interface PostRsConfirmation {
    data class Trash(val postId: Long) : PostRsConfirmation
    data class Delete(val postId: Long) : PostRsConfirmation
    data class MoveToDraft(val postId: Long) : PostRsConfirmation
}

enum class PostRsMenuAction(
    @StringRes override val labelResId: Int,
    @DrawableRes override val iconResId: Int,
    override val isDestructive: Boolean = false
) : RsMenuAction {
    SETTINGS(
        R.string.post_settings,
        R.drawable.ic_settings_white_24dp
    ),
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

/**
 * Projects a post onto the shared row model the redesigned list renders. Keeping the projection
 * here means the row component itself stays free of anything post-specific, so the pages screen
 * can supply its own equivalent.
 */
fun PostRsUiModel.toContentListRowUiState() = ContentListRowUiState(
    id = remoteId,
    title = title,
    excerpt = excerpt,
    dateLabel = date,
    thumbnailImageUrl = featuredImage?.thumbnail,
    heroImageUrl = featuredImage?.hero,
    isImagePending = featuredImageId != 0L &&
        featuredImage == null &&
        !isFeaturedImageUnresolvable,
    viewCount = viewCount,
    commentCount = commentCount,
    areMetricsPending = areMetricsPending,
    badges = badges,
    isSyncing = displayState == ContentDisplayState.FETCHING_WITH_DATA,
    hasSyncFailed = displayState == ContentDisplayState.FAILED_WITH_DATA
)
