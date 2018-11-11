package org.wordpress.android.viewmodel.helpers

import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

class ConnectionStatus(val isConnected: Boolean)

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
