package org.wordpress.android.util

import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around GravatarUtils.
 *
 * GravatarUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class GravatarUtilsWrapper @Inject constructor() {
    fun fixGravatarUrl(imageUrl: String, avatarSz: Int): String {
        return GravatarUtils.fixGravatarUrl(imageUrl, avatarSz)
    }
}