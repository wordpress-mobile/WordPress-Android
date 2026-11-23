package org.wordpress.android.ui.media.library

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.widgets.AppReviewManager
import javax.inject.Inject

/**
 * Turns files picked, captured or shared into the media library into local media and queues them for upload.
 * Each queued item is written to the store first, so the library picks it up through `OnMediaChanged`.
 */
class MediaLibraryUploader @Inject constructor(
    private val appContext: Context,
    private val dispatcher: Dispatcher,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val uploadServiceFacade: UploadServiceFacade
) {
    fun uploadPickedFiles(site: SiteModel, uris: List<Uri>) {
        uris.forEach { uri ->
            val mimeType = appContext.contentResolver.getType(uri)
            if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, uri)) {
                showVideoDurationError()
            } else {
                WPMediaUtils.fetchMediaAndDoNext(appContext, uri) { downloadedUri ->
                    queueFileForUpload(site, getOptimizedPictureIfNecessary(site, downloadedUri), mimeType)
                }
                trackAddedMedia(isNewMedia = false, isVideo = mediaUtilsWrapper.isVideoMimeType(mimeType), uri = uri)
            }
        }
    }

    fun uploadCapturedPhoto(site: SiteModel, capturePath: String) {
        WPMediaUtils.scanMediaFile(appContext, capturePath)
        val uri = getOptimizedPictureIfNecessary(site, capturePath.toUri())
        queueFileForUpload(site, uri, appContext.contentResolver.getType(uri))
        trackAddedMedia(isNewMedia = true, isVideo = false, uri = uri)
    }

    fun uploadCapturedVideo(site: SiteModel, uri: Uri) {
        if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, uri)) {
            showVideoDurationError()
        } else {
            queueFileForUpload(site, uri, appContext.contentResolver.getType(uri))
        }
        trackAddedMedia(isNewMedia = true, isVideo = true, uri = uri)
    }

    fun uploadSharedMedia(site: SiteModel, uris: List<Uri>) {
        uris.forEach { uri ->
            WPMediaUtils.fetchMediaAndDoNext(appContext, uri) { downloadedUri ->
                queueFileForUpload(
                    site,
                    getOptimizedPictureIfNecessary(site, downloadedUri),
                    appContext.contentResolver.getType(downloadedUri)
                )
            }
        }
    }

    /** Hands [mediaList] to the upload service, or tells the user it can't when offline */
    fun addToUploadService(mediaList: List<MediaModel>) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start UploadService, internet connection required.")
            ToastUtils.showToast(appContext, R.string.no_network_message, ToastUtils.Duration.SHORT)
            return
        }
        uploadServiceFacade.uploadMedia(ArrayList(mediaList), UPLOAD_SOURCE_FOR_LOGGING)
        AppReviewManager.incrementInteractions(Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA)
    }

    private fun queueFileForUpload(site: SiteModel, uri: Uri, mimeType: String?) {
        val media = fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)
        if (media == null) {
            ToastUtils.showToast(appContext, R.string.file_not_found, ToastUtils.Duration.SHORT)
            return
        }
        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
        addToUploadService(listOf(media))
    }

    private fun getOptimizedPictureIfNecessary(site: SiteModel, originalUri: Uri): Uri {
        val filePath = MediaUtils.getRealPathFromURI(appContext, originalUri)
        if (filePath.isNullOrEmpty()) {
            return originalUri
        }
        WPMediaUtils.getOptimizedMedia(appContext, filePath, false)?.let { return it }

        // Optimization is off, so make sure the picture is in portrait for a self-hosted site
        // https://github.com/wordpress-mobile/WordPress-Android/issues/5737
        if (!site.isWPCom) {
            WPMediaUtils.fixOrientationIssue(appContext, filePath, false)?.let { return it }
        }
        return originalUri
    }

    private fun showVideoDurationError() {
        ToastUtils.showToast(appContext, R.string.error_media_video_duration_exceeds_limit, ToastUtils.Duration.LONG)
    }

    private fun trackAddedMedia(isNewMedia: Boolean, isVideo: Boolean, uri: Uri) {
        val properties = AnalyticsUtils.getMediaProperties(appContext, isVideo, uri, null)
        properties["via"] = if (isNewMedia) "device_camera" else "device_library"
        val stat = if (isVideo) Stat.MEDIA_LIBRARY_ADDED_VIDEO else Stat.MEDIA_LIBRARY_ADDED_PHOTO
        AnalyticsTracker.track(stat, properties)
    }

    private companion object {
        const val UPLOAD_SOURCE_FOR_LOGGING = "MediaBrowserActivity#addMediaToUploadService"
    }
}
