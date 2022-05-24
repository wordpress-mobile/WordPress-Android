package org.wordpress.android.fluxc.logging

import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

object FluxCCrashLoggerProvider {
    var crashLogger: FluxCCrashLogger? = null
        get() {
            if (field == null) {
                AppLog.w(T.MAIN, "FluxCCrashLogger is not initialized.")
            }
            return field
        }
        private set

    fun initLogger(logger: FluxCCrashLogger) {
        crashLogger = logger
    }
}
