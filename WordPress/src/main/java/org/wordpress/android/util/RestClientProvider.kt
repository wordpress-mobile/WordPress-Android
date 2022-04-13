package org.wordpress.android.util

import org.wordpress.android.WordPress
import org.wordpress.android.networking.RestClientUtils
import javax.inject.Inject

class RestClientProvider @Inject constructor() {
    fun getRestClientUtilsV2(): RestClientUtils = WordPress.getRestClientUtilsV2()
}
