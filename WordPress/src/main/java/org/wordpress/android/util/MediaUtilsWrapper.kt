package org.wordpress.android.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.editor.EditorMediaUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeTypes.Plan
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Injectable wrapper around MediaUtils, WPMediaUtils, FluxC's MediaUtils & EditorMediaUtils.
 *
 * MediaUtils, WPMediaUtils, FluxC's MediaUtils & EditorMediaUtils interfaces are consisted of static methods, which
 * makes the client code difficult to test/mock. Main purpose of this wrapper is to make testing easier.
 */
@Reusable
class MediaUtilsWrapper @Inject constructor(private val appContext: Context) {
    fun getRealPathFromURI(mediaUri: Uri): String? =
        MediaUtils.getRealPathFromURI(appContext, mediaUri)

    fun isVideo(mediaUri: Uri) = MediaUtils.isVideo(mediaUri.toString())

    fun isVideo(mediaUriString: String) = MediaUtils.isVideo(mediaUriString)

    fun getOptimizedMedia(path: String, isVideo: Boolean): Uri? =
        WPMediaUtils.getOptimizedMedia(appContext, path, isVideo)

    fun fixOrientationIssue(path: String, isVideo: Boolean): Uri? =
        WPMediaUtils.fixOrientationIssue(appContext, path, isVideo)

    fun isVideoMimeType(mimeType: String?): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(mimeType)

    /** @return whether the location was removed; false leaves it in the file. */
    fun stripImageLocation(imagePath: String): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.stripLocation(imagePath)

    fun isInMediaStore(mediaUri: Uri?): Boolean =
        MediaUtils.isInMediaStore(mediaUri)

    fun copyFileToAppStorage(imageUri: Uri, headers: Map<String, String>? = null): Uri? =
        MediaUtils.downloadExternalMedia(appContext, imageUri, headers)

    fun getMimeType(uri: Uri): String? = appContext.contentResolver.getType(uri)

    fun getVideoThumbnail(videoPath: String, headers: Map<String, String>): String? =
        EditorMediaUtils.getVideoThumbnail(appContext, videoPath, headers)

    fun isLocalFile(uploadState: String): Boolean = MediaUtils.isLocalFile(uploadState)

    fun getExtensionForMimeType(mimeType: String?): String = MediaUtils.getExtensionForMimeType(mimeType)

    fun isFile(mediaUri: Uri): Boolean = MediaUtils.isFile(mediaUri)

    fun getSitePlanForMimeTypes(site: SiteModel?): Plan = WPMediaUtils.getSitePlanForMimeTypes(site)

    fun isMimeTypeSupportedBySitePlan(site: SiteModel?, mimeType: String): Boolean =
        WPMediaUtils.isMimeTypeSupportedBySitePlan(site, mimeType)

    fun isVideoFile(mediaUri: Uri): Boolean =
        isVideo(mediaUri) || isVideoMimeType(getMimeType(mediaUri))

    fun isProhibitedVideoDuration(context: Context, site: SiteModel, uri: Uri): Boolean {
        // The five-minute limit only applies to WordPress.com-hosted sites on the free plan. Self-hosted
        // sites connected to Jetpack Free also report a free plan, but they upload to their own media library.
        if (!isVideoFile(uri) || !site.isWPCom || !site.hasFreePlan || site.isActiveModuleEnabled("videopress")) {
            return false
        }

        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)
            val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            val allowedVideoDurationForFreeSites = TimeUnit.MILLISECONDS.convert(DURATION_5_MIN, TimeUnit.MINUTES)
            allowedVideoDurationForFreeSites < videoDuration
        } catch (e: RuntimeException) {
            AppLog.d(T.MEDIA, "Cannot retrieve video file $e")
            false
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val DURATION_5_MIN = 5L
    }
}
