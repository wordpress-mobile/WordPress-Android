package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** What the shared overflow menu needs from a screen's own action enum. */
interface RsMenuAction {
    @get:StringRes val labelResId: Int

    @get:DrawableRes val iconResId: Int

    val isDestructive: Boolean

    /** Set for actions a redesigned row shows as a footer button instead of in its overflow menu. */
    val quickActionType: ContentListQuickActionType? get() = null
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
