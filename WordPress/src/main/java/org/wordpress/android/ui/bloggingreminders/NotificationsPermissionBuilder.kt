package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class NotificationsPermissionBuilder @Inject constructor() {
    fun buildUiItems(appName: String, showAppSettingsGuide: Boolean): List<BloggingRemindersItem> {
        val title = UiStringRes(string.blogging_reminders_notifications_permission_title)

        val body = UiString.UiStringResWithParams(
            string.blogging_reminders_notifications_permission_description,
            UiString.UiStringText(appName)
        )

        val uiItems = mutableListOf(
            Illustration(drawable.img_illustration_bell_yellow_96dp),
            Title(title),
            Caption(UiStringRes(string.blogging_reminders_notifications_permission_caption))
        )
        if (showAppSettingsGuide) {
            uiItems.add(HighEmphasisText(EmphasizedText(body, false)))
        }
        return uiItems
    }

    fun buildPrimaryButton(onDone: () -> Unit): PrimaryButton {
        return PrimaryButton(
            UiStringRes(string.blogging_reminders_notifications_permission_primary_button),
            enabled = true,
            ListItemInteraction.create(onDone)
        )
    }
}
