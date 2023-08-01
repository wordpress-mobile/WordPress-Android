package org.wordpress.android.util

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import org.wordpress.android.R

object MissingSplitsUtils {
    // Dialog texts are hardcoded because we can't be sure if resources will be available when there are missing splits.
    const val DIALOG_TITLE = "Installation error"
    const val DIALOG_MESSAGE =
        "The app Jetpack is missing required components and must be reinstalled from the Google Play Store."
    const val DIALOG_BUTTON = "CLOSE"

    /**
     * R.drawable.splash_icon is the first drawable the app attempts to access. If a NotFoundException occurs while
     * trying to access it, it indicates that there are missing splits, possibly due to sideloading the app.
     */
    fun isMissingSplits(context: Context) = try {
        context.resources.getValue(R.drawable.bg_jetpack_login_splash, TypedValue(), true)
        true
    } catch (e: Resources.NotFoundException) {
        false
    }
}
