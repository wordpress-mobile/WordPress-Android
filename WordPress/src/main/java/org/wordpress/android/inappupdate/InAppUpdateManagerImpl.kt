package org.wordpress.android.inappupdate

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.InstallStatus.CANCELED
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADED
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADING
import com.google.android.play.core.install.model.InstallStatus.FAILED
import com.google.android.play.core.install.model.InstallStatus.INSTALLED
import com.google.android.play.core.install.model.InstallStatus.INSTALLING
import com.google.android.play.core.install.model.InstallStatus.PENDING
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UNKNOWN
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_NOT_AVAILABLE
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_FLEXIBLE_REQUEST_CODE
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_IMMEDIATE_REQUEST_CODE

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.RemoteConfigWrapper
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class InAppUpdateManagerImpl(
    @ApplicationContext private val applicationContext: Context,
    private val appUpdateManager: AppUpdateManager,
    private val remoteConfigWrapper: RemoteConfigWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val inAppUpdateAnalyticsTracker: InAppUpdateAnalyticsTracker,
    private val currentTimeProvider: () -> Long = {System.currentTimeMillis()}
): IInAppUpdateManager {
    private var updateListener: IInAppUpdateListener? = null

    override fun checkForAppUpdate(activity: Activity, listener: IInAppUpdateListener) {
        Log.e(TAG, "checkForAppUpdate() entered")

        updateListener = listener

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->

            Log.e(TAG, "checkForAppUpdate(): success")

            val updateAvailability = appUpdateInfo.updateAvailability()

            // If the update is downloaded but not installed,
            // notify the user to complete the update.
            if (appUpdateInfo.installStatus() == DOWNLOADED) {
                Log.e(TAG, "checkForAppUpdate(): appUpdateInfo.installStatus() == DOWNLOADED")
                listener.onAppUpdateDownloaded()
                Log.e(TAG, "checkForAppUpdate(): listener.onAppUpdateDownloaded() called")
                return@addOnSuccessListener
            }

            when (updateAvailability) {
                UPDATE_NOT_AVAILABLE -> {
                    Log.e(TAG, "checkForAppUpdate(): no update available")
                    return@addOnSuccessListener
                }

                UPDATE_AVAILABLE -> {
                    Log.e(TAG, "checkForAppUpdate(): update available")

                    // reset saved values if new update is available
                    val updateVersion = getAvailableUpdateAppVersion(appUpdateInfo)
                    Log.e(TAG, "checkForAppUpdate(): updateVersion = $updateVersion")
                    if (updateVersion != getLastUpdateRequestedVersion()) {
                        resetLastUpdateRequestInfo()
                    }

                    if (isImmediateUpdateNecessary()) {
                        Log.e(TAG, "checkForAppUpdate(): isImmediateUpdateNecessary == true")
                        requestImmediateUpdate(appUpdateInfo, activity)
                    } else {
                        Log.e(TAG, "checkForAppUpdate(): isImmediateUpdateNecessary == false")
                        if (shouldRequestFlexibleUpdate()) {
                            requestFlexibleUpdate(appUpdateInfo, activity)
                        }
                    }
                }

                DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.e(TAG, "checkForAppUpdate(): UPDATE_IN_PROGRESS")
                    if (isImmediateUpdateInProgress(appUpdateInfo)) {
                        Log.e(TAG, "checkForAppUpdate(): isImmediateUpdateNecessary == true")
                        requestImmediateUpdate(appUpdateInfo, activity)
                    } else {
                        Log.e(TAG, "checkForAppUpdate(): isImmediateUpdateNecessary == false")
                        requestFlexibleUpdate(appUpdateInfo, activity)
                    }
                }

                UNKNOWN -> {
                    Log.e(TAG, "checkForAppUpdate(): UNKNOWN")
                    return@addOnSuccessListener
                }
            }
        }

        appUpdateInfoTask.addOnFailureListener { exception ->
            Log.e(TAG, "checkForAppUpdate():, checking update, failure")
            Log.e(TAG, exception.message.toString())
        }
    }

    override fun completeAppUpdate() {
        Log.e(TAG, "completeAppUpdate(): entered")
        appUpdateManager.completeUpdate()
    }

    override fun cancelAppUpdate() {
        Log.e(TAG, "cancelAppUpdate(): entered")
        appUpdateManager.unregisterListener(installStateListener)
        inAppUpdateAnalyticsTracker.trackUpdateDismissed()
    }

    override fun onUserAcceptedAppUpdate(updateType: Int) {
        Log.e(TAG, "onUserAcceptedAppUpdate(): entered")
        inAppUpdateAnalyticsTracker.trackUpdateAccepted(updateType)
    }

    private fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e(TAG, "requestImmediateUpdate(): entered")
        updateListener?.onAppUpdateStarted(AppUpdateType.IMMEDIATE)
        requestUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo, activity)
    }

    private fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e(TAG, "requestFlexibleUpdate(): entered")
        appUpdateManager.registerListener(installStateListener)
        updateListener?.onAppUpdateStarted(AppUpdateType.FLEXIBLE)
        requestUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo, activity)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun requestUpdate(updateType: Int, appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e(TAG, "requestUpdate(): entered with updateType = $updateType")
        val requestCode = if (updateType == AppUpdateType.IMMEDIATE) {
            APP_UPDATE_IMMEDIATE_REQUEST_CODE
        } else {
            saveLastUpdateRequestInfo(appUpdateInfo)
            APP_UPDATE_FLEXIBLE_REQUEST_CODE
        }
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(updateType).build(),
                requestCode
            )
            inAppUpdateAnalyticsTracker.trackUpdateShown(updateType)
        } catch (e: Exception) {
            Log.e(TAG, "requestUpdate for type: $updateType, exception occurred")
            Log.e(TAG, e.message.toString())
            appUpdateManager.unregisterListener(installStateListener)
        }
    }

    private val installStateListener = object : InstallStateUpdatedListener {
        @SuppressLint("SwitchIntDef")
        override fun onStateUpdate(state: InstallState) {
            when (state.installStatus()) {
                DOWNLOADED -> {
                    Log.e(TAG, "installStateListener DOWNLOADED")
                    updateListener?.onAppUpdateDownloaded()
                }
                INSTALLED -> {
                    Log.e(TAG, "installStateListener INSTALLED")
                    updateListener?.onAppUpdateInstalled()
                    appUpdateManager.unregisterListener(this) // 'this' refers to the listener object
                }
                CANCELED -> {
                    Log.e(TAG, "installStateListener CANCELED")
                    updateListener?.onAppUpdateCancelled()
                    appUpdateManager.unregisterListener(this)
                }
                FAILED -> {
                    Log.e(TAG, "installStateListener FAILED")
                    updateListener?.onAppUpdateFailed()
                    appUpdateManager.unregisterListener(this)
                }
                PENDING -> {
                    Log.e(TAG, "installStateListener PENDING")
                    updateListener?.onAppUpdatePending()
                }
                DOWNLOADING -> {
                    Log.e(TAG, "installStateListener DOWNLOADING")
                }
                INSTALLING -> {
                    Log.e(TAG, "installStateListener INSTALLING")
                }
                InstallStatus.UNKNOWN -> {
                    Log.e(TAG, "installStateListener UNKNOWN")
                }
            }
        }
    }

    private fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        Log.e(TAG, "isImmediateUpdateInProgress(): entered, " +
                "appUpdateInfo.updateAvailability() = ${appUpdateInfo.updateAvailability()}," +
                "appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) " +
                "= ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}" +
                "isImmediateUpdateNecessary = ${isImmediateUpdateNecessary()}")
        val result = appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isImmediateUpdateNecessary()

        Log.e(TAG, "isImmediateUpdateInProgress(): result = $result")

        return result
    }

    private fun saveLastUpdateRequestInfo(appUpdateInfo: AppUpdateInfo) {
        Log.e(TAG, "setLastUpdateRequestedTime(): entered")
        val currentTime = currentTimeProvider.invoke()
        Log.e(TAG, "setLastUpdateRequestedTime(): currentTime = $currentTime")
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, getAvailableUpdateAppVersion(appUpdateInfo))
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, currentTime)
            apply()
        }
    }

    private fun resetLastUpdateRequestInfo() {
        Log.e(TAG, "resetLastUpdateRequestedTime(): entered")
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, -1)
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, -1L)
            apply()
        }
    }

    private fun getLastUpdateRequestedVersion(): Int {
        val defaultValue = -1
        val result = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, defaultValue)

        Log.e(TAG, "getLastUpdateRequestedVersion(): result = $result")

        return result
    }

    private fun getLastUpdateRequestedTime(): Long {
        val defaultValue = -1L
        val result = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APP_UPDATE_CHECK_TIME, defaultValue)

        Log.e(TAG, "getLastUpdateRequestedTime(): result = $result")

        return result
    }

    private fun shouldRequestFlexibleUpdate(): Boolean {
        Log.e(TAG, "shouldRequestFlexibleUpdate(): entered")
        val result = currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= FLEXIBLE_UPDATES_INTERVAL_IN_MILLIS
        Log.e(TAG, "shouldRequestFlexibleUpdate(): result = $result")
        return result
    }


    /**
     * Retrieves the current version code of the application.
     *
     * This version code is obtained from the application's build configuration.
     *
     * @return The current application version code.
     */
    private fun getCurrentAppVersion() = buildConfigWrapper.getAppVersionCode()

    /**
     * Retrieves the version code of the last known update that requires blocking.
     *
     * This value is sourced from a remote configuration that specifies the
     * version of the application that requires immediate blocking updates.
     *
     * @return The version code of the last blocking app update.
     */
    private fun getLastBlockingAppVersion(): Int = remoteConfigWrapper.getInAppUpdateBlockingVersion()


    /**
     * Extracts the available version code for the app update from the given update information.
     *
     * The available version code indicates the most recent update version that's available
     * and ready to be installed on the user's device.
     *
     * @param appUpdateInfo The update information object that contains version details.
     * @return The available version code for the app update.
     */
    private fun getAvailableUpdateAppVersion(appUpdateInfo: AppUpdateInfo) = appUpdateInfo.availableVersionCode()

    /**
     * Checks if an immediate app update is required based on the current app version
     * and the last known blocking version.
     *
     * @return `true` if the current app version is lower than the last blocking app version, otherwise `false`.
     */
    private fun isImmediateUpdateNecessary(): Boolean {
        Log.e(TAG, "isImmediateUpdateNecessary(): entered")
        val currentVersion = getCurrentAppVersion()
        val getLastBlockingAppVersion = getLastBlockingAppVersion()
        Log.e(TAG, "isImmediateUpdateNecessary() called")
        Log.e(TAG, "currentVersion = $currentVersion, lastBlockingVersion = $getLastBlockingAppVersion")
        val result = getCurrentAppVersion() < getLastBlockingAppVersion()
        Log.e(TAG, "isImmediateUpdateNecessary(): result = $result")
        return result
    }

    companion object {
        private const val TAG = "AppUpdateChecker"

        private const val PREF_NAME = "in_app_update_prefs"
        private const val KEY_LAST_APP_UPDATE_CHECK_VERSION = "last_app_update_check_version"
        private const val KEY_LAST_APP_UPDATE_CHECK_TIME = "last_app_update_check_time"
        private const val FLEXIBLE_UPDATES_INTERVAL_IN_MILLIS: Long = 1000 * 60 * 60 * 24 * 5 // 5 days
    }
}
