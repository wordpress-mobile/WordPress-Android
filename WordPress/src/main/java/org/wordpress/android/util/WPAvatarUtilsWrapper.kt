package org.wordpress.android.util

import android.content.Context
import androidx.annotation.DimenRes
import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around GravatarUtils.
 *
 * WordPressAvatarUtils interface is consisted of static methods, which makes the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class WPAvatarUtilsWrapper @Inject constructor(private val appContext: Context) {
    fun rewriteAvatarUrl(imageUrl: String, avatarSz: Int): String {
        return WPAvatarUtils.rewriteAvatarUrl(imageUrl, avatarSz)
    }

    fun rewriteAvatarUrlWithResource(imageUrl: String, @DimenRes avatarSzRes: Int): String {
        return WPAvatarUtils.rewriteAvatarUrl(imageUrl,
            appContext.resources.getDimensionPixelSize(avatarSzRes))
    }
}
