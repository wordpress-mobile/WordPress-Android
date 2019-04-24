package org.wordpress.android.util

import android.preference.PreferenceManager
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.BreadcrumbBuilder
import org.wordpress.android.BuildConfig
import org.wordpress.android.R

class CrashLoggingUtils {
    fun shouldEnableCrashLogging(context: android.content.Context): Boolean {
        if (PackageUtils.isDebugBuild()) {
            return false
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val hasUserOptedOut = !prefs.getBoolean(context.getString(R.string.pref_key_send_crash), true)
        return !hasUserOptedOut
    }

    fun enableCrashLogging(context: android.content.Context) {
        Sentry.init(BuildConfig.SENTRY_DSN, AndroidSentryClientFactory(context))
        Sentry.getContext().addTag("version", BuildConfig.VERSION_NAME)
    }

    companion object {
        @JvmStatic fun startCrashLogging(context: android.content.Context) {
            val obj = CrashLoggingUtils()

            if (obj.shouldEnableCrashLogging(context)) {
                obj.enableCrashLogging(context)
            }
        }

        @JvmStatic fun stopCrashLogging() {
            Sentry.clearContext()
            Sentry.close()
        }

        @JvmStatic fun log(message: String?) {
            if (message == null) {
                return
            }

            Sentry.getContext().recordBreadcrumb(
                    BreadcrumbBuilder().setMessage(message).build()
            )
        }

        @JvmStatic fun log(exception: Throwable) {
            Sentry.capture(exception)
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
