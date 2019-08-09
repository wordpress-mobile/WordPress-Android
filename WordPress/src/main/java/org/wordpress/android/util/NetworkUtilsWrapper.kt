package org.wordpress.android.util

import dagger.Reusable
import org.wordpress.android.WordPress
import javax.inject.Inject

@Reusable
class NetworkUtilsWrapper @Inject constructor() {
    /**
     * Returns true if a network connection is available.
     */
    fun isNetworkAvailable() = NetworkUtils.isNetworkAvailable(WordPress.getContext())
}
