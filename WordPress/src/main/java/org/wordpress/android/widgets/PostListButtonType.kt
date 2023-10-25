package org.wordpress.android.widgets

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import com.google.android.material.R as MaterialR

// PostListButton.java types - from attrs.xml
enum class PostListButtonType constructor(
    val value: Int,
    @StringRes val textResId: Int,
    @DrawableRes val iconResId: Int,
    @AttrRes val colorAttrId: Int
) {
    BUTTON_EDIT(
        1,
        R.string.button_edit,
        R.drawable.gb_ic_pencil,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_VIEW(
        2,
        R.string.button_view,
        R.drawable.gb_ic_seen,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_PREVIEW(
        3,
        R.string.button_preview,
        R.drawable.gb_ic_globe,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_STATS(
        4,
        R.string.button_stats,
        R.drawable.gb_ic_chart_bar,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_TRASH(
        5,
        R.string.button_trash,
        R.drawable.gb_ic_trash,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_DELETE(
        6,
        R.string.button_delete,
        R.drawable.gb_ic_trash,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_PUBLISH(
        7,
        R.string.button_publish,
        R.drawable.gb_ic_globe,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_SYNC(
        8,
        R.string.button_sync,
        R.drawable.gb_ic_update,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_MORE(
        9,
        R.string.button_more,
        R.drawable.gb_ic_more_horizontal,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_SUBMIT(
        10,
        R.string.submit_for_review,
        R.drawable.ic_reader_white_24dp,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_RETRY(
        11,
        R.string.button_retry,
        R.drawable.gb_ic_redo,
        MaterialR.attr.colorError
    ),
    BUTTON_MOVE_TO_DRAFT(
        12,
        R.string.button_move_to_draft,
        R.drawable.gb_ic_move_to,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_DELETE_PERMANENTLY(
        13,
        R.string.button_delete_permanently,
        R.drawable.gb_ic_trash,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_CANCEL_PENDING_AUTO_UPLOAD(
        14,
        R.string.pages_and_posts_cancel_auto_upload,
        R.drawable.gb_ic_undo,
        R.attr.wpColorWarningDark
    ),
    BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG(
        15,
        0,
        0,
        0
    ),
    BUTTON_COPY(
        16,
        R.string.button_copy,
        R.drawable.gb_ic_copy,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_COPY_URL(
        17,
        R.string.button_copy_link,
        R.drawable.gb_ic_link,
        MaterialR.attr.colorOnSurface
    ),
    BUTTON_PROMOTE_WITH_BLAZE(
        18,
        R.string.button_promote_with_blaze,
        R.drawable.ic_promote_with_blaze,
        0
    );

    companion object {
        fun fromInt(value: Int): PostListButtonType? = values().firstOrNull { it.value == value }
    }
}
