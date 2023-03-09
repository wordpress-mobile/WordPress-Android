package org.wordpress.android.ui.prefs.appicon

import org.wordpress.android.R
import javax.inject.Inject

class AppIconSet @Inject constructor() : IAppIconSet {
    override val appIcons: List<AppIcon> = listOf(
        AppIcon.DEFAULT, RED
    )

    companion object {
        val RED = AppIcon(
            id = "red",
            nameRes = R.string.app_icon_red_name,
            iconRes = R.mipmap.app_icon_red,
            alias = "org.wordpress.android.launch.WPLaunchActivityRed"
        )
    }
}
