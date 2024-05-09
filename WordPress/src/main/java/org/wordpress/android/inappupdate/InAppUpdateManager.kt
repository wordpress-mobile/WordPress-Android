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

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.RemoteConfigWrapper
import javax.inject.Singleton


@Singleton
@Suppress("TooManyFunctions")
class InAppUpdateManager(
    @ApplicationContext private val applicationContext: Context,
    private val appUpdateManager: AppUpdateManager,
    private val remoteConfigWrapper: RemoteConfigWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val currentTimeProvider: () -> Long = {System.currentTimeMillis()}
) {

    private var updateListener: IInAppUpdateListener? = null

    fun checkForAppUpdate(activity: Activity, listener: IInAppUpdateListener) {
        Log.e("AppUpdateChecker", "checkPlayStoreUpdate called")

        updateListener = listener

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->

            Log.e("AppUpdateChecker", appUpdateInfo.toString())
            Log.e("AppUpdateChecker", appUpdateInfo.updateAvailability().toString())
            Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checking update, success")

            val updateAvailability = appUpdateInfo.updateAvailability()

            when (updateAvailability) {
                UPDATE_NOT_AVAILABLE -> {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checking update, no update available")
                    return@addOnSuccessListener
                }

                UPDATE_AVAILABLE -> {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update available")
                    if (isImmediateUpdateNecessary()) {
                        Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, immediate update")
                        requestImmediateUpdate(appUpdateInfo, activity)
                    } else {
                        Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, flexible update")
                        if (shouldRequestFlexibleUpdate()) {
                            requestFlexibleUpdate(appUpdateInfo, activity)
                        }
                    }
                }

                DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update in progress")
                    if (isImmediateUpdateInProgress(appUpdateInfo)) {
                        Log.e(
                            "AppUpdateChecker",
                            "checkPlayStoreUpdate called, checcking update, immediate update in progress"
                        )
                        requestImmediateUpdate(appUpdateInfo, activity)
                    } else {
                        Log.e(
                            "AppUpdateChecker",
                            "checkPlayStoreUpdate called, checcking update, flexible update in progress"
                        )
                        requestFlexibleUpdate(appUpdateInfo, activity)
                    }
                }

                UNKNOWN -> {
                    Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, update available")
                    return@addOnSuccessListener
                }
            }
        }

        appUpdateInfoTask.addOnFailureListener { exception ->
            Log.e("AppUpdateChecker", "checkPlayStoreUpdate called, checcking update, failure")
            Log.e("AppUpdateChecker", exception.message.toString())
        }
    }

    fun completeAppUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun cancelAppUpdate() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    fun onUserAcceptedAppUpdate() {
        resetLastUpdateRequestedTime()
    }

    private fun requestImmediateUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        updateListener?.onAppUpdateStarted(AppUpdateType.IMMEDIATE)
        requestUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo, activity)
    }

    private fun requestFlexibleUpdate(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        appUpdateManager.registerListener(installStateListener)
        updateListener?.onAppUpdateStarted(AppUpdateType.FLEXIBLE)
        requestUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo, activity)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun requestUpdate(updateType: Int, appUpdateInfo: AppUpdateInfo, activity: Activity) {
        Log.e("AppUpdateChecker", "requestUpdate called for type: $updateType")
        val requestCode = if (updateType == AppUpdateType.IMMEDIATE) {
            APP_UPDATE_IMMEDIATE_REQUEST_CODE
        } else {
            setLastUpdateRequestedTime()
            APP_UPDATE_FLEXIBLE_REQUEST_CODE
        }
        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(updateType).build(),
                requestCode
            )
        } catch (e: Exception) {
            Log.e("AppUpdateChecker", "requestUpdate for type: $updateType, exception occurred")
            Log.e("AppUpdateChecker", e.message.toString())
            appUpdateManager.unregisterListener(installStateListener)
        }
    }

    private val installStateListener = object : InstallStateUpdatedListener {
        @SuppressLint("SwitchIntDef")
        override fun onStateUpdate(state: InstallState) {
            when (state.installStatus()) {
                DOWNLOADED -> {
                    Log.e("AppUpdateChecker", "installStateListener DOWNLOADED")
                    updateListener?.onAppUpdateDownloaded()
                }
                INSTALLED -> {
                    Log.e("AppUpdateChecker", "installStateListener INSTALLED")
                    updateListener?.onAppUpdateInstalled()
                    appUpdateManager.unregisterListener(this) // 'this' refers to the listener object
                }
                CANCELED -> {
                    Log.e("AppUpdateChecker", "installStateListener CANCELED")
                    updateListener?.onAppUpdateCancelled()
                    appUpdateManager.unregisterListener(this)
                }
                FAILED -> {
                    Log.e("AppUpdateChecker", "installStateListener FAILED")
                    updateListener?.onAppUpdateFailed()
                    appUpdateManager.unregisterListener(this)
                }
                PENDING -> {
                    Log.e("AppUpdateChecker", "installStateListener PENDING")
                    updateListener?.onAppUpdatePending()
                }
                DOWNLOADING -> {
                    Log.e("AppUpdateChecker", "installStateListener DOWNLOADING")
                }
                INSTALLING -> {
                    Log.e("AppUpdateChecker", "installStateListener INSTALLING")
                }
                InstallStatus.UNKNOWN -> {
                    Log.e("AppUpdateChecker", "installStateListener UNKNOWN")
                }
            }
        }
    }

    private fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isImmediateUpdateNecessary()
    }

    private fun isFlexibleUpdateInProgress(appUpdateInfo: AppUpdateInfo): Boolean {
        return appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !isImmediateUpdateNecessary()
    }

    private fun setLastUpdateRequestedTime() {
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, currentTimeProvider.invoke())
            apply()
        }
    }

    private fun resetLastUpdateRequestedTime() {
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, -1L)
            apply()
        }
    }

    private fun getLastUpdateRequestedTime(): Long {
        val defaultValue = -1L
        return applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APP_UPDATE_CHECK_TIME, defaultValue)
    }

    private fun shouldRequestFlexibleUpdate() =
        currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= FLEXIBLE_UPDATES_INTERVAL_IN_MILLIS


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
    private fun getLastBlockingAppVersion(): Int =
        if (buildConfigWrapper.isJetpackApp) {
            remoteConfigWrapper.getJetpackInAppUpdateBlockingVersion()
        } else {
            remoteConfigWrapper.getWordPressInAppUpdateBlockingVersion()
        }

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
    private fun isImmediateUpdateNecessary() = getCurrentAppVersion() < getLastBlockingAppVersion()

    companion object {
        const val APP_UPDATE_IMMEDIATE_REQUEST_CODE = 1001
        const val APP_UPDATE_FLEXIBLE_REQUEST_CODE = 1002

        private const val PREF_NAME = "in_app_update_prefs"
        private const val KEY_LAST_APP_UPDATE_CHECK_TIME = "last_app_update_check_time"
        private const val FLEXIBLE_UPDATES_INTERVAL_IN_MILLIS: Long = 1000 * 60 * 60 * 24 * 5 // 5 days
    }
}
