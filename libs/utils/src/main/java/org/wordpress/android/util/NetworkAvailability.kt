package org.wordpress.android.util

/**
 * Abstraction for checking network availability.
 * Implementations are provided via DI in the app module.
 */
interface NetworkAvailability {
    fun isNetworkAvailable(): Boolean
}
