package org.wordpress.android.inappupdate

import android.app.Activity

interface IInAppUpdateManager {
    fun checkForAppUpdate(activity: Activity, listener: InAppUpdateListener)
    fun completeAppUpdate()
    fun cancelAppUpdate(updateType: Int)
    fun onUserAcceptedAppUpdate(updateType: Int)

    companion object {
        const val APP_UPDATE_IMMEDIATE_REQUEST_CODE = 1001
        const val APP_UPDATE_FLEXIBLE_REQUEST_CODE = 1002
    }
}
