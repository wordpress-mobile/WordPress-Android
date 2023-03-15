package org.wordpress.android.ui.prefs.appicon

import org.wordpress.android.R
import javax.inject.Inject

class AppIconSet @Inject constructor() : IAppIconSet {
    override val appIcons: List<AppIcon> = listOf(
        AppIcon.DEFAULT, BLACK, WHITE_ON_BLACK, WHITE_ON_GREEN, CELADON, GREEN, PINK_BLUE, PRIDE, NEU_DARK, NEU_LIGHT
    )

    companion object {
        private val BLACK = AppIcon(
            id = "black",
            nameRes = R.string.app_icon_black_name,
            iconRes = R.mipmap.app_icon_black,
            alias = "org.wordpress.android.launch.WPLaunchActivityBlack"
        )
        private val WHITE_ON_BLACK = AppIcon(
            id = "white_on_black",
            nameRes = R.string.app_icon_white_on_black_name,
            iconRes = R.mipmap.app_icon_white_on_black,
            alias = "org.wordpress.android.launch.WPLaunchActivityWhiteOnBlack"
        )
        private val WHITE_ON_GREEN = AppIcon(
            id = "white_on_green",
            nameRes = R.string.app_icon_white_on_green_name,
            iconRes = R.mipmap.app_icon_white_on_green,
            alias = "org.wordpress.android.launch.WPLaunchActivityWhiteOnGreen"
        )
        private val CELADON = AppIcon(
            id = "celadon",
            nameRes = R.string.app_icon_celadon_name,
            iconRes = R.mipmap.app_icon_celadon,
            alias = "org.wordpress.android.launch.WPLaunchActivityCeladon"
        )
        private val GREEN = AppIcon(
            id = "green",
            nameRes = R.string.app_icon_green_name,
            iconRes = R.mipmap.app_icon_green,
            alias = "org.wordpress.android.launch.WPLaunchActivityGreen"
        )
        private val PINK_BLUE = AppIcon(
            id = "pink_blue",
            nameRes = R.string.app_icon_pink_blue_name,
            iconRes = R.mipmap.app_icon_pink_blue,
            alias = "org.wordpress.android.launch.WPLaunchActivityPinkBlue"
        )
        private val PRIDE = AppIcon(
            id = "pride",
            nameRes = R.string.app_icon_pride_name,
            iconRes = R.mipmap.app_icon_pride,
            alias = "org.wordpress.android.launch.WPLaunchActivityPride"
        )
        private val NEU_DARK = AppIcon(
            id = "neu_dark",
            nameRes = R.string.app_icon_neu_dark_name,
            iconRes = R.mipmap.app_icon_neu_dark,
            alias = "org.wordpress.android.launch.WPLaunchActivityNeuDark"
        )
        private val NEU_LIGHT = AppIcon(
            id = "neu_light",
            nameRes = R.string.app_icon_neu_light_name,
            iconRes = R.mipmap.app_icon_neu_light,
            alias = "org.wordpress.android.launch.WPLaunchActivityNeuLight"
        )
    }
}
