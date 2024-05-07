package org.wordpress.android

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.updatePriority
import org.wordpress.android.util.config.RemoteConfigWrapper
import javax.inject.Inject

import javax.inject.Singleton

private const val MAXIMUM_THRESHOLD_FOR_FLEXIBLE_UPDATES: Int = 60

private const val UPDATE_PRIORITY_DEFAULT: Int = 4

@Singleton
@Suppress("TooManyFunctions")
class InAppUpdateManager @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    private val remoteConfigWrapper: RemoteConfigWrapper
) {
    fun registerUpdateListener(installStateUpdatedListener: InstallStateUpdatedListener) {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    fun getInAppUpdateManager(): Task<AppUpdateInfo> {
        return appUpdateManager.appUpdateInfo
    }

    fun checkForAppUpdate(activity: Activity) {
        Log.e("AppUpdateChecker", "checkPlayStoreUpdate called")
        if (!shouldRequestUpdate())
            return
        Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update")
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Log.e("AppUpdateChecker", appUpdateInfo.toString())
            Log.e("AppUpdateChecker", appUpdateInfo.updateAvailability().toString())
            Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, success")
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, no update available")
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update available")
                if (isImmediateUpdateNecessary(appUpdateInfo)) {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, immediate update")
                    requestImmediateUpdate(appUpdateInfo, activity)
                } else {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, flexible update")
                    requestFlexibleUpdate(appUpdateInfo, activity)
                }
            } else if (
                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update in progress")
                if (isImmediateUpdateInProgress(appUpdateInfo)) {
                    Log.e(
                        "AppUpdateChecker",
                        "checkPlayStoreUpdate called, checcking update, immediate update in progress"
                    )
                    requestImmediateUpdate(appUpdateInfo, activity)
                } else if (isFlexibleUpdateInProgress(appUpdateInfo)) {
                    Log.e(
                        "AppUpdateChecker",
                        "checkPlayStoreUpdate called, checcking update, flexible update in progress"
                    )
                    requestFlexibleUpdate(appUpdateInfo, activity)
                }
            } else {
                Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update available")
                return@addOnSuccessListener
            }
        }

        appUpdateInfoTask.addOnFailureListener { exception ->
            Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, failure")
            Log.e("AppUpdateChecker", exception.message.toString())
        }
    }

    private fun isImmediateUpdateNecessary(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isUpdatePriorityHigh(appUpdateInfo)) || isClientVersionOlderThanThreshold(
            appUpdateInfo
        )
    }

    private fun isUpdatePriorityHigh(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updatePriority > UPDATE_PRIORITY_DEFAULT
    }

    private fun isClientVersionOlderThanThreshold(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.clientVersionStalenessDays()
            ?: -1) >= MAXIMUM_THRESHOLD_FOR_FLEXIBLE_UPDATES
    }

    @Suppress("TooGenericExceptionCaught")
    fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e("AppUpdateChecker", "requestImmediateUpdate called")
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                APP_UPDATE_IMMEDIATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "requestImmediateUpdate called, exception")
            Log.e("AppUpdateChecker", e.message.toString())
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e("AppUpdateChecker", "requestFlexibleUpdate called")
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                APP_UPDATE_IMMEDIATE_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "requestFlexibleUpdate called, exception")
            Log.e("AppUpdateChecker", e.message.toString())
        }
    }

    fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isUpdatePriorityHigh(appUpdateInfo)
    }

    fun isFlexibleUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !isUpdatePriorityHigh(appUpdateInfo)
    }

    @Suppress("unused")
    private fun setLastUpdateRequestedTime() {
        // todo: add logic to save the time when the update request was made
    }

    @Suppress("unused","FunctionOnlyReturningConstant")
    private fun shouldRequestUpdate(): Boolean {
        // todo: add logic to check the time since last update request
        return true
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun unregisterListener(installStateUpdatedListener: InstallStateUpdatedListener) {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    companion object {
        const val APP_UPDATE_IMMEDIATE_REQUEST_CODE = 1001
        const val APP_UPDATE_FLEXIBLE_REQUEST_CODE = 1002
    }
}
