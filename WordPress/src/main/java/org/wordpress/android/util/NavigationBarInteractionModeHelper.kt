package org.wordpress.android.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import javax.inject.Inject

/**
 * Inspired by the System Navigation screen in Android 10's Settings. This functionality is found by going to:
 * Settings -> Accessibility -> System navigation
 */
class NavigationBarInteractionModeHelper @Inject constructor() {
    fun getNavigationMode(context: Context): NavigationMode? {
        val resources = context.resources
        val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        try {
            return NavigationMode.values().find { it.mode == resources.getInteger(resourceId) }
        } catch (exception: NotFoundException) {
            AppLog.e(
                    AppLog.T.UTILS,
                    String.format("getNavigationMode is not working on this device. The resource identifiers for the " +
                            "navigation modes cannot be found. " + exception.message)
            )
        }
        return null
    }

    enum class NavigationMode(val mode: Int) {
        NAV_BAR_MODE_3BUTTON(0), NAV_BAR_MODE_2BUTTON(1), NAV_BAR_MODE_GESTURAL(2),
    }
}
