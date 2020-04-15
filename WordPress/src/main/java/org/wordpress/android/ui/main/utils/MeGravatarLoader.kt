package org.wordpress.android.ui.main.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeGravatarLoader @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val imageManager: ImageManager,
    private val resourseProvider: ResourceProvider
) {
    fun load(
        newAvatarUploaded: Boolean,
        avatarUrl: String,
        injectFilePath: String?,
        imageView: ImageView,
        imageType: ImageType,
        listener: RequestListener<Drawable>? = null
    ) {
        if (newAvatarUploaded) {
            // invalidate the specific gravatar entry from the bitmap cache. It will be updated via the injected
            // request cache.
            WordPress.getBitmapCache().removeSimilar(avatarUrl)
            // Changing the signature invalidates Glide's cache
            appPrefsWrapper.avatarVersion = appPrefsWrapper.avatarVersion + 1
        }

        val bitmap = WordPress.getBitmapCache().get(avatarUrl)
        // Avatar's API doesn't synchronously update the image at avatarUrl. There is a replication lag
        // (cca 5s), before the old avatar is replaced with the new avatar. Therefore we need to use this workaround,
        // which temporary saves the new image into a local bitmap cache.
        if (bitmap != null) {
            imageManager.load(imageView, bitmap)
        } else {
            imageManager.loadIntoCircle(
                    imageView,
                    imageType,
                    if (newAvatarUploaded && injectFilePath != null) {
                        injectFilePath
                    } else {
                        avatarUrl
                    },
                    listener,
                    appPrefsWrapper.avatarVersion
            )
        }
    }

    fun constructGravatarUrl(rawAvatarUrl: String): String {
        val avatarSz = resourseProvider.getDimensionPixelSize(R.dimen.avatar_sz_large)
        return GravatarUtils.fixGravatarUrl(rawAvatarUrl, avatarSz)
    }
}
