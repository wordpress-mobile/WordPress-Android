package org.wordpress.android

import android.app.Application
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import javax.inject.Singleton

private const val MAXIMUM_THRESHHOLD_FOR_FLEXIBLE_UPDATES: Int = 60

@Singleton
class AppUpdateChecker {

    private lateinit var appUpdateManager: AppUpdateManager

    fun init(application: Application) {
        AppUpdateManagerFactory.create(application)
    }

    private fun checkPlayStoreUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        //todo: add a log here
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                requestImmediateUpdate(appUpdateInfo)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && (appUpdateInfo.clientVersionStalenessDays()
                    ?: -1) >= MAXIMUM_THRESHHOLD_FOR_FLEXIBLE_UPDATES
            ) {
                requestImmediateUpdate(appUpdateInfo)
            } else if (
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                requestFlexibleUpdate()
            } else {
                //todo: check
            }
        }
    }

    private fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        //todo: show a dialog to the user to confirm the update
    }

    private fun requestFlexibleUpdate() {
        //todo: show a dialog to the user to confirm the update
    }
}
