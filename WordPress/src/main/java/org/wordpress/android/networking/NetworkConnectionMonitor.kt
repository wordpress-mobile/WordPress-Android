package org.wordpress.android.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.HandlerThread
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global monitor for changes to the device's default network connectivity.
 *
 * Uses [ConnectivityManager.NetworkCallback] registered on a background [HandlerThread] rather than the
 * deprecated CONNECTIVITY_ACTION broadcast. Because a NetworkCallback is not a broadcast it is not subject to
 * background-broadcast ANR timeouts, and all connectivity work runs off the main thread. A
 * [ConnectionChangeEvent] is posted on EventBus whenever the connected state changes; EventBus marshals the
 * `onEventMainThread` subscribers back onto the main thread.
 */
@Singleton
class NetworkConnectionMonitor @Inject constructor() {
    private var started = false
    private var wasConnected = false
    private var isFirstCallback = true

    private var connectivityManager: ConnectivityManager? = null

    class ConnectionChangeEvent(val isConnected: Boolean)

    @Synchronized
    fun start(context: Context) {
        if (started) return
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = manager

        val thread = HandlerThread("NetworkConnectionMonitor").apply { start() }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onConnectivityChanged(true)
            // onLost fires for the network that was lost, but during a handover (e.g. Wi-Fi -> cellular) the
            // new default has already arrived via onAvailable, so re-query rather than assuming we're offline.
            override fun onLost(network: Network) = onConnectivityChanged(hasActiveConnection())
        }
        manager.registerDefaultNetworkCallback(callback, Handler(thread.looper))
        started = true
    }

    /**
     * Called on the monitor's background thread. Posts a [ConnectionChangeEvent] on the first callback and
     * whenever the connected state actually changes, so subscribers aren't spammed when a network's
     * capabilities change without a change in availability.
     */
    private fun onConnectivityChanged(isConnected: Boolean) {
        if (isFirstCallback || isConnected != wasConnected) {
            isFirstCallback = false
            wasConnected = isConnected
            AppLog.i(T.UTILS, "Connection status changed, isConnected=$isConnected")
            EventBus.getDefault().post(ConnectionChangeEvent(isConnected))
        }
    }

    /**
     * Whether there is currently a default network capable of reaching the internet. Used to distinguish a
     * true disconnection from a handover between networks, where the replacement is already the default.
     */
    private fun hasActiveConnection(): Boolean {
        val manager = connectivityManager ?: return false
        val capabilities = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
