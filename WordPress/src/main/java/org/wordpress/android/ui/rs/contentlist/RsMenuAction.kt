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
