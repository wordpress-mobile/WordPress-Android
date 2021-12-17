package org.wordpress.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.editor.EditorMediaUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeTypes.Plan
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

    @SuppressLint("InlinedApi")
    fun isAllowedUploadVideoDuration(context: Context, uri: Uri): Boolean {
        val mediaColumns = arrayOf(android.provider.MediaStore.Video.VideoColumns.DURATION)
        val cursor: Cursor? = context.contentResolver.query(
                uri,
                mediaColumns,
                null,
                null,
                null
        )

        cursor?.moveToFirst()
        val durationColIndex: Int? = cursor?.getColumnIndexOrThrow(mediaColumns[0])
        val fileDuration: Long = durationColIndex?.let { cursor.getLong(it) } ?: 0
        cursor?.close()

        return TimeUnit.MILLISECONDS.toMinutes(fileDuration) <= DURATION_LIMIT_5_MIN
    }

    companion object {
        private const val DURATION_LIMIT_5_MIN = 5
    }
}
