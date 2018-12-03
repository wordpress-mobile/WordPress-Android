package org.wordpress.android.util

import org.wordpress.android.WordPress
import org.wordpress.android.util.NetworkUtils.getActiveNetworkInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtilsWrapper @Inject constructor() {
    /**
     * Returns true if a network connection is available.
     */
    fun isNetworkAvailable(): Boolean {
        val info = getActiveNetworkInfo(WordPress.getContext())
        return info != null && info.isConnected
    }
}
