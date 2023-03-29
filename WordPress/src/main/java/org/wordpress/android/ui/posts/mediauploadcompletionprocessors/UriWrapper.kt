package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import android.net.Uri

/**
 * Wrapper for the Uri class.
 *
 * We need to use the Uri.encode function in order to encode a URL for the VideoPress block.
 * However, As Uri.encode is part an Android class, it cannot run in unit tests by default.
 * Note, java.net.URLEncoder is not currently a suitable alternative as it requires API 33 or later
 * (we support down to API 24 at the time of writing).
 *
 * The main purpose of this wrapper is to make it easier to reference and replace this functionality in unit tests.
 */
class UriWrapper {
    fun encode(input: String): String {
        return Uri.encode(input)
    }
}
