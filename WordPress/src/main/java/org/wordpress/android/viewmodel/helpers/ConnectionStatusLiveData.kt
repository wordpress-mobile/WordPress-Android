package org.wordpress.android.viewmodel.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData.Factory
import javax.inject.Inject

enum class ConnectionStatus {
    AVAILABLE,
    UNAVAILABLE
}

/**
 * A LiveData instance that can be injected to keep track of the network availability.
 *
 * Use [Factory] to create an instance. The Factory guarantees that this only emits if the network availability
 * changes and not when the user switches between cellular and wi-fi.
 *
 * IMPORTANT: It needs to be observed for the changes to be posted.
 */
class ConnectionStatusLiveData private constructor(private val context: Context) : LiveData<ConnectionStatus>() {
    @Suppress("DEPRECATION")
    override fun onActive() {
        super.onActive()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(networkReceiver, intentFilter)
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(networkReceiver)
    }

    @Suppress("DEPRECATION")
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val networkInfo = connectivityManager?.activeNetworkInfo

            val nextValue: ConnectionStatus = if (networkInfo?.isConnected == true) {
                ConnectionStatus.AVAILABLE
            } else {
                ConnectionStatus.UNAVAILABLE
            }
            postValue(nextValue)
        }
    }

    class Factory @Inject constructor(private val context: Context) {
        fun create() = ConnectionStatusLiveData(context).distinct()
    }
}
