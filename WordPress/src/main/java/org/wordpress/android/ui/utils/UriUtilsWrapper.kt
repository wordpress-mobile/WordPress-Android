package org.wordpress.android.ui.utils

import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class UriUtilsWrapper
@Inject constructor() {
    fun getHost(url: String) = UrlUtils.getHost(url)
}
