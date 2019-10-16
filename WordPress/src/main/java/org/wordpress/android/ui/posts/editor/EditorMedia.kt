package org.wordpress.android.ui.posts.editor

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.EditorMediaUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.ui.posts.EditPostActivity.NEW_MEDIA_POST_EXTRA_IDS
import org.wordpress.android.ui.posts.EditPostActivity.AfterSavePostListener
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.helpers.MediaFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

interface EditorMediaListener {
    // Temporary overlay functions
    fun showOverlayFromEditorMedia(animate: Boolean)

    fun hideOverlayFromEditorMedia()
    fun appendMediaFiles(mediaMap: ArrayMap<String, MediaFile>)
    fun appendMediaFile(mediaFile: MediaFile, imageUrl: String)
    fun isPostLocalDraft(): Boolean
    fun localPostId(): Int
    fun remotePostId(): Long
    fun savePostAsyncFromEditorMedia(listener: AfterSavePostListener? = null)
}

class EditorMedia(
    private val activity: AppCompatActivity,
    private val site: SiteModel,
    private val editorMediaListener: EditorMediaListener,
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore
) {
    private var mAddMediaListThread: AddMediaListThread? = null
    private var mAllowMultipleSelection: Boolean = false

    enum class AddExistingdMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

    /**
     * Analytics about media from device
     *
     * @param isNew Whether is a fresh media
     * @param isVideo Whether is a video or not
     * @param uri The URI of the media on the device, or null
     */
    fun trackAddMediaFromDeviceEvents(isNew: Boolean, isVideo: Boolean, uri: Uri?) {
        if (uri == null) {
            AppLog.e(T.MEDIA, "Cannot track new media events if both path and mediaURI are null!!")
            return
        }

        val properties = AnalyticsUtils.getMediaProperties(activity, isVideo, uri, null)
        val currentStat: Stat = if (isVideo) {
            if (isNew) {
                Stat.EDITOR_ADDED_VIDEO_NEW
            } else {
                Stat.EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY
            }
        } else {
            if (isNew) {
                Stat.EDITOR_ADDED_PHOTO_NEW
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY
            }
        }

        AnalyticsUtils.trackWithSiteDetails(currentStat, site, properties)
    }

    /**
     * Analytics about media already available in the blog's library.
     * @param source where the media is being added from
     * @param media media being added
     */
    private fun trackAddMediaEvent(source: AddExistingdMediaSource, media: MediaModel) {
        val stat = when (source) {
            AddExistingdMediaSource.WP_MEDIA_LIBRARY -> if (media.isVideo) {
                Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY
            }
            AddExistingdMediaSource.STOCK_PHOTO_LIBRARY -> Stat.EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY
        }
        AnalyticsUtils.trackWithSiteDetails(
                stat,
                site,
                null
        )
    }

    @Suppress("SameParameterValue")
    fun addMedia(mediaUri: Uri?, isNew: Boolean): Boolean {
        mediaUri?.let {
            addMediaList(listOf(it), isNew)
            return true
        }
        return false
    }

    fun addMediaList(uriList: List<Uri>, isNew: Boolean) {
        // fetch any shared media first - must be done on the main thread
        val fetchedUriList = fetchMediaList(uriList)
        mAddMediaListThread = AddMediaListThread(fetchedUriList, isNew, mAllowMultipleSelection)
        mAddMediaListThread!!.start()
    }

    fun cancelAddMediaListThread() {
        if (mAddMediaListThread != null && !mAddMediaListThread!!.isInterrupted) {
            try {
                mAddMediaListThread!!.interrupt()
            } catch (e: SecurityException) {
                AppLog.e(T.MEDIA, e)
            }
        }
    }

    /*
     * called before we add media to make sure we have access to any media shared from another app (Google Photos, etc.)
     */
    private fun fetchMediaList(uriList: List<Uri>): List<Uri> {
        var didAnyFail = false
        val fetchedUriList = ArrayList<Uri>()
        for (mediaUri in uriList) {
            if (!MediaUtils.isInMediaStore(mediaUri)) {
                // Do not download the file in async task. See
                // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
                var fetchedUri: Uri? = null
                try {
                    fetchedUri = MediaUtils.downloadExternalMedia(activity, mediaUri)
                } catch (e: IllegalStateException) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                    AppLog.e(T.UTILS, "Can't download the image at: $mediaUri", e)
                    CrashLoggingUtils
                            .logException(
                                    e,
                                    T.MEDIA,
                                    "Can't download the image at: " + mediaUri.toString()
                                            + " See issue #5823"
                            )
                    didAnyFail = true
                }

                if (fetchedUri != null) {
                    fetchedUriList.add(fetchedUri)
                } else {
                    didAnyFail = true
                }
            } else {
                fetchedUriList.add(mediaUri)
            }
        }

        if (didAnyFail) {
            ToastUtils.showToast(activity, R.string.error_downloading_image, Duration.SHORT)
        }

        return fetchedUriList
    }

    /*
     * processes a list of media in the background (optimizing, resizing, etc.) and adds them to
     * the editor one at a time
     */
    private inner class AddMediaListThread(
        private val uriList: List<Uri>,
        private val isNew: Boolean,
        private val allowMultipleSelection: Boolean = false
    ) : Thread() {
        @Suppress("DEPRECATION")
        private var mProgressDialog: ProgressDialog? = null
        private var mDidAnyFail: Boolean = false
        private var mFinishedUploads = 0
        private val mediaMap = ArrayMap<String, MediaFile>()

        init {
            editorMediaListener.showOverlayFromEditorMedia(false)
        }

        private fun showProgressDialog(show: Boolean) {
            activity.runOnUiThread {
                try {
                    if (show) {
                        mProgressDialog = ProgressDialog(activity)
                        mProgressDialog!!.setCancelable(false)
                        mProgressDialog!!.isIndeterminate = true
                        mProgressDialog!!.setMessage(activity.getString(R.string.add_media_progress))
                        mProgressDialog!!.show()
                    } else if (mProgressDialog != null && mProgressDialog!!.isShowing) {
                        mProgressDialog!!.dismiss()
                    }
                } catch (e: IllegalArgumentException) {
                    AppLog.e(T.MEDIA, e)
                }
            }
        }

        override fun run() {
            // adding multiple media items at once can take several seconds on slower devices, so we show a blocking
            // progress dialog in this situation - otherwise the user could accidentally back out of the process
            // before all items were added
            val shouldShowProgress = uriList.size > 2
            if (shouldShowProgress) {
                showProgressDialog(true)
            }
            try {
                for (mediaUri in uriList) {
                    if (isInterrupted) {
                        return
                    }
                    if (!processMedia(mediaUri)) {
                        mDidAnyFail = true
                    }
                }
            } finally {
                if (shouldShowProgress) {
                    showProgressDialog(false)
                }
            }


            activity.runOnUiThread {
                if (!isInterrupted) {
                    editorMediaListener.savePostAsyncFromEditorMedia()
                    editorMediaListener.hideOverlayFromEditorMedia()
                    if (mDidAnyFail) {
                        ToastUtils.showToast(activity, R.string.gallery_error, Duration.SHORT)
                    }
                }
            }
        }

        private fun processMedia(mediaUri: Uri): Boolean {
            val path = MediaUtils.getRealPathFromURI(activity, mediaUri) ?: return false

            val isVideo = MediaUtils.isVideo(mediaUri.toString())
            val optimizedMedia = WPMediaUtils.getOptimizedMedia(activity, path, isVideo)
            var updatedMediaUri: Uri = mediaUri
            if (optimizedMedia != null) {
                updatedMediaUri = optimizedMedia
            } else {
                // Fix for the rotation issue https://github.com/wordpress-mobile/WordPress-Android/issues/5737
                if (!site.isWPCom) {
                    // If it's not wpcom we must rotate the picture locally
                    val rotatedMedia = WPMediaUtils.fixOrientationIssue(activity, path, isVideo)
                    if (rotatedMedia != null) {
                        updatedMediaUri = rotatedMedia
                    }
                }
            }

            if (isInterrupted) {
                return false
            }

            trackAddMediaFromDeviceEvents(isNew, isVideo, updatedMediaUri)
            postProcessMedia(updatedMediaUri, path)

            return true
        }

        private fun postProcessMedia(mediaUri: Uri, path: String) {
            if (allowMultipleSelection) {
                getMediaFile(mediaUri)?.let {
                    mediaMap[path] = it
                }
                mFinishedUploads++
                if (uriList.size == mFinishedUploads) {
                    activity.runOnUiThread { editorMediaListener.appendMediaFiles(mediaMap) }
                }
            } else {
                activity.runOnUiThread { addMediaVisualEditor(mediaUri, path) }
            }
        }
    }

    private fun addMediaVisualEditor(uri: Uri, path: String) {
        val mediaFile = getMediaFile(uri)
        if (mediaFile != null) {
            editorMediaListener.appendMediaFile(mediaFile, path)
        }
    }

    private fun getMediaFile(uri: Uri): MediaFile? {
        val media = queueFileForUpload(uri, activity.contentResolver.getType(uri))
        val mediaFile = FluxCUtils.mediaFileFromMediaModel(media)
        return if (media != null) {
            mediaFile
        } else {
            null
        }
    }

    fun addMediaItemGroupOrSingleItem(data: Intent) {
        val clipData = data.clipData
        if (clipData != null) {
            val uriList = ArrayList<Uri>()
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)
                uriList.add(item.uri)
            }
            addMediaList(uriList, false)
        } else {
            addMedia(data.data, false)
        }
    }

    fun advertiseImageOptimisationAndAddMedia(data: Intent) {
        if (WPMediaUtils.shouldAdvertiseImageOptimization(activity)) {
            WPMediaUtils.advertiseImageOptimization(
                    activity
            ) { addMediaItemGroupOrSingleItem(data) }
        } else {
            addMediaItemGroupOrSingleItem(data)
        }
    }

    fun addExistingMediaToEditor(source: AddExistingdMediaSource, mediaId: Long): Boolean {
        val media = mediaStore.getSiteMediaWithId(site, mediaId)
        if (media == null) {
            AppLog.w(T.MEDIA, "Cannot add null media to post")
            return false
        }

        trackAddMediaEvent(source, media)

        val mediaFile = FluxCUtils.mediaFileFromMediaModel(media)
        editorMediaListener.appendMediaFile(mediaFile, media.urlToUse)
        return true
    }

    fun addExistingMediaToEditor(source: AddExistingdMediaSource, mediaIdList: List<Long>) {
        val mediaMap = ArrayMap<String, MediaFile>()
        for (mediaId in mediaIdList) {
            val media = mediaStore.getSiteMediaWithId(site, mediaId)
            if (media == null) {
                AppLog.w(T.MEDIA, "Cannot add null media to post")
            } else {
                trackAddMediaEvent(source, media)

                mediaMap[media.urlToUse] = FluxCUtils.mediaFileFromMediaModel(media)
            }
        }
        editorMediaListener.appendMediaFiles(mediaMap)
    }

    /**
     * Queues a media file for upload and starts the UploadService. Toasts will alert the user
     * if there are issues with the file.
     */
    private fun queueFileForUpload(uri: Uri, mimeType: String?): MediaModel? {
        return queueFileForUpload(uri, mimeType, MediaUploadState.QUEUED)
    }

    @Suppress("SameParameterValue")
    fun queueFileForUpload(
        uri: Uri,
        mimeType: String?,
        startingState: MediaUploadState
    ): MediaModel? {
        val path = MediaUtils.getRealPathFromURI(activity, uri)

        // Invalid file path
        if (TextUtils.isEmpty(path)) {
            ToastUtils.showToast(activity, R.string.editor_toast_invalid_path, Duration.SHORT)
            return null
        }

        // File not found
        val file = File(path)
        if (!file.exists()) {
            ToastUtils.showToast(activity, R.string.file_not_found, Duration.SHORT)
            return null
        }

        // we need to update media with the local post Id
        val media = buildMediaModel(uri, mimeType, startingState)
        if (media == null) {
            ToastUtils.showToast(activity, R.string.file_not_found, Duration.SHORT)
            return null
        }
        media.localPostId = editorMediaListener.localPostId()
        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))

        startUploadService(listOf(media))

        return media
    }

    private fun buildMediaModel(
        uri: Uri,
        mimeType: String?,
        startingState: MediaUploadState
    ): MediaModel? {
        val media = FluxCUtils.mediaModelFromLocalUri(
                activity,
                uri,
                mimeType,
                mediaStore,
                site.id
        ) ?: return null
        if (org.wordpress.android.fluxc.utils.MediaUtils.isVideoMimeType(media.mimeType)) {
            val path = MediaUtils.getRealPathFromURI(activity, uri)
            media.thumbnailUrl = getVideoThumbnail(path)
        }

        media.setUploadState(startingState)
        if (!editorMediaListener.isPostLocalDraft()) {
            media.postId = editorMediaListener.remotePostId()
        }

        return media
    }

    private fun getVideoThumbnail(videoPath: String): String? {
        var thumbnailPath: String? = null
        try {
            val outputFile = File.createTempFile("thumb", ".png", activity.cacheDir)
            val outputStream = FileOutputStream(outputFile)
            val thumb = ImageUtils.getVideoFrameFromVideo(
                    videoPath,
                    EditorMediaUtils.getMaximumThumbnailSizeForEditor(activity)
            )
            if (thumb != null) {
                thumb.compress(Bitmap.CompressFormat.PNG, 75, outputStream)
                thumbnailPath = outputFile.absolutePath
            }
        } catch (e: IOException) {
            AppLog.i(T.MEDIA, "Can't create thumbnail for video: $videoPath")
        }

        return thumbnailPath
    }

    fun prepareMediaPost() {
        val idsArray = activity.intent.getLongArrayExtra(NEW_MEDIA_POST_EXTRA_IDS)
        ListUtils.fromLongArray(idsArray)?.forEach { id ->
            addExistingMediaToEditor(AddExistingdMediaSource.WP_MEDIA_LIBRARY, id)
        }
        editorMediaListener.savePostAsyncFromEditorMedia()
    }

    /**
     * Start the [UploadService] to upload the given `mediaModels`.
     *
     * Only [MediaModel] objects that have `MediaUploadState.QUEUED` statuses will be uploaded. .
     */
    fun startUploadService(mediaModels: List<MediaModel>) {
        // make sure we only pass items with the QUEUED state to the UploadService
        val queuedMediaModels = mediaModels.filter { media ->
            MediaUploadState.fromString(media.uploadState) == MediaUploadState.QUEUED
        }

        // before starting the service, we need to update the posts' contents so we are sure the service
        // can retrieve it from there on
        editorMediaListener.savePostAsyncFromEditorMedia(AfterSavePostListener {
            UploadService.uploadMediaFromEditor(
                    activity,
                    ArrayList(queuedMediaModels)
            )
        })
    }

    fun cancelMediaUpload(localMediaId: Int, delete: Boolean) {
        val mediaModel = mediaStore.getMediaWithLocalId(localMediaId)
        if (mediaModel != null) {
            val payload = CancelMediaPayload(site, mediaModel, delete)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
        }
    }

    fun refreshBlogMedia() {
        if (NetworkUtils.isNetworkAvailable(activity)) {
            val payload = FetchMediaListPayload(site, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false)
            dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        } else {
            ToastUtils.showToast(
                    activity,
                    R.string.error_media_refresh_no_connection,
                    Duration.SHORT
            )
        }
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
