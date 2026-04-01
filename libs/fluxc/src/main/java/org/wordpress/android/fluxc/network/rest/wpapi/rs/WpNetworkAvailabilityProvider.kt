package org.wordpress.android.fluxc.network.rest.wpapi.rs

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import rs.wordpress.api.kotlin.NetworkAvailabilityProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WpNetworkAvailabilityProvider @Inject constructor(
    private val context: Context
) : NetworkAvailabilityProvider {
    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true
    }
}
