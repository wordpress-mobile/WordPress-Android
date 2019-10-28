package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.ArrayMap
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
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
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import java.io.File
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
    private val editorTracker: EditorTracker,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val uploadServiceFacade: UploadServiceFacade,
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
    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage = _toastMessage as LiveData<ToastMessageHolder>

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
        val fetchedUriList = uriList.mapNotNull { mediaUri ->
            if (!mediaUtilsWrapper.isInMediaStore(mediaUri)) {
                // Do not download the file in async task. See
                // https://github.com/wordpress-mobile/WordPress-Android/issues/5818
                try {
                    return@mapNotNull mediaUtilsWrapper.downloadExternalMedia(mediaUri)
                } catch (e: IllegalStateException) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
                    val errorMessage = "Can't download the image at: $mediaUri See issue #5823"
                    AppLog.e(T.UTILS, errorMessage, e)
                    CrashLoggingUtils.logException(e, T.MEDIA, errorMessage)
                    return@mapNotNull null
                }
            } else {
                return@mapNotNull mediaUri
            }
        }

        if (fetchedUriList.size < uriList.size) {
            // At least one media failed
            _toastMessage.value = ToastMessageHolder(R.string.error_downloading_image, Duration.SHORT)
        }

        return fetchedUriList
    }

    fun addMediaItemGroupOrSingleItem(data: Intent) {
        val uriList: List<Uri> = data.clipData?.let { clipData ->
            (0 until clipData.itemCount).mapNotNull {
                clipData.getItemAt(it)?.uri
            }
        } ?: listOf(data.data)
        addMediaList(uriList, false)
    }

    fun advertiseImageOptimisationAndAddMedia(data: Intent) {
        if (mediaUtilsWrapper.shouldAdvertiseImageOptimization()) {
            mediaUtilsWrapper.advertiseImageOptimization { addMediaItemGroupOrSingleItem(data) }
        } else {
            addMediaItemGroupOrSingleItem(data)
        }
    }

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaId: Long): Boolean {
        return mediaStore.getSiteMediaWithId(site, mediaId)?.let { media ->
            editorTracker.trackAddMediaEvent(site, source, media.isVideo)
            fluxCUtilsWrapper.mediaFileFromMediaModel(media)?.let { mediaFile ->
                editorMediaListener.appendMediaFile(mediaFile, media.urlToUse)
            }
            true
        } ?: false.also { AppLog.w(T.MEDIA, "Cannot add null media to post") }
    }

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaIdList: List<Long>) {
        val mediaMap = mediaIdList.asSequence().mapNotNull { mediaId ->
            mediaStore.getSiteMediaWithId(site, mediaId)
        }.onEach { media ->
            editorTracker.trackAddMediaEvent(site, source, media.isVideo)
        }.mapNotNull { media ->
            fluxCUtilsWrapper.mediaFileFromMediaModel(media)?.let { mediaFile ->
                Pair(media.urlToUse, mediaFile)
            }
        }.toMap(ArrayMap())
        (mediaIdList.size - mediaMap.size).takeIf { it > 0 }?.let { failedMediaCount ->
            AppLog.w(T.MEDIA, "Failed to add $failedMediaCount media to post")
        }
        editorMediaListener.appendMediaFiles(mediaMap)
    }

    /**
     * Queues a media file for upload and starts the UploadService. Toasts will alert the user
     * if there are issues with the file.
     */
    fun queueFileForUpload(
        uri: Uri,
        startingState: MediaUploadState = MediaUploadState.QUEUED
    ): MediaModel? {
        val mimeType = mediaUtilsWrapper.getMimeType(uri)
        val path = mediaUtilsWrapper.getRealPathFromURI(uri)

        // Invalid file path
        if (TextUtils.isEmpty(path)) {
            _toastMessage.value = ToastMessageHolder(R.string.editor_toast_invalid_path, Duration.SHORT)
            return null
        }

        // File not found
        val file = File(path)
        if (!file.exists()) {
            _toastMessage.value = ToastMessageHolder(R.string.file_not_found, Duration.SHORT)
            return null
        }

        // we need to update media with the local post Id
        val media = buildMediaModel(uri, mimeType, startingState)
        if (media == null) {
            _toastMessage.value = ToastMessageHolder(R.string.file_not_found, Duration.SHORT)
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
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, mediaStore, site.id)
                ?.let { media ->
                    if (mediaUtilsWrapper.isVideoMimeType(media.mimeType)) {
                        val path = mediaUtilsWrapper.getRealPathFromURI(uri)
                        media.thumbnailUrl = EditorMediaUtils.getVideoThumbnail(activity, path)
                    }

                    media.setUploadState(startingState)
                    editorMediaListener.editorMediaPostData().let {
                        if (!it.isLocalDraft) {
                            media.postId = it.remotePostId
                        }
                    }
                    media
                }
    }

    fun prepareMediaPost(mediaIds: LongArray) {
        mediaIds.forEach { id ->
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
            uploadServiceFacade.uploadMediaFromEditor(ArrayList(queuedMediaModels))
        })
    }

    fun cancelMediaUpload(localMediaId: Int, delete: Boolean) {
        mediaStore.getMediaWithLocalId(localMediaId)?.let { mediaModel ->
            val payload = CancelMediaPayload(site, mediaModel, delete)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
        }
    }

    fun refreshBlogMedia() {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            val payload = FetchMediaListPayload(site, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false)
            dispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        } else {
            _toastMessage.value = ToastMessageHolder(R.string.error_media_refresh_no_connection, Duration.SHORT)
        }
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
