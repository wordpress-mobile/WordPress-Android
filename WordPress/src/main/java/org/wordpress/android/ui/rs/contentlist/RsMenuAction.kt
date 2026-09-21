package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * What the shared overflow menu needs from a screen's own action enum.
 *
 * The posts and pages enums are deliberately not merged - they offer different actions, and the
 * ones that overlap use different wording ("Move to trash" against a page, "Trash" against a post).
 * This is the shape they share, so neither the menu nor [toContentListMenuActions] has to know
 * about either.
 */
interface RsMenuAction {
    @get:StringRes val labelResId: Int

    @get:DrawableRes val iconResId: Int

    val isDestructive: Boolean
}

/** Projects a row's actions onto what [ContentListOverflowMenu] renders. */
fun <A : RsMenuAction> List<A>.toContentListMenuActions(
    onAction: (A) -> Unit
): List<ContentListMenuAction> = map { action ->
    ContentListMenuAction(
        labelResId = action.labelResId,
        iconResId = action.iconResId,
        isDestructive = action.isDestructive
    ) { onAction(action) }
}
