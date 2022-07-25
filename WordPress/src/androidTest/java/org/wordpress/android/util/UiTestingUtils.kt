package org.wordpress.android.util

import org.wordpress.android.support.WPSupportUtils

object UiTestingUtils {
    /**
     * Workaround to avoid gray overlay.
     * In some cases there's a gray overlay on view pager screens when taking screenshots.
     * This function swipes left and then right as a workaround to clear it.
     * @param resourceID: Int the ID of the viewPager
     */
    @JvmStatic
    fun swipeToAvoidGrayOverlayIgnoringFailures(resourceID: Int) {
        try {
            WPSupportUtils.swipeLeftOnViewPager(resourceID)
            WPSupportUtils.idleFor(1000)
            WPSupportUtils.swipeRightOnViewPager(resourceID)
            WPSupportUtils.idleFor(1000)
        } catch (e: Throwable) {
            // Fail softly
            @Suppress("PrintStackTrace")
            e.printStackTrace()
        }
    }
}
