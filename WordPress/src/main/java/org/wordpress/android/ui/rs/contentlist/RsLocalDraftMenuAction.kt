package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

/** The overflow menu of a local draft row, shared by the posts and pages lists. */
enum class RsLocalDraftMenuAction(
    @StringRes override val labelResId: Int,
    @DrawableRes override val iconResId: Int,
    override val isDestructive: Boolean = false
) : RsMenuAction {
    RETRY(R.string.retry, R.drawable.ic_refresh_white_24dp),
    DELETE(R.string.delete, R.drawable.gb_ic_trash, isDestructive = true)
}
