package org.wordpress.android.ui.main.utils

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.ResourceProvider
import java.math.BigInteger
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeGravatarLoader @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val imageManager: ImageManager,
    private val resourseProvider: ResourceProvider
) {
    fun load(
        newAvatarSelected: Boolean,
        avatarUrl: String,
        injectFilePath: String?,
        imageView: ImageView,
        imageType: ImageType,
        listener: RequestListener<Drawable>? = null
    ) {
        if (newAvatarSelected) {
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
                if (newAvatarSelected && injectFilePath != null) {
                    injectFilePath
                } else {
                    // If new avatar selected we force refresh the avatar
                    val constructGravatarUrl = constructGravatarUrl(avatarUrl, newAvatarSelected)
                    constructGravatarUrl
                },
                listener,
                appPrefsWrapper.avatarVersion
            )
        }
    }
    fun constructGravatarUrl(email: String, forceRefresh: Boolean = false): String {
        val avatarSz = resourseProvider.getDimensionPixelSize(R.dimen.avatar_sz_extra_small)
        val trimmedEmail = email.trim().lowercase()
        val md5Hash = md5Hash(trimmedEmail)
        val cacheBuster = if (forceRefresh) "?d=${System.currentTimeMillis()}" else ""
        return "https://www.gravatar.com/avatar/$md5Hash?s=$avatarSz$cacheBuster"
    }

    fun md5Hash(input: String): String{
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray()))
            .toString(16)
            .padStart(32, '0')
    }
}
