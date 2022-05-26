package org.wordpress.android.fluxc.logging

interface FluxCCrashLogger {
    fun recordEvent(message: String, category: String?)

    fun recordException(exception: Throwable, category: String?)

    fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?)
}
