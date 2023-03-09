package org.wordpress.android.ui.prefs.appicon

import org.wordpress.android.R
import javax.inject.Inject

class AppIconSet @Inject constructor() : IAppIconSet {
    override val appIcons: List<AppIcon> = listOf(
        AppIcon.DEFAULT, PURPLE
    )

    companion object {
        val PURPLE = AppIcon(
            id = "purple",
            nameRes = R.string.app_icon_purple_name,
            iconRes = R.mipmap.app_icon_purple,
            alias = "org.wordpress.android.launch.WPLaunchActivityPurple"
        )
    }
}
