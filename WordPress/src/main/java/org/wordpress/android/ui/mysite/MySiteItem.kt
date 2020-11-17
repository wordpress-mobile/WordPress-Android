package org.wordpress.android.ui.mysite

sealed class MySiteItem(val type: Type) {
    enum class Type {
        SITE_BLOCK, HEADER, LIST_ITEM
    }
}
