package org.wordpress.android.ui.media.library

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
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
import javax.inject.Named

/**
 * Turns files picked, captured or shared into the media library into local media and queues them for upload.
 * The work runs in the app scope, so leaving the library while files are still being copied doesn't drop them.
 * Copying, resizing and reading metadata run on [bgDispatcher]; toasts, store writes and starting the upload run
 * on [mainDispatcher]. Each queued item is written to the store first, so the library picks it up through
 * `OnMediaChanged`.
 */
class MediaLibraryUploader @Inject constructor(
    private val appContext: Context,
    private val dispatcher: Dispatcher,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val uploadServiceFacade: UploadServiceFacade,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    fun uploadPickedFiles(site: SiteModel, uris: List<Uri>) = launchOnMain {
        uris.forEach { uri ->
            val prepared = withContext(bgDispatcher) {
                val mimeType = appContext.contentResolver.getType(uri)
                if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, uri)) {
                    Prepared.Failed(R.string.error_media_video_duration_exceeds_limit)
                } else {
                    trackAddedMedia(isNewMedia = false, isVideo = mediaUtilsWrapper.isVideoMimeType(mimeType), uri)
                    downloadAndPrepare(site, uri, mimeType)
                }
            }
            queue(prepared)
        }
    }

    fun uploadCapturedPhoto(site: SiteModel, capturePath: String) = launchOnMain {
        val prepared = withContext(bgDispatcher) {
            WPMediaUtils.scanMediaFile(appContext, capturePath)
            val uri = getOptimizedPictureIfNecessary(site, capturePath.toUri())
            trackAddedMedia(isNewMedia = true, isVideo = false, uri = uri)
            prepare(site, uri, appContext.contentResolver.getType(uri))
        }
        queue(prepared)
    }

    fun uploadCapturedVideo(site: SiteModel, uri: Uri) = launchOnMain {
        val prepared = withContext(bgDispatcher) {
            trackAddedMedia(isNewMedia = true, isVideo = true, uri = uri)
            if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, uri)) {
                Prepared.Failed(R.string.error_media_video_duration_exceeds_limit)
            } else {
                prepare(site, uri, appContext.contentResolver.getType(uri))
            }
        }
        queue(prepared)
    }

    fun uploadSharedMedia(site: SiteModel, uris: List<Uri>) = launchOnMain {
        uris.forEach { uri ->
            queue(withContext(bgDispatcher) { downloadAndPrepare(site, uri, mimeType = null) })
        }
    }

    /** Hands [mediaList] to the upload service, or tells the user it can't when offline */
    fun addToUploadService(mediaList: List<MediaModel>) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            AppLog.v(AppLog.T.MEDIA, "Unable to start UploadService, internet connection required.")
            ToastUtils.showToast(appContext, R.string.no_network_message, ToastUtils.Duration.SHORT)
            return
        }
        try {
            uploadServiceFacade.uploadMedia(ArrayList(mediaList), UPLOAD_SOURCE_FOR_LOGGING)
        } catch (e: IllegalStateException) {
            // the copy can finish after the app went to the background, where starting a service isn't allowed
            // see: https://github.com/wordpress-mobile/WordPress-Android/issues/18638
            AppLog.e(AppLog.T.MEDIA, "Unable to start UploadService: ${e.message}")
            return
        }
        AppReviewManager.incrementInteractions(Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA)
    }

    private fun launchOnMain(block: suspend CoroutineScope.() -> Unit) {
        appScope.launch(mainDispatcher, block = block)
    }

    /** Copies [uri] to a local file; a null [mimeType] is read from the copy */
    private fun downloadAndPrepare(site: SiteModel, uri: Uri, mimeType: String?): Prepared {
        val downloadedUri = WPMediaUtils.fetchMedia(appContext, uri)
            ?: return Prepared.Failed(R.string.error_downloading_image)
        val optimizedUri = getOptimizedPictureIfNecessary(site, downloadedUri)
        return prepare(site, optimizedUri, mimeType ?: appContext.contentResolver.getType(downloadedUri))
    }

    private fun prepare(site: SiteModel, uri: Uri, mimeType: String?): Prepared =
        fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)?.let { Prepared.Ready(it) }
            ?: Prepared.Failed(R.string.file_not_found)

    private fun queue(prepared: Prepared) {
        when (prepared) {
            is Prepared.Failed -> ToastUtils.showToast(appContext, prepared.messageResId, ToastUtils.Duration.LONG)
            is Prepared.Ready -> {
                dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(prepared.media))
                addToUploadService(listOf(prepared.media))
            }
        }
    }

    private fun getOptimizedPictureIfNecessary(site: SiteModel, originalUri: Uri): Uri {
        val filePath = MediaUtils.getRealPathFromURI(appContext, originalUri)?.takeIf { it.isNotEmpty() }
            ?: return originalUri
        // when optimization is off, make sure the picture is in portrait for a self-hosted site
        // https://github.com/wordpress-mobile/WordPress-Android/issues/5737
        return WPMediaUtils.getOptimizedMedia(appContext, filePath, false)
            ?: (if (site.isWPCom) null else WPMediaUtils.fixOrientationIssue(appContext, filePath, false))
            ?: originalUri
    }

    private fun trackAddedMedia(isNewMedia: Boolean, isVideo: Boolean, uri: Uri) {
        val properties = AnalyticsUtils.getMediaProperties(appContext, isVideo, uri, null)
        properties["via"] = if (isNewMedia) "device_camera" else "device_library"
        val stat = if (isVideo) Stat.MEDIA_LIBRARY_ADDED_VIDEO else Stat.MEDIA_LIBRARY_ADDED_PHOTO
        AnalyticsTracker.track(stat, properties)
    }

    private sealed interface Prepared {
        data class Ready(val media: MediaModel) : Prepared
        data class Failed(@param:StringRes val messageResId: Int) : Prepared
    }

    private companion object {
        const val UPLOAD_SOURCE_FOR_LOGGING = "MediaBrowserActivity#addMediaToUploadService"
    }
}
