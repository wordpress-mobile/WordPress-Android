package org.wordpress.android.util

import android.content.Context
import javax.inject.Inject

class WpUrlUtilsWrapper @Inject constructor() {
    fun isWordPressCom(interceptedUri: String?) = WPUrlUtils.isWordPressCom(interceptedUri)
    fun buildTermsOfServiceUrl(context: Context): String = WPUrlUtils.buildTermsOfServiceUrl(context)
    fun buildPatchedUrl(context: Context, url: String): String = WPUrlUtils.buildPatchedUrl(context, url)
}
