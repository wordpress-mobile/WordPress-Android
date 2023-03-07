package org.wordpress.android.ui.prefs.appicon

import androidx.annotation.DrawableRes
import org.wordpress.android.R

enum class AppIcon(
    val id: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    val alias: String,
) {
    DEFAULT(
        id = "default",
        displayName = "Default",
        icon = R.mipmap.app_icon,
        alias = "org.wordpress.android.launch.WPLaunchActivityDefault"
    ),
    RED(
        id = "red",
        displayName = "Red",
        icon = R.mipmap.app_icon_red,
        alias = "org.wordpress.android.launch.WPLaunchActivityRed"
    );

    companion object {
        fun fromId(id: String): AppIcon {
            return values().firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
