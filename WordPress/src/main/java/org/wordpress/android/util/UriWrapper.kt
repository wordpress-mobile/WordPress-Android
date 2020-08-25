package org.wordpress.android.util

import android.net.Uri

/**
 * This class is necessary because standard Uri doesn't work in unit tests (it's always null)
 */
class UriWrapper(val uri: Uri) {
    override fun toString() = uri.toString()
}
