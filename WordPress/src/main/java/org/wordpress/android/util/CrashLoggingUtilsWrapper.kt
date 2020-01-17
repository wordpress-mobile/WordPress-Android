package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject

@Reusable
class CrashLoggingUtilsWrapper @Inject constructor() {
    fun log(message: String?) = CrashLoggingUtils.log(message)
    fun log(exception: Throwable) = CrashLoggingUtils.log(exception)
    fun logException(throwable: Throwable, tag: AppLog.T) = CrashLoggingUtils.logException(throwable, tag)
    fun logException(throwable: Throwable, tag: AppLog.T, message: String?) =
            CrashLoggingUtils.logException(throwable, tag, message)
}
