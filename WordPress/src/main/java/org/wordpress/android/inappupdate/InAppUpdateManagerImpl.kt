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
        Log.d(TAG, "Checking for app update")
        updateListener = listener

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            handleUpdateInfoSuccess(appUpdateInfo, activity)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to check for update: ${exception.message}")
        }
    }

    override fun completeAppUpdate() {
        Log.d(TAG, "completeAppUpdate(): entered")
        appUpdateManager.completeUpdate()
    }

    override fun cancelAppUpdate(updateType: Int) {
        Log.d(TAG, "cancelAppUpdate(): entered")
        appUpdateManager.unregisterListener(installStateListener)
        inAppUpdateAnalyticsTracker.trackUpdateDismissed(updateType)
    }

    override fun onUserAcceptedAppUpdate(updateType: Int) {
        Log.d(TAG, "onUserAcceptedAppUpdate(): entered")
        inAppUpdateAnalyticsTracker.trackUpdateAccepted(updateType)
    }

    private fun handleUpdateInfoSuccess(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        when (appUpdateInfo.updateAvailability()) {
            UPDATE_NOT_AVAILABLE -> {
                Log.d(TAG, "No update available")
            }
            UPDATE_AVAILABLE -> {
                handleUpdateAvailable(appUpdateInfo, activity)
            }
            DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                handleUpdateInProgress(appUpdateInfo, activity)
            }
            else -> {
                Log.w(TAG, "Unknown update availability")
            }
        }
    }

    private fun handleUpdateAvailable(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        if (appUpdateInfo.installStatus() == DOWNLOADED) {
            Log.d(TAG, "Update downloaded, notifying listener")
            updateListener?.onAppUpdateDownloaded()
            return
        }

        val updateVersion = getAvailableUpdateAppVersion(appUpdateInfo)
        if (updateVersion != getLastUpdateRequestedVersion()) {
            resetLastUpdateRequestInfo()
        }

        if (isImmediateUpdateNecessary()) {
            if (shouldRequestImmediateUpdate()) {
                requestImmediateUpdate(appUpdateInfo, activity)
            }
        } else if (shouldRequestFlexibleUpdate()) {
            requestFlexibleUpdate(appUpdateInfo, activity)
        }
    }

    private fun handleUpdateInProgress(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        if (isImmediateUpdateInProgress(appUpdateInfo)) {
            requestImmediateUpdate(appUpdateInfo, activity)
        } else {
            requestFlexibleUpdate(appUpdateInfo, activity)
        }
    }

    private fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.d(TAG, "requestImmediateUpdate(): entered")
        updateListener?.onAppUpdateStarted(AppUpdateType.IMMEDIATE)
        requestUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo, activity)
    }

    private fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.d(TAG, "requestFlexibleUpdate(): entered")
        appUpdateManager.registerListener(installStateListener)
        updateListener?.onAppUpdateStarted(AppUpdateType.FLEXIBLE)
        requestUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo, activity)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun requestUpdate(updateType: Int, appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.d(TAG, "requestUpdate(): entered with updateType = $updateType")
        saveLastUpdateRequestInfo(appUpdateInfo)
        val requestCode = if (updateType == AppUpdateType.IMMEDIATE) {
            APP_UPDATE_IMMEDIATE_REQUEST_CODE
        } else {
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
                    Log.d(TAG, "installStateListener DOWNLOADED")
                    updateListener?.onAppUpdateDownloaded()
                }
                INSTALLED -> {
                    Log.d(TAG, "installStateListener INSTALLED")
                    updateListener?.onAppUpdateInstalled()
                    appUpdateManager.unregisterListener(this) // 'this' refers to the listener object
                }
                CANCELED -> {
                    Log.d(TAG, "installStateListener CANCELED")
                    updateListener?.onAppUpdateCancelled()
                    appUpdateManager.unregisterListener(this)
                }
                FAILED -> {
                    Log.d(TAG, "installStateListener FAILED")
                    updateListener?.onAppUpdateFailed()
                    appUpdateManager.unregisterListener(this)
                }
                PENDING -> {
                    Log.d(TAG, "installStateListener PENDING")
                    updateListener?.onAppUpdatePending()
                }
                DOWNLOADING -> {
                    Log.d(TAG, "installStateListener DOWNLOADING")
                }
                INSTALLING -> {
                    Log.d(TAG, "installStateListener INSTALLING")
                }
                InstallStatus.UNKNOWN -> {
                    Log.d(TAG, "installStateListener UNKNOWN")
                }
            }
        }
    }

    private fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        Log.d(TAG, "isImmediateUpdateInProgress(): entered, " +
                "appUpdateInfo.updateAvailability() = ${appUpdateInfo.updateAvailability()}," +
                "appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) " +
                "= ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}" +
                "isImmediateUpdateNecessary = ${isImmediateUpdateNecessary()}")
        val result = appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isImmediateUpdateNecessary()

        Log.d(TAG, "isImmediateUpdateInProgress(): result = $result")

        return result
    }

    private fun saveLastUpdateRequestInfo(appUpdateInfo: AppUpdateInfo) {
        Log.d(TAG, "setLastUpdateRequestedTime(): entered")
        val currentTime = currentTimeProvider.invoke()
        Log.d(TAG, "setLastUpdateRequestedTime(): currentTime = $currentTime")
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, getAvailableUpdateAppVersion(appUpdateInfo))
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, currentTime)
            apply()
        }
    }

    private fun resetLastUpdateRequestInfo() {
        Log.d(TAG, "resetLastUpdateRequestedTime(): entered")
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
        Log.d(TAG, "getLastUpdateRequestedVersion(): result = $result")
        return result
    }

    private fun getLastUpdateRequestedTime(): Long {
        val defaultValue = -1L
        val result = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APP_UPDATE_CHECK_TIME, defaultValue)
        Log.d(TAG, "getLastUpdateRequestedTime(): result = $result")
        return result
    }

    private fun shouldRequestFlexibleUpdate(): Boolean {
        Log.d(TAG, "shouldRequestFlexibleUpdate(): entered")
        val result = currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= getFlexibleUpdateIntervalInMillis()
        Log.d(TAG, "shouldRequestFlexibleUpdate(): result = $result")
        return result
    }

    private fun shouldRequestImmediateUpdate(): Boolean {
        Log.d(TAG, "shouldRequestImmediateUpdate(): entered")
        val result = currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS
        Log.d(TAG, "shouldRequestFlexibleUpdate(): result = $result")
        return result
    }

    @Suppress("MagicNumber")
    private fun getFlexibleUpdateIntervalInMillis(): Long {
        Log.d(TAG, "getFlexibleUpdateIntervalInMillis(): entered")
        val result = 1000 * 60 * 60 * 24 * remoteConfigWrapper.getInAppUpdateFlexibleIntervalInDays()
        Log.d(TAG, "getFlexibleUpdateIntervalInMillis(): result = $result")
        return result.toLong()
    }

    private fun getCurrentAppVersion() = buildConfigWrapper.getAppVersionCode()

    private fun getLastBlockingAppVersion(): Int = remoteConfigWrapper.getInAppUpdateBlockingVersion()

    private fun getAvailableUpdateAppVersion(appUpdateInfo: AppUpdateInfo) = appUpdateInfo.availableVersionCode()

    private fun isImmediateUpdateNecessary(): Boolean {
        Log.d(TAG, "isImmediateUpdateNecessary(): entered")
        val currentVersion = getCurrentAppVersion()
        val getLastBlockingAppVersion = getLastBlockingAppVersion()
        Log.d(TAG, "isImmediateUpdateNecessary() called")
        Log.d(TAG, "currentVersion = $currentVersion, lastBlockingVersion = $getLastBlockingAppVersion")
        val result = getCurrentAppVersion() < getLastBlockingAppVersion()
        Log.d(TAG, "isImmediateUpdateNecessary(): result = $result")
        return result
    }

    companion object {
        private const val TAG = "AppUpdateChecker"
        private const val IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS = 1000 * 60 * 5 // 5 minutes
        private const val PREF_NAME = "in_app_update_prefs"
        private const val KEY_LAST_APP_UPDATE_CHECK_VERSION = "last_app_update_check_version"
        private const val KEY_LAST_APP_UPDATE_CHECK_TIME = "last_app_update_check_time"
    }
}
