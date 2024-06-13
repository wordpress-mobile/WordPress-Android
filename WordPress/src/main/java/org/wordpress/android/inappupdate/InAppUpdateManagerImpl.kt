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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_FLEXIBLE_REQUEST_CODE
import org.wordpress.android.inappupdate.IInAppUpdateManager.Companion.APP_UPDATE_IMMEDIATE_REQUEST_CODE

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.RemoteConfigWrapper
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class InAppUpdateManagerImpl(
    @ApplicationContext private val applicationContext: Context,
    private val coroutineScope: CoroutineScope,
    private val appUpdateManager: AppUpdateManager,
    private val remoteConfigWrapper: RemoteConfigWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val inAppUpdateAnalyticsTracker: InAppUpdateAnalyticsTracker,
    private val currentTimeProvider: () -> Long = {System.currentTimeMillis()}
): IInAppUpdateManager {
    private var updateListener: InAppUpdateListener? = null

    override fun checkForAppUpdate(activity: Activity, listener: InAppUpdateListener) {
        updateListener = listener
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            handleUpdateInfoSuccess(appUpdateInfo, activity)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to check for update: ${exception.message}")
        }
    }

    override fun completeAppUpdate() {
        coroutineScope.launch(Dispatchers.Main) {
            // Track the app restart to complete update
            inAppUpdateAnalyticsTracker.trackAppRestartToCompleteUpdate()

            // Delay so the event above can be logged
            delay(RESTART_DELAY_IN_MILLIS)

            // Complete the update
            appUpdateManager.completeUpdate()
        }
    }

    override fun cancelAppUpdate(updateType: Int) {
        appUpdateManager.unregisterListener(installStateListener)
        inAppUpdateAnalyticsTracker.trackUpdateDismissed(updateType)
    }

    override fun onUserAcceptedAppUpdate(updateType: Int) {
        inAppUpdateAnalyticsTracker.trackUpdateAccepted(updateType)
    }

    private fun handleUpdateInfoSuccess(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        when (appUpdateInfo.updateAvailability()) {
            UPDATE_NOT_AVAILABLE -> {
               /* do nothing */
            }
            UPDATE_AVAILABLE -> {
                handleUpdateAvailable(appUpdateInfo, activity)
            }
            DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                handleUpdateInProgress(appUpdateInfo, activity)
            }
            else -> { /* do nothing */ }
        }
    }

    private fun handleUpdateAvailable(appUpdateInfo: AppUpdateInfo, activity: Activity) {
        if (appUpdateInfo.installStatus() == DOWNLOADED) {
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
                    updateListener?.onAppUpdateDownloaded()
                }
                INSTALLED -> {
                    updateListener?.onAppUpdateInstalled()
                    appUpdateManager.unregisterListener(this) // 'this' refers to the listener object
                }
                CANCELED -> {
                    updateListener?.onAppUpdateCancelled()
                    appUpdateManager.unregisterListener(this)
                }
                FAILED -> {
                    updateListener?.onAppUpdateFailed()
                    appUpdateManager.unregisterListener(this)
                }
                PENDING -> {
                    updateListener?.onAppUpdatePending()
                }
                DOWNLOADING, INSTALLING, InstallStatus.UNKNOWN -> {
                   /* do nothing */
                }
            }
        }
    }

    private fun isImmediateUpdateInProgress(appUpdateInfo: AppUpdateInfo) =
        appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                && isImmediateUpdateNecessary()

    private fun saveLastUpdateRequestInfo(appUpdateInfo: AppUpdateInfo) {
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, getAvailableUpdateAppVersion(appUpdateInfo))
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, currentTimeProvider.invoke())
            apply()
        }
    }

    private fun resetLastUpdateRequestInfo() {
        val sharedPref = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, -1)
            putLong(KEY_LAST_APP_UPDATE_CHECK_TIME, -1L)
            apply()
        }
    }

    private fun getLastUpdateRequestedVersion() =
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_APP_UPDATE_CHECK_VERSION, -1)

    private fun getLastUpdateRequestedTime() =
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APP_UPDATE_CHECK_TIME, -1L)

    private fun shouldRequestFlexibleUpdate() =
        currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= getFlexibleUpdateIntervalInMillis()

    private fun shouldRequestImmediateUpdate() =
        currentTimeProvider.invoke() - getLastUpdateRequestedTime() >= IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS

    @Suppress("MagicNumber")
    private fun getFlexibleUpdateIntervalInMillis(): Long =
        1000 * 60 * 60 * 24 * remoteConfigWrapper.getInAppUpdateFlexibleIntervalInDays().toLong()

    private fun getCurrentAppVersion() = buildConfigWrapper.getAppVersionCode()

    private fun getLastBlockingAppVersion(): Int = remoteConfigWrapper.getInAppUpdateBlockingVersion()

    private fun getAvailableUpdateAppVersion(appUpdateInfo: AppUpdateInfo) = appUpdateInfo.availableVersionCode()

    private fun isImmediateUpdateNecessary() = getCurrentAppVersion() < getLastBlockingAppVersion()

    companion object {
        const val IMMEDIATE_UPDATE_INTERVAL_IN_MILLIS = 1000 * 60 * 5 // 5 minutes
        const val KEY_LAST_APP_UPDATE_CHECK_TIME = "last_app_update_check_time"

        private const val TAG = "AppUpdateChecker"
        private const val PREF_NAME = "in_app_update_prefs"
        private const val KEY_LAST_APP_UPDATE_CHECK_VERSION = "last_app_update_check_version"
        private const val RESTART_DELAY_IN_MILLIS = 500L
    }
}
