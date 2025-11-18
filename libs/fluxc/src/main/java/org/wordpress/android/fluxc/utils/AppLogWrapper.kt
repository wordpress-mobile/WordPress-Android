package org.wordpress.android.fluxc.utils

import org.wordpress.android.util.AppLog
import javax.inject.Inject

class AppLogWrapper @Inject constructor() {
    fun d(tag: AppLog.T, message: String) = AppLog.d(tag, message)
    fun e(tag: AppLog.T, message: String) = AppLog.e(tag, message)
    fun e(tag: AppLog.T, message: String, throwable: Throwable? = null) = AppLog.e(tag, message, throwable)
    fun w(tag: AppLog.T, message: String) = AppLog.w(tag, message)
}
