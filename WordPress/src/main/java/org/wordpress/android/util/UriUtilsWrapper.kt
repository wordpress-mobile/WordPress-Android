package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject
import androidx.core.net.toUri

@Reusable
class UriUtilsWrapper @Inject constructor() {
    fun parse(uriString: String): UriWrapper = UriWrapper(uriString.toUri())
}
