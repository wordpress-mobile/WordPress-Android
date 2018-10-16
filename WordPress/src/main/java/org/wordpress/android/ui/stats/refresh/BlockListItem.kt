package org.wordpress.android.ui.stats.refresh

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE

sealed class BlockListItem(val type: Type) {
    enum class Type { TITLE, ITEM }
    data class Title(@StringRes val text: Int) : BlockListItem(TITLE)
    data class Item(@DrawableRes val icon: Int, @StringRes val text: Int, val value: String, val showDivider: Boolean = true) :
            BlockListItem(ITEM)
}
