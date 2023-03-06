package org.wordpress.android.launch

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.util.PackageManagerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconHelper @Inject constructor(
//    @ApplicationContext private val appContext: Context,
    private val packageManagerWrapper: PackageManagerWrapper,
) {
    enum class IconType(
        val id: String,
        @DrawableRes val icon: Int,
        val alias: String,
    ) {
        DEFAULT(
            id = "default",
            icon = R.mipmap.app_icon,
            alias = ".launch.WPLaunchActivityDefault"
        ),
        RED(
            id = "red",
            icon = R.mipmap.app_icon_red,
            alias = ".launch.WPLaunchActivityRed"
        ),
    }

    private val iconTypes = IconType.values()

    fun enableSelectedType(type: IconType) {
        val classpath = "org.wordpress.android"
        iconTypes.forEach {
            val component = "$classpath${it.alias}"

            if (it == type) {
                packageManagerWrapper.enableComponentEnabledSetting(component)
            } else {
                packageManagerWrapper.disableComponentEnabledSetting(component)
            }
        }
    }
}
