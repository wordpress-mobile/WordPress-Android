package org.wordpress.android.ui.prefs.appicon

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.PackageManagerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconHelper @Inject constructor(
    private val packageManagerWrapper: PackageManagerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val appIconSet: AppIconSet,
) {
    val appIcons get() = appIconSet.appIcons

    fun getCurrentIcon(): AppIcon = iconFromId(appPrefsWrapper.currentAppIconId)

    fun setCurrentIcon(icon: AppIcon) {
        enableSelectedIcon(icon)
        appPrefsWrapper.currentAppIconId = icon.id
    }

    private fun enableSelectedIcon(icon: AppIcon) {
        appIcons.forEach {
            if (it == icon) {
                packageManagerWrapper.enableComponentEnabledSetting(it.alias)
            } else {
                packageManagerWrapper.disableComponentEnabledSetting(it.alias)
            }
        }
    }

    private fun iconFromId(id: String): AppIcon = appIcons.firstOrNull { it.id == id } ?: AppIcon.DEFAULT
}
