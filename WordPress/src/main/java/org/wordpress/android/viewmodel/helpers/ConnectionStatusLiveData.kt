package org.wordpress.android.viewmodel.helpers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

enum class ConnectionStatus {
    AVAILABLE,
    UNAVAILABLE
}

/**
 * A LiveData instance that can be injected to keep track of the network availability.
 *
 * Backed by [org.wordpress.android.networking.NetworkConnectionMonitor], which observes connectivity through a
 * ConnectivityManager.NetworkCallback on a background thread rather than the deprecated CONNECTIVITY_ACTION
 * broadcast.
 *
 * Only emits when the connected state changes. The state the monitor already holds when this instance is created
 * is taken as the baseline, so the value LiveData replays to a new observer is swallowed instead of re-running
 * that observer's side effect (a refresh, a retry, an upload) every time it starts observing.
 *
 * IMPORTANT: It needs to be observed for the changes to be posted.
 */
class ConnectionStatusLiveData(source: LiveData<Boolean>) : MediatorLiveData<ConnectionStatus>() {
    private var lastStatus: ConnectionStatus? = source.value?.toConnectionStatus()

    init {
        addSource(source) { isConnected ->
            val status = isConnected.toConnectionStatus()
            if (status != lastStatus) {
                lastStatus = status
                value = status
            }
        }
    }
}

private fun Boolean.toConnectionStatus() =
    if (this) ConnectionStatus.AVAILABLE else ConnectionStatus.UNAVAILABLE
