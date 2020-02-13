package org.wordpress.android.util

import android.content.Context
import android.preference.PreferenceManager
import io.sentry.android.core.SentryAndroid
import io.sentry.core.Breadcrumb
import io.sentry.core.Sentry
import io.sentry.core.SentryOptions.BeforeSendCallback
import org.wordpress.android.BuildConfig

class CrashLoggingUtils {
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
                // Todo: Update event with additional info (extras)
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
            Sentry.clearBreadcrumbs()
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
