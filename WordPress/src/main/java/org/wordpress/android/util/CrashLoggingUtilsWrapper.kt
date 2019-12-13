package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject

@Reusable
class CrashLoggingUtilsWrapper @Inject constructor() {
    fun log(message: String?) = CrashLoggingUtils.log(message)
    fun log(exception: Throwable) = CrashLoggingUtils.log(exception)
    fun logException(tr: Throwable, tag: AppLog.T) = CrashLoggingUtils.logException(tr, tag)
    fun logException(tr: Throwable, tag: AppLog.T, message: String?) =
            CrashLoggingUtils.logException(tr, tag, message)
}
