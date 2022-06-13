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
import kotlin.IllegalArgumentException

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

    fun getLastRecordedVideoUri(): Uri = MediaUtils.getLastRecordedVideoUri(appContext)

    fun getOptimizedMedia(path: String, isVideo: Boolean): Uri? =
            WPMediaUtils.getOptimizedMedia(appContext, path, isVideo)

    fun fixOrientationIssue(path: String, isVideo: Boolean): Uri? =
            WPMediaUtils.fixOrientationIssue(appContext, path, isVideo)

    fun isVideoMimeType(mimeType: String?): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(mimeType)

    fun isInMediaStore(mediaUri: Uri?): Boolean =
            MediaUtils.isInMediaStore(mediaUri)

    fun copyFileToAppStorage(imageUri: Uri, headers: Map<String, String>? = null): Uri? =
            MediaUtils.downloadExternalMedia(appContext, imageUri, headers)

    fun shouldAdvertiseImageOptimization(): Boolean =
            WPMediaUtils.shouldAdvertiseImageOptimization(appContext)

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
        if (isVideoFile(uri) && site.hasFreePlan && !site.isActiveModuleEnabled("videopress")) {
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, uri)
            } catch (e: IllegalArgumentException) {
                AppLog.d(T.MEDIA, "Cannot retrieve video file $e")
            }

            val videoDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            retriever.release()

            val allowedVideoDurationForFreeSites = TimeUnit.MILLISECONDS.convert(DURATION_5_MIN, TimeUnit.MINUTES)
            return allowedVideoDurationForFreeSites < videoDuration
        }

        return false
    }

    companion object {
        private const val DURATION_5_MIN = 5L
    }
}
