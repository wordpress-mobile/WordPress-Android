package org.wordpress.android.util

import android.net.Uri
import dagger.Reusable
import javax.inject.Inject

@Reusable
class UriUtilsWrapper @Inject constructor() {
    fun parse(uriString: String?): UriWrapper = UriWrapper(Uri.parse(uriString))
}
