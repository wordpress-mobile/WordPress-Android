package org.wordpress.android.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
    private var handlerThread: HandlerThread? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    class ConnectionChangeEvent(val isConnected: Boolean)

    @Synchronized
    fun start(context: Context) {
        if (started) return
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = manager

        val thread = HandlerThread("NetworkConnectionMonitor").apply { start() }
        handlerThread = thread

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onConnectivityChanged(true)
            override fun onLost(network: Network) = onConnectivityChanged(false)
        }
        networkCallback = callback

        manager.registerDefaultNetworkCallback(callback, Handler(thread.looper))
        started = true
    }

    @Synchronized
    fun stop() {
        if (!started) return
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        handlerThread?.quitSafely()
        networkCallback = null
        handlerThread = null
        connectivityManager = null
        started = false
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
}
