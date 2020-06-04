package org.wordpress.android.util

import android.content.Context
import android.preference.PreferenceManager
import io.sentry.android.core.SentryAndroid
import io.sentry.core.Sentry
import org.wordpress.android.BuildConfig
import org.wordpress.android.R

private const val EVENT_BUS_MODULE = "org.greenrobot.eventbus"
private const val EVENT_BUS_EXCEPTION = "EventBusException"
private const val EVENT_BUS_INVOKING_SUBSCRIBER_FAILED_ERROR = "Invoking subscriber failed"

class CrashLoggingUtils {
    companion object {
        @JvmStatic fun shouldEnableCrashLogging(context: Context): Boolean {
            if (PackageUtils.isDebugBuild()) {
                return false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val hasUserOptedOut = !prefs.getBoolean(context.getString(R.string.pref_key_send_crash), true)
            return !hasUserOptedOut
        }

        @JvmStatic fun startCrashLogging(context: Context) {
            SentryAndroid.init(context) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.cacheDirPath = context.cacheDir.absolutePath
                options.isEnableSessionTracking = true // Release Health tracking
                options.setBeforeSend { event, _ ->
                    if (event.exceptions.size > 1) {
                        event.exceptions.lastOrNull()?.let { lastException ->
                            // Remove the "Invoking subscriber failed" exception so that the main error will show up
                            // in Sentry. This error only means that an exception occurred during an EventBus event and
                            // it's not particularly useful for debugging.
                            if (lastException.module == EVENT_BUS_MODULE
                                    && lastException.type == EVENT_BUS_EXCEPTION
                                    && lastException.value == EVENT_BUS_INVOKING_SUBSCRIBER_FAILED_ERROR) {
                                event.exceptions.remove(lastException)
                            }
                        }
                    }
                    event
                }
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
