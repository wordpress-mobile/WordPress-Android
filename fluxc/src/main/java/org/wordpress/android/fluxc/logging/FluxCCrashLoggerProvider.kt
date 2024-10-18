package org.wordpress.android.fluxc.logging

import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.MAIN

object FluxCCrashLoggerProvider {
    @Volatile
    var crashLogger: FluxCCrashLogger? = null
        get() {
            if (field == null) {
                AppLog.w(T.MAIN, "FluxCCrashLogger is not initialized.")
            }
            return field
        }
        private set

    fun initLogger(logger: FluxCCrashLogger) {
        if (crashLogger == null) {
            synchronized(FluxCCrashLoggerProvider.javaClass) {
                if (crashLogger == null) {
                    crashLogger = logger
                } else {
                    logAlreadyInitialized()
                }
            }
        } else {
            logAlreadyInitialized()
        }
    }

    private fun logAlreadyInitialized() {
        AppLog.w(MAIN, "FluxCCrashLoggerProvider already initialized.")
    }
}
