package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TITLE
import org.wordpress.android.ui.utils.UiString

sealed class BloggingRemindersItem(val type: Type) {
    enum class Type {
        TITLE
    }
    data class Title(val text: UiString): BloggingRemindersItem(TITLE)
}

