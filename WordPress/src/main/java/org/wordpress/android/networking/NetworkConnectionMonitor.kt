package org.wordpress.android.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global monitor for changes to the device's network connectivity.
 *
 * Uses [ConnectivityManager.NetworkCallback] registered on a background [HandlerThread] rather than the
 * deprecated CONNECTIVITY_ACTION broadcast. Because a NetworkCallback is not a broadcast it is not subject to
 * background-broadcast ANR timeouts, and all connectivity work runs off the main thread. The connected state
 * is exposed as [isConnected]; observers are notified on the main thread via LiveData whenever the connected
 * state changes.
 *
 * Connectivity is tracked as the set of currently-available internet-capable networks rather than a single
 * network. During a handover (e.g. Wi-Fi -> cellular) where the replacement is already up, it is added before
 * the old one is removed, so the set doesn't empty and the handover isn't misreported as a disconnection; a
 * genuine disconnect empties the set and is reported reliably.
 */
@Singleton
class NetworkConnectionMonitor @Inject constructor() {
    private var started = false
    private var wasConnected = false
    private var isFirstCallback = true
    private val availableNetworks = mutableSetOf<Network>()

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    @Synchronized
    fun start(context: Context) {
        if (started) return
        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val thread = HandlerThread("NetworkConnectionMonitor").apply { start() }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onNetworkAvailable(network)
            override fun onLost(network: Network) = onNetworkLost(network)
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback, Handler(thread.looper))
        started = true
    }

    @VisibleForTesting
    internal fun onNetworkAvailable(network: Network) {
        availableNetworks.add(network)
        onConnectivityChanged(availableNetworks.isNotEmpty())
    }

    @VisibleForTesting
    internal fun onNetworkLost(network: Network) {
        availableNetworks.remove(network)
        onConnectivityChanged(availableNetworks.isNotEmpty())
    }

    /**
     * Called on the monitor's background thread. Updates [isConnected] on the first callback and whenever the
     * connected state actually changes, so observers aren't spammed while connectivity churns.
     */
    private fun onConnectivityChanged(isConnected: Boolean) {
        if (isFirstCallback || isConnected != wasConnected) {
            isFirstCallback = false
            wasConnected = isConnected
            AppLog.i(T.UTILS, "Connection status changed, isConnected=$isConnected")
            _isConnected.postValue(isConnected)
        }
    }
}
