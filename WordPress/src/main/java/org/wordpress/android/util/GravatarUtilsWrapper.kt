package org.wordpress.android.util

import android.content.Context
import androidx.annotation.DimenRes
import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around GravatarUtils.
 *
 * GravatarUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class GravatarUtilsWrapper @Inject constructor(private val appContext: Context) {
    fun fixGravatarUrl(imageUrl: String, avatarSz: Int): String {
        return GravatarUtils.fixGravatarUrl(imageUrl, avatarSz)
    }

    fun fixGravatarUrlWithResource(imageUrl: String, @DimenRes avatarSzRes: Int): String {
        return GravatarUtils.fixGravatarUrl(imageUrl, appContext.resources.getDimensionPixelSize(avatarSzRes))
    }
}
