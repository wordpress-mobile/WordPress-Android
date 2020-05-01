package org.wordpress.android.widgets

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

// PostListButton.java types - from attrs.xml
enum class PostListButtonType constructor(
    val value: Int,
    @StringRes val textResId: Int,
    @DrawableRes val iconResId: Int,
    @AttrRes val colorAttrId: Int
) {
    BUTTON_EDIT(1, R.string.button_edit, R.drawable.ic_pencil_white_24dp, R.attr.colorOnSurface),
    BUTTON_VIEW(2, R.string.button_view, R.drawable.ic_external_white_24dp, R.attr.colorOnSurface),
    BUTTON_PREVIEW(3, R.string.button_preview, R.drawable.ic_external_white_24dp, R.attr.colorOnSurface),
    BUTTON_STATS(4, R.string.button_stats, R.drawable.ic_stats_alt_white_24dp, R.attr.colorOnSurface),
    BUTTON_TRASH(5, R.string.button_trash, R.drawable.ic_trash_white_24dp, R.attr.colorOnSurface),
    BUTTON_DELETE(6, R.string.button_delete, R.drawable.ic_trash_white_24dp, R.attr.colorOnSurface),
    BUTTON_PUBLISH(7, R.string.button_publish, R.drawable.ic_reader_white_24dp, R.attr.colorOnSurface),
    BUTTON_SYNC(8, R.string.button_sync, R.drawable.ic_reader_white_24dp, R.attr.colorOnSurface),
    BUTTON_MORE(9, R.string.button_more, R.drawable.ic_ellipsis_white_24dp, R.attr.colorOnSurface),
    BUTTON_SUBMIT(10, R.string.submit_for_review, R.drawable.ic_reader_white_24dp, R.attr.colorOnSurface),
    BUTTON_RETRY(11, R.string.button_retry, R.drawable.ic_refresh_white_24dp, R.attr.colorError),
    BUTTON_MOVE_TO_DRAFT(12, R.string.button_move_to_draft, R.drawable.ic_refresh_white_24dp, R.attr.colorOnSurface),
    BUTTON_DELETE_PERMANENTLY(
            13,
            R.string.button_delete_permanently,
            R.drawable.ic_trash_white_24dp,
            R.attr.colorOnSurface
    ),
    BUTTON_CANCEL_PENDING_AUTO_UPLOAD(
            14,
            R.string.pages_and_posts_cancel_auto_upload,
            R.drawable.ic_undo_white_24dp,
            R.attr.wpColorWarningDark
    );

    companion object {
        fun fromInt(value: Int): PostListButtonType? = values().firstOrNull { it.value == value }
    }
}
