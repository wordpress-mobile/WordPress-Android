package org.wordpress.android.ui.mysite

import androidx.annotation.StringRes
import org.wordpress.android.R.string

sealed class SiteDialogModel(
    val tag: String,
    @StringRes val title: Int,
    @StringRes open val message: Int,
    @StringRes val positiveButtonLabel: Int,
    @StringRes val negativeButtonLabel: Int? = null,
    @StringRes val cancelButtonLabel: Int? = null
) {
    object AddSiteIconDialogModel : SiteDialogModel(
            MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG, string.my_site_icon_dialog_title,
            string.my_site_icon_dialog_add_message,
            string.yes,
            string.no,
            null
    )

    object ChangeSiteIconDialogModel : SiteDialogModel(
            MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG,
            string.my_site_icon_dialog_title,
            string.my_site_icon_dialog_change_message,
            string.my_site_icon_dialog_change_button,
            string.my_site_icon_dialog_remove_button,
            string.my_site_icon_dialog_cancel_button
    )
}
