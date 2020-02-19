package org.wordpress.android.util

import android.content.Context
import android.preference.PreferenceManager
import android.util.Base64
import io.sentry.android.core.SentryAndroid
import io.sentry.core.Breadcrumb
import io.sentry.core.Sentry
import io.sentry.core.SentryOptions.BeforeSendCallback
import org.json.JSONObject
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.util.encryption.EncryptionUtils
import org.wordpress.android.util.encryption.LogEncryptionTestingActions
import org.wordpress.android.util.encryption.LogsFileProvider

class CrashLoggingUtils {
    private val logEncryptionActions = LogEncryptionTestingActions()
    private val logsFileProvider = LogsFileProvider()

    fun shouldEnableCrashLogging(context: Context): Boolean {
        if (PackageUtils.isDebugBuild()) {
            return false
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val hasUserOptedOut = !prefs.getBoolean(context.getString(R.string.pref_key_send_crash), true)
        return !hasUserOptedOut
    }

    fun enableCrashLogging(context: Context) {
        SentryAndroid.init(context.applicationContext) {
            it.dsn = BuildConfig.SENTRY_DSN
            it.beforeSend = BeforeSendCallback { event, hint ->
                val decodedPublicKey = Base64.decode(BuildConfig.ENCRYPTION_PUBLIC_KEY, Base64.DEFAULT)
                val encryptedLogsSerialized = EncryptionUtils.generateJSONEncryptedLogs(
                        decodedPublicKey,
                        logsFileProvider.getMostRecentLogs(context)
                )
                val encryptedLogsJson = JSONObject(encryptedLogsSerialized)
                logEncryptionActions.sendEncryptedLogsFile(encryptedLogsJson)
                event.setExtra("LogsID", EncryptionUtils.getLogsUUID(encryptedLogsJson))
                return@BeforeSendCallback event
            }
        }
        Sentry.setTag("version", BuildConfig.VERSION_NAME)
    }

    companion object {
        @JvmStatic fun startCrashLogging(context: android.content.Context) {
            val obj = CrashLoggingUtils()

            if (obj.shouldEnableCrashLogging(context)) {
                obj.enableCrashLogging(context)
            }
        }

        @JvmStatic fun stopCrashLogging() {
            Sentry.close()
        }

        @JvmStatic fun log(message: String?) {
            if (message == null) {
                return
            }

            Sentry.addBreadcrumb(Breadcrumb(message))
        }

        @JvmStatic fun log(exception: Throwable) {
            Sentry.captureException(exception)
        }

        @JvmStatic fun logException(tr: Throwable, tag: AppLog.T) {
            log(tr)
        }

        @JvmStatic fun logException(tr: Throwable, tag: AppLog.T, message: String?) {
            log(message)
            log(tr)
        }
    }
}
