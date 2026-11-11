package org.wordpress.android.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.editor.EditorMediaUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeTypes.Plan
import org.wordpress.android.util.AppLog.T
import java.io.File
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

    fun isAudioMimeType(mimeType: String?): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.isAudioMimeType(mimeType)

    fun isApplicationMimeType(mimeType: String?): Boolean =
        org.wordpress.android.fluxc.utils.MediaUtils.isApplicationMimeType(mimeType)

    fun stripImageLocation(imagePath: String) =
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

    /**
     * Duration check for a file whose mime type is already known. The [Uri] overload can't type a
     * `file://` URI (ContentResolver only types content URIs), so it would fall back to an
     * extension-only test and skip extensionless videos.
     */
    fun isProhibitedVideoDuration(context: Context, site: SiteModel, file: File, mimeType: String): Boolean =
        isProhibitedVideoDuration(context, site, Uri.fromFile(file), isVideoMimeType(mimeType))

    fun isProhibitedVideoDuration(context: Context, site: SiteModel, uri: Uri): Boolean =
        isProhibitedVideoDuration(context, site, uri, isVideoFile(uri))

    @Suppress("TooGenericExceptionCaught")
    private fun isProhibitedVideoDuration(context: Context, site: SiteModel, uri: Uri, isVideo: Boolean): Boolean {
        if (isVideo && site.hasFreePlan && !site.isActiveModuleEnabled("videopress")) {
            val retriever = MediaMetadataRetriever()

            // The native extractor throws a plain RuntimeException for content it can't parse.
            // An unknown duration leaves the decision to the server rather than failing the upload.
            val videoDuration = try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            } catch (e: RuntimeException) {
                AppLog.d(T.MEDIA, "Cannot retrieve video file $e")
                0
            } finally {
                retriever.release()
            }

            val allowedVideoDurationForFreeSites = TimeUnit.MILLISECONDS.convert(DURATION_5_MIN, TimeUnit.MINUTES)
            return allowedVideoDurationForFreeSites < videoDuration
        }

        return false
    }

    companion object {
        private const val DURATION_5_MIN = 5L
    }
}
