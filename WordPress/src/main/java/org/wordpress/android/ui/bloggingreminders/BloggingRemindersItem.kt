package org.wordpress.android.ui.bloggingreminders

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.ILLUSTRATION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.PRIMARY_BUTTON
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TEXT
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TITLE
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class BloggingRemindersItem(val type: Type) {
    enum class Type {
        ILLUSTRATION, TITLE, TEXT, PRIMARY_BUTTON
    }

    data class Illustration(@DrawableRes val illustration: Int) : BloggingRemindersItem(ILLUSTRATION)
    data class Title(val text: UiString) : BloggingRemindersItem(TITLE)
    data class Text(val text: UiString) : BloggingRemindersItem(TEXT)
    data class PrimaryButton(val text: UiString, val enabled: Boolean, val onClick: ListItemInteraction) :
        BloggingRemindersItem(PRIMARY_BUTTON)
}
