package org.wordpress.android.util

import javax.inject.Inject

class WpUrlUtilsWrapper @Inject constructor() {
    fun isWordPressCom(interceptedUri: String?) = WPUrlUtils.isWordPressCom(interceptedUri)
}
