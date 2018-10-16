package org.wordpress.android.ui.stats.refresh

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

sealed class BlockListItem {
    data class Title(@StringRes val text: Int): BlockListItem()
    data class Item(@DrawableRes val icon: Int, @StringRes val text: Int, val value: String): BlockListItem()
}
