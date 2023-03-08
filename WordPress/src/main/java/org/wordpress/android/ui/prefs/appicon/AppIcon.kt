package org.wordpress.android.ui.prefs.appicon

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

enum class AppIcon(
    val id: String,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val alias: String,
) {
    DEFAULT(
        id = "default",
        nameRes = R.string.app_icon_default_name,
        iconRes = R.mipmap.app_icon,
        alias = "org.wordpress.android.launch.WPLaunchActivityDefault"
    ),
    RED(
        id = "red",
        nameRes = R.string.app_icon_red_name,
        iconRes = R.mipmap.app_icon_red,
        alias = "org.wordpress.android.launch.WPLaunchActivityRed"
    );

    companion object {
        @JvmStatic
        fun fromId(id: String): AppIcon {
            return values().firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
