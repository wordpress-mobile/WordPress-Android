package org.wordpress.android

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.updatePriority

import javax.inject.Singleton

private const val MAXIMUM_THRESHOLD_FOR_FLEXIBLE_UPDATES: Int = 60

private const val MIN_DURATION_TO_WAIT_FOR_UPDATE_PROMPT: Int = 7

@Singleton
class InAppUpdateManager constructor(private val appUpdateManager: AppUpdateManager) {

    fun registerUpdateListener(installStateUpdatedListener: InstallStateUpdatedListener) {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    fun checkForAppUpdate(activity: Activity) {
        if (!shouldRequestUpdate())
            return
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (isImmediateUpdateNecessary(appUpdateInfo)) {
                requestImmediateUpdate(appUpdateInfo, activity)
            } else if (isFlexibleUpdateNecessary(appUpdateInfo)) {
                requestFlexibleUpdate(appUpdateInfo, activity)
            }
        }
    }

    private fun isImmediateUpdateNecessary(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isUpdatePriorityHigh(appUpdateInfo)) || isClientVersionOlderThanThreshold(
            appUpdateInfo
        )
    }

    private fun isUpdatePriorityHigh(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updatePriority > 4
    }

    private fun isClientVersionOlderThanThreshold(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.clientVersionStalenessDays()
            ?: -1) >= MAXIMUM_THRESHOLD_FOR_FLEXIBLE_UPDATES
    }

    private fun isFlexibleUpdateNecessary(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !isUpdatePriorityHigh(appUpdateInfo)
    }

    fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.IMMEDIATE,
            activity,
            APP_UPDATE_IMMEDIATE_REQUEST_CODE
        )
    }

    fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            activity,
            APP_UPDATE_FLEXIBLE_REQUEST_CODE
        )
    }

    private fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isUpdatePriorityHigh(appUpdateInfo)
    }

    private fun isFlexibleUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !isUpdatePriorityHigh(appUpdateInfo)
    }

    private fun setLastUpdateRequestedTime() {
        //todo: add logic to save the time when the update request was made
    }

    private fun shouldRequestUpdate(): Boolean {
        //todo: add logic to check the time since last update request
    }


    companion object {
        const val APP_UPDATE_IMMEDIATE_REQUEST_CODE = 1001
        const val APP_UPDATE_FLEXIBLE_REQUEST_CODE = 1002
    }
}
