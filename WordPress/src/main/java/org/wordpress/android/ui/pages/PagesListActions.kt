package org.wordpress.android.ui.pages

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import com.google.android.material.R as MaterialR

enum class ActionGroup(val id: Int) {
    VIEW(1),
    TAKE_AN_ACTION(2),
    SHARE_AND_PROMOTE(3),
    TRASH(4)
}

enum class PagesListAction(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val colorTint: Int = MaterialR.attr.colorOnSurface,
    val actionGroup: ActionGroup,
    val positionInGroup: Int
) {
    VIEW_PAGE(
        R.string.pages_view,
        R.drawable.gb_ic_external,
        actionGroup = ActionGroup.VIEW,
        positionInGroup = 1,
    ),
    CANCEL_AUTO_UPLOAD(
        R.string.pages_and_posts_cancel_auto_upload,
        R.drawable.gb_ic_undo,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        colorTint = R.attr.wpColorWarningDark,
        positionInGroup = 1,
    ),
    SET_PARENT(
        R.string.set_parent,
        R.drawable.ic_pages_set_as_parent,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 2,
    ),
    SET_AS_HOMEPAGE(
        R.string.pages_set_as_homepage,
        R.drawable.ic_homepage_16dp,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 3,
    ),
    SET_AS_POSTS_PAGE(
        R.string.pages_set_as_posts_page,
        R.drawable.ic_posts_16dp,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 4,
    ),
    COPY(
        R.string.button_copy,
        R.drawable.gb_ic_copy,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 7,
    ),
    COPY_LINK(
        R.string.button_share,
        R.drawable.gb_ic_share,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 8,
    ),
    PUBLISH_NOW(
        R.string.pages_publish_now,
        R.drawable.gb_ic_globe,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 3,
    ),
    PROMOTE_WITH_BLAZE(
        R.string.pages_promote_with_blaze,
        R.drawable.ic_blaze_flame_24dp,
        0,
        actionGroup = ActionGroup.SHARE_AND_PROMOTE,
        positionInGroup = 1,
    ),
    MOVE_TO_DRAFT(
        R.string.pages_move_to_draft,
        R.drawable.gb_ic_move_to,
        actionGroup = ActionGroup.TAKE_AN_ACTION,
        positionInGroup = 6,
    ),
    DELETE_PERMANENTLY(
        R.string.pages_delete_permanently,
        R.drawable.gb_ic_trash,
        actionGroup = ActionGroup.TRASH,
        colorTint = R.attr.wpColorError,
        positionInGroup = 1,
    ),
    MOVE_TO_TRASH(
        R.string.pages_move_to_trash,
        R.drawable.gb_ic_trash,
        actionGroup = ActionGroup.TRASH,
        colorTint = R.attr.wpColorError,
        positionInGroup = 2,
    );
}
