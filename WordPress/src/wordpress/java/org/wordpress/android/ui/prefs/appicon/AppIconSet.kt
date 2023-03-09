package org.wordpress.android.ui.prefs.appicon

import org.wordpress.android.R
import javax.inject.Inject

class AppIconSet @Inject constructor() : IAppIconSet {
    override val appIcons: List<AppIcon> = listOf(
        AppIcon.DEFAULT, BLACK, BLUE, CELADON, PINK
    )

    companion object {
        private val BLACK = AppIcon(
            id = "black",
            nameRes = R.string.app_icon_black_name,
            iconRes = R.mipmap.app_icon_black,
            alias = "org.wordpress.android.launch.WPLaunchActivityBlack"
        )
        private val BLUE = AppIcon(
            id = "blue",
            nameRes = R.string.app_icon_blue_name,
            iconRes = R.mipmap.app_icon_blue,
            alias = "org.wordpress.android.launch.WPLaunchActivityBlue"
        )
        private val CELADON = AppIcon(
            id = "celadon",
            nameRes = R.string.app_icon_celadon_name,
            iconRes = R.mipmap.app_icon_celadon,
            alias = "org.wordpress.android.launch.WPLaunchActivityCeladon"
        )
        private val PINK = AppIcon(
            id = "pink",
            nameRes = R.string.app_icon_pink_name,
            iconRes = R.mipmap.app_icon_pink,
            alias = "org.wordpress.android.launch.WPLaunchActivityPink"
        )
    }
}
