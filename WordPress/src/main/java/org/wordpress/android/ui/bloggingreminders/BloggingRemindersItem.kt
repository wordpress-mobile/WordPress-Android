package org.wordpress.android.ui.bloggingreminders

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.CLOSE_BUTTON
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.ILLUSTRATION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TITLE
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class BloggingRemindersItem(val type: Type) {
    enum class Type {
        CLOSE_BUTTON, ILLUSTRATION, TITLE
    }
    data class CloseButton(val listItemInteraction: ListItemInteraction): BloggingRemindersItem(CLOSE_BUTTON)
    data class Illustration(@DrawableRes val illustration: Int): BloggingRemindersItem(ILLUSTRATION)
    data class Title(val text: UiString) : BloggingRemindersItem(TITLE)
}
