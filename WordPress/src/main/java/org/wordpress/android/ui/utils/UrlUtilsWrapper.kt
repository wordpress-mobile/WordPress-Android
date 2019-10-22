package org.wordpress.android.ui.utils

import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class UrlUtilsWrapper
@Inject constructor() {
    fun getHost(url: String): String = UrlUtils.getHost(url)
}
