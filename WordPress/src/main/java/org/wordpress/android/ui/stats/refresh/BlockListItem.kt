package org.wordpress.android.ui.stats.refresh

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.text.Spannable
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.COLUMNS
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.ITEM
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TEXT
import org.wordpress.android.ui.stats.refresh.BlockListItem.Type.TITLE

sealed class BlockListItem(val type: Type) {
    enum class Type { TITLE, ITEM, EMPTY, TEXT, COLUMNS, LINK }
    data class Title(@StringRes val text: Int) : BlockListItem(TITLE)
    data class Item(
        @DrawableRes val icon: Int,
        @StringRes val text: Int,
        val value: String,
        val showDivider: Boolean = true
    ) : BlockListItem(ITEM)
    data class Text(val text: Spannable? = null) : BlockListItem(TEXT)
    data class Columns(val headers: List<Int>, val values: List<String>) : BlockListItem(COLUMNS)
    data class Link(@DrawableRes val icon: Int? = null, @StringRes val text: Int, val action: () -> Unit) : BlockListItem(LINK)
    object Empty : BlockListItem(EMPTY)
}
