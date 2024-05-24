package org.wordpress.android.inappupdate

import android.app.Activity

class InAppUpdateManagerNoop: IInAppUpdateManager {
    override fun checkForAppUpdate(activity: Activity, listener: InAppUpdateListener) {
        /* Empty implementation */
    }

    override fun completeAppUpdate() {
        /* Empty implementation */
    }

    override fun cancelAppUpdate(updateType: Int) {
        /* Empty implementation */
    }

    override fun onUserAcceptedAppUpdate(updateType: Int) {
        /* Empty implementation */
    }
}
