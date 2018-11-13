package org.wordpress.android.viewmodel.helpers

import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

/**
 * A wrapper class for the network connection status. It can be extended to provide more details about the current
 * network connection.
 */
class ConnectionStatus(val isConnected: Boolean)

/**
 * A LiveData instance that can be injected to keep track of the network availability.
 *
 * IMPORTANT: It needs to be observed for the changes to be posted.
 */
class ConnectionStatusLiveData(private val context: Context) : LiveData<ConnectionStatus>() {
    override fun onActive() {
        super.onActive()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(networkReceiver, intentFilter)
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(networkReceiver)
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val networkInfo = connectivityManager?.activeNetworkInfo
            postValue(ConnectionStatus(networkInfo?.isConnected == true))
        }
    }
}
