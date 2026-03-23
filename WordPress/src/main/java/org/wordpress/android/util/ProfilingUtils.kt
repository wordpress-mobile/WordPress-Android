package org.wordpress.android.util

import android.os.SystemClock
import android.os.Trace
import org.wordpress.android.util.AppLog.T

/**
 * Utility for profiling app startup and other critical paths.
 *
 * Combines [android.os.Trace] sections (visible in Android Studio
 * CPU Profiler / systrace) with wall-clock split timing logged via
 * [AppLog] so that timings are available in debug logs as well.
 *
 * Usage:
 * ```
 * ProfilingUtils.start("App Startup")
 * // ... work ...
 * ProfilingUtils.split("after DB init")
 * // ... more work ...
 * ProfilingUtils.dump()
 * ProfilingUtils.stop()
 * ```
 */
object ProfilingUtils {
    private var sessionLabel: String? = null
    private var sessionStartMs: Long = 0
    private var lastSplitMs: Long = 0
    private val splits = mutableListOf<Split>()

    private data class Split(
        val label: String,
        val wallMs: Long,
        val sinceLastMs: Long
    )

    /**
     * Begin a new profiling session. Resets any prior state.
     */
    @JvmStatic
    fun start(label: String) {
        sessionLabel = label
        sessionStartMs = SystemClock.elapsedRealtime()
        lastSplitMs = sessionStartMs
        splits.clear()
        Trace.beginSection(label)
    }

    /**
     * Record a named split / checkpoint within the current session.
     * Also starts a systrace section for the *next* span (the one
     * between this split and the following split or [stop]).
     */
    @JvmStatic
    fun split(label: String) {
        val now = SystemClock.elapsedRealtime()
        val sinceStart = now - sessionStartMs
        val sinceLast = now - lastSplitMs
        splits.add(Split(label, sinceStart, sinceLast))
        lastSplitMs = now

        // End the previous systrace section and start a new one
        // named after this split so the *next* span is labelled.
        Trace.endSection()
        Trace.beginSection(label)
    }

    /**
     * Log all collected splits to [AppLog].
     */
    @JvmStatic
    fun dump() {
        val total = SystemClock.elapsedRealtime() - sessionStartMs
        val session = sessionLabel ?: "Profiling"
        AppLog.d(
            T.PROFILING,
            "--- $session: ${total}ms total ---"
        )
        for (s in splits) {
            AppLog.d(
                T.PROFILING,
                "  ${s.label}: +${s.sinceLastMs}ms (${s.wallMs}ms total)"
            )
        }
        AppLog.d(T.PROFILING, "--- end $session ---")
    }

    /**
     * End the current profiling session and close the systrace section.
     */
    @JvmStatic
    fun stop() {
        Trace.endSection()
        sessionLabel = null
        splits.clear()
    }
}
