package org.wordpress.android.widgets

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import com.google.android.material.R as MaterialR

private const val VIEW_GROUP_ID = 1
private const val TAKE_AN_ACTION_GROUP_ID = 2
private const val SHARE_AND_PROMOTE_GROUP_ID = 3
private const val NAVIGATE_GROUP_ID = 4
private const val TRASH_GROUP_ID = 5
private const val OTHER_GROUP_ID = 6

// PostListButton.java types - from attrs.xml
enum class PostListButtonType constructor(
    val value: Int,
    @StringRes val textResId: Int,
    @DrawableRes val iconResId: Int,
    @AttrRes val colorAttrId: Int,
    val groupId: Int,
    val positionInGroup: Int
) {
    BUTTON_EDIT(
        1,
        R.string.button_edit,
        R.drawable.gb_ic_pencil,
        MaterialR.attr.colorOnSurface,
        VIEW_GROUP_ID,
        1
    ),
    BUTTON_VIEW(
        2,
        R.string.button_view,
        R.drawable.gb_ic_external,
        MaterialR.attr.colorOnSurface,
        VIEW_GROUP_ID,
        1
    ),
    BUTTON_PREVIEW(
        3,
        R.string.button_preview,
        R.drawable.gb_ic_globe,
        MaterialR.attr.colorOnSurface,
        VIEW_GROUP_ID,
        2
    ),
    BUTTON_STATS(
        4,
        R.string.button_stats,
        R.drawable.gb_ic_chart_bar,
        MaterialR.attr.colorOnSurface,
        NAVIGATE_GROUP_ID,
        1
    ),
    BUTTON_TRASH(
        5,
        R.string.button_trash,
        R.drawable.gb_ic_trash,
        R.attr.wpColorError,
        TRASH_GROUP_ID,
        3
    ),
    BUTTON_DELETE(
        6,
        R.string.button_delete,
        R.drawable.gb_ic_trash,
        R.attr.wpColorError,
        TRASH_GROUP_ID,
        1
    ),
    BUTTON_PUBLISH(
        7,
        R.string.button_publish,
        R.drawable.gb_ic_globe,
        MaterialR.attr.colorOnSurface,
        TAKE_AN_ACTION_GROUP_ID,
        2
    ),
    BUTTON_SYNC(
        8,
        R.string.button_sync,
        R.drawable.gb_ic_update,
        MaterialR.attr.colorOnSurface,
        TAKE_AN_ACTION_GROUP_ID,
        4
    ),
    BUTTON_MORE(
        9,
        R.string.button_more,
        R.drawable.gb_ic_more_horizontal,
        MaterialR.attr.colorOnSurface,
        OTHER_GROUP_ID,
        1
    ),
    BUTTON_SUBMIT(
        10,
        R.string.submit_for_review,
        R.drawable.gb_ic_post_author,
        MaterialR.attr.colorOnSurface,
        TAKE_AN_ACTION_GROUP_ID,
        5
    ),
    BUTTON_RETRY(
        11,
        R.string.button_retry,
        R.drawable.gb_ic_redo,
        R.attr.wpColorError,
        TAKE_AN_ACTION_GROUP_ID,
        3
    ),
    BUTTON_MOVE_TO_DRAFT(
        12,
        R.string.button_move_to_draft,
        R.drawable.gb_ic_move_to,
        MaterialR.attr.colorOnSurface,
        TAKE_AN_ACTION_GROUP_ID,
        6
    ),
    BUTTON_DELETE_PERMANENTLY(
        13,
        R.string.button_delete_permanently,
        R.drawable.gb_ic_trash,
        R.attr.wpColorError,
        TRASH_GROUP_ID,
        3
    ),
    BUTTON_CANCEL_PENDING_AUTO_UPLOAD(
        14,
        R.string.pages_and_posts_cancel_auto_upload,
        R.drawable.gb_ic_undo,
        R.attr.wpColorWarningDark,
        TAKE_AN_ACTION_GROUP_ID,
        1
    ),
    BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG(
        15,
        0,
        0,
        0,
        TRASH_GROUP_ID,
        4
    ),
    BUTTON_COPY(
        16,
        R.string.button_copy,
        R.drawable.gb_ic_copy,
        MaterialR.attr.colorOnSurface,
        TAKE_AN_ACTION_GROUP_ID,
        7
    ),
    BUTTON_SHARE(
        17,
        R.string.button_share,
        R.drawable.gb_ic_share,
        MaterialR.attr.colorOnSurface,
        SHARE_AND_PROMOTE_GROUP_ID,
        1
    ),
    BUTTON_PROMOTE_WITH_BLAZE(
        18,
        R.string.button_promote_with_blaze,
        R.drawable.ic_blaze_flame_24dp,
        MaterialR.attr.colorOnSurface,
        SHARE_AND_PROMOTE_GROUP_ID,
        2
    ),
    BUTTON_COMMENTS(
        19,
        R.string.button_comments,
        R.drawable.gb_ic_comment,
        MaterialR.attr.colorOnSurface,
        NAVIGATE_GROUP_ID,
        2
    ),
    BUTTON_READ(
        20,
        R.string.button_read,
        R.drawable.ic_reader_glasses_white_24dp,
        MaterialR.attr.colorOnSurface,
        VIEW_GROUP_ID,
        1
    );

    companion object {
        fun fromInt(value: Int): PostListButtonType? = values().firstOrNull { it.value == value }
    }
}
