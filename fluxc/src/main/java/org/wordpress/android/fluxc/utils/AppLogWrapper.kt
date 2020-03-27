package org.wordpress.android.fluxc.utils

import org.wordpress.android.util.AppLog
import javax.inject.Inject

class AppLogWrapper
@Inject constructor() {
    fun d(tag: AppLog.T, message: String) = AppLog.d(tag, message)
}
