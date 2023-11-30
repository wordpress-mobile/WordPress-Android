package org.wordpress.android.workers.notification.push

import android.content.Context
import android.text.TextUtils
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.util.AppLog
import java.util.UUID

class GCMRegistrationWorker(
    val appContext: Context,
    val accountStore: AccountStore,
    val zendeskHelper: ZendeskHelper,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            FirebaseMessaging
                .getInstance()
                .token
                .addOnCompleteListener { task: Task<String?> ->
                    if (!task.isSuccessful) {
                        AppLog.e(
                            AppLog.T.NOTIFS,
                            "Fetching FCM registration token failed: ",
                            task.exception
                        )
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    sendRegistrationToken(token)
                }

            Result.success()
        } catch (e: SecurityException) {
            // SecurityException can happen on some devices without Google services (these devices probably strip
            // the AndroidManifest.xml and remove unsupported permissions).
            AppLog.e(AppLog.T.NOTIFS, "Google Play Services unavailable: ", e)
            Result.failure()
        }
    }

    class Factory(
        private val accountStore: AccountStore,
        private val zendeskHelper: ZendeskHelper
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == GCMRegistrationWorker::class.java.name) {
                GCMRegistrationWorker(appContext, accountStore, zendeskHelper, workerParameters)
            } else {
                null
            }
        }
    }

    private fun sendRegistrationToken(gcmToken: String?) {
        if (!TextUtils.isEmpty(gcmToken)) {
            AppLog.i(
                AppLog.T.NOTIFS,
                "Sending GCM token to our remote services: $gcmToken"
            )
            // Register to WordPress.com notifications
            if (accountStore.hasAccessToken()) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
                // Get or create UUID for WP.com notes api
                var uuid = preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, null)
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString()
                    preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_UUID, uuid).apply()
                }
                preferences.edit().putString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, gcmToken).apply()
                NotificationsUtils.registerDeviceForPushNotifications(appContext, gcmToken)
            }
            zendeskHelper.enablePushNotifications()
        } else {
            AppLog.w(AppLog.T.NOTIFS, "Empty GCM token, can't register the id on remote services")
            PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                .remove(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN).apply()
        }
    }
}
