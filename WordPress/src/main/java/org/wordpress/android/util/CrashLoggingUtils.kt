package org.wordpress.android.util

import android.content.Context
import android.preference.PreferenceManager
import io.sentry.core.Sentry
import org.wordpress.android.BuildConfig
import org.wordpress.android.R

class CrashLoggingUtils {
    companion object {
        @JvmStatic fun shouldEnableCrashLogging(context: android.content.Context): Boolean {
            if (PackageUtils.isDebugBuild()) {
                return false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val hasUserOptedOut = !prefs.getBoolean(context.getString(R.string.pref_key_send_crash), true)
            return !hasUserOptedOut
        }

        // TODO: Update Sentry DSN in mobile-secrets
        @JvmStatic fun startCrashLogging(context: Context) {
            Sentry.init { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.cacheDirPath = context.cacheDir.absolutePath
            }
            Sentry.setTag("version", BuildConfig.VERSION_NAME)
        }

        @JvmStatic fun stopCrashLogging() {
            Sentry.clearBreadcrumbs()
            Sentry.close()
        }

        @JvmStatic fun log(message: String?) {
            if (message == null) {
                return
            }

            Sentry.addBreadcrumb(message)
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
