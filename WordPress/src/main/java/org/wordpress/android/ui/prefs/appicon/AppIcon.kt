package org.wordpress.android.ui.prefs.appicon

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

data class AppIcon(
    val id: String,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val alias: String,
) {
    companion object {
        @JvmField
        val DEFAULT = AppIcon(
            id = "default",
            nameRes = R.string.app_icon_default_name,
            iconRes = R.mipmap.app_icon,
            alias = "org.wordpress.android.launch.WPLaunchActivityDefault"
        )
    }
}
