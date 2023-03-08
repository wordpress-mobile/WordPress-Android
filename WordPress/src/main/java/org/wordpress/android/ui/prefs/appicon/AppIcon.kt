package org.wordpress.android.ui.prefs.appicon

import androidx.annotation.DrawableRes
import org.wordpress.android.R

enum class AppIcon(
    val id: String,
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val alias: String,
) {
    DEFAULT(
        id = "default",
        displayName = "Default",
        iconRes = R.mipmap.app_icon,
        alias = "org.wordpress.android.launch.WPLaunchActivityDefault"
    ),
    RED(
        id = "red",
        displayName = "Red",
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
