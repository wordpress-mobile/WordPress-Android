package org.wordpress.android.util

/**
 * No-op stub retained so pre-existing callers in editor fragments
 * and WPLaunchActivity continue to compile.
 */
object ProfilingUtils {
    @Suppress("UnusedParameter")
    @JvmStatic
    fun start(label: String) = Unit

    @Suppress("UnusedParameter")
    @JvmStatic
    fun split(label: String) = Unit

    @JvmStatic
    fun dump() = Unit

    @JvmStatic
    fun stop() = Unit
}
