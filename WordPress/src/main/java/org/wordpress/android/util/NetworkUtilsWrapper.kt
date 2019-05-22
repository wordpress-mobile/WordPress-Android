package org.wordpress.android.util

import org.wordpress.android.WordPress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class NetworkUtilsWrapper @Inject constructor() {
    /**
     * Returns true if a network connection is available.
     */
    fun isNetworkAvailable() = NetworkUtils.isNetworkAvailable(WordPress.getContext())
}
