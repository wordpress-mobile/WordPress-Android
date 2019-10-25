package org.wordpress.android.util

import android.content.Context
import android.net.Uri
import dagger.Reusable
import javax.inject.Inject

/**
 * Injectable wrapper around MediaUtils, WPMediaUtils & FluxC's MediaUtils.
 *
 * MediaUtils and WPMediaUtils interfaces are consisted of static methods, which makes the client code difficult to
 * test/mock. Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class MediaUtilsWrapper @Inject constructor(private val context: Context) {
    fun getRealPathFromURI(mediaUri: Uri): String? =
            MediaUtils.getRealPathFromURI(context, mediaUri)

    fun isVideo(mediaUri: Uri) = MediaUtils.isVideo(mediaUri.toString())

    fun isVideo(mediaUriString: String) = MediaUtils.isVideo(mediaUriString)

    fun getOptimizedMedia(path: String, isVideo: Boolean): Uri? =
            WPMediaUtils.getOptimizedMedia(context, path, isVideo)

    fun fixOrientationIssue(path: String, isVideo: Boolean): Uri? =
            WPMediaUtils.fixOrientationIssue(context, path, isVideo)

    fun isVideoMimeType(mimeType: String): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(mimeType)

    fun isInMediaStore(mediaUri: Uri?): Boolean =
            MediaUtils.isInMediaStore(mediaUri)

    fun downloadExternalMedia(imageUri: Uri): Uri? =
            MediaUtils.downloadExternalMedia(context, imageUri)

    fun shouldAdvertiseImageOptimization(): Boolean =
            WPMediaUtils.shouldAdvertiseImageOptimization(context)

    fun advertiseImageOptimization(listener: () -> Unit) {
        WPMediaUtils.advertiseImageOptimization(context) {
            listener.invoke()
        }
    }

    fun getMimeType(uri: Uri): String? = context.contentResolver.getType(uri)
}
