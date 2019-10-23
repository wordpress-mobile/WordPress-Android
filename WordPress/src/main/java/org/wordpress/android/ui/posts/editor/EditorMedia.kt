package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity.AfterSavePostListener
import org.wordpress.android.ui.posts.EditPostActivity.NEW_MEDIA_POST_EXTRA_IDS
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.ListUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.MediaUtilsWrapper
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
import javax.inject.Named

data class EditorMediaPostData(val localPostId: Int, val remotePostId: Long, val isLocalDraft: Boolean)

interface EditorMediaListener {
    fun appendMediaFiles(mediaMap: ArrayMap<String, MediaFile>)
    fun appendMediaFile(mediaFile: MediaFile, imageUrl: String)
    fun editorMediaPostData(): EditorMediaPostData
    fun savePostAsyncFromEditorMedia(listener: AfterSavePostListener? = null)
}

class EditorMedia(
    private val activity: AppCompatActivity,
    private val site: SiteModel,
    private val editorMediaListener: EditorMediaListener,
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
    editorTracker: EditorTracker,
    mediaUtilsWrapper: MediaUtilsWrapper,
    fluxCUtilsWrapper: FluxCUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val addMediaToEditorUseCase: OptimizeAndAddMediaToEditorUseCase = OptimizeAndAddMediaToEditorUseCase(
            editorTracker,
            mediaUtilsWrapper,
            fluxCUtilsWrapper,
            mainDispatcher,
            bgDispatcher
    )
    val uiState: LiveData<AddMediaToEditorUiState> = addMediaToEditorUseCase.uiState
    val snackBarMessage: LiveData<SnackbarMessageHolder> = addMediaToEditorUseCase.snackBarMessage

    // for keeping the media uri while asking for permissions
    var droppedMediaUris: ArrayList<Uri>? = null
    val fetchMediaRunnable = Runnable {
        droppedMediaUris?.let {
            droppedMediaUris = null
            addMediaList(it, false)
        }
    }

    enum class AddExistingMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

    /**
     * Analytics about media already available in the blog's library.
     * @param source where the media is being added from
     * @param media media being added
     */
    private fun trackAddMediaEvent(source: AddExistingMediaSource, media: MediaModel) {
        val stat = when (source) {
            AddExistingMediaSource.WP_MEDIA_LIBRARY -> if (media.isVideo) {
                Stat.EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY
            } else {
                Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY
            }
            AddExistingMediaSource.STOCK_PHOTO_LIBRARY -> Stat.EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY
        }
        AnalyticsUtils.trackWithSiteDetails(stat, site, null)
    }

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
        addMediaToEditorUseCase.optimizeAndAddAsync(
                fetchedUriList,
                site,
                isNew,
                editorMediaListener,
                this@EditorMedia
        )
    }

    fun cancelAddMediaListThread() {
        // TODO The current behavior seems broken - we show a blocking dialog so the user can't cancel the action, but
        //  when the user rotates the device we actually cancel the action ourselves ...
        addMediaToEditorUseCase.cancel()
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
                    CrashLoggingUtils.logException(
                            e,
                            T.MEDIA,
                            "Can't download the image at: $mediaUri See issue #5823"
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

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaId: Long): Boolean {
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

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaIdList: List<Long>) {
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
    fun queueFileForUpload(uri: Uri): MediaModel? {
        return queueFileForUpload(uri, MediaUploadState.QUEUED)
    }

    fun queueFileForUpload(
        uri: Uri,
        startingState: MediaUploadState
    ): MediaModel? {
        val mimeType = activity.contentResolver.getType(uri)
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
        media.localPostId = editorMediaListener.editorMediaPostData().localPostId
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
        editorMediaListener.editorMediaPostData().let {
            if (!it.isLocalDraft) {
                media.postId = it.remotePostId
            }
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
            addExistingMediaToEditor(AddExistingMediaSource.WP_MEDIA_LIBRARY, id)
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
