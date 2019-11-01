package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.net.Uri
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import kotlinx.coroutines.runBlocking
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity.AfterSavePostListener
import org.wordpress.android.ui.posts.editor.media.AddMediaToEditorUseCase
import org.wordpress.android.ui.posts.editor.media.AddMediaToEditorUseCase.AddMediaToEditorUiState
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.posts.editor.media.UpdateMediaModelUseCase
import org.wordpress.android.ui.posts.editor.media.UploadMediaUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.util.mergeNotNull
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import java.util.ArrayList
import javax.inject.Inject

data class EditorMediaPostData(val localPostId: Int, val remotePostId: Long, val isLocalDraft: Boolean)

interface EditorMediaListener {
    // TODO convert this into LiveData<Action> or similar and remove EditorMediaListener from all the places and send
    //  there only EditorMediaPostData
    fun appendMediaFiles(mediaMap: ArrayMap<String, MediaFile>)
    fun appendMediaFile(mediaFile: MediaFile, imageUrl: String)
    fun savePostAsyncFromEditorMedia(listener: AfterSavePostListener? = null)

    fun editorMediaPostData(): EditorMediaPostData
}

// TODO convert this into a view model
// TODO move this to media package
class EditorMedia @Inject constructor(
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val dispatcher: Dispatcher,
    private val mediaStore: MediaStore,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val addMediaToEditorUseCase: AddMediaToEditorUseCase
) {
    private lateinit var site: SiteModel
    private lateinit var editorMediaListener: EditorMediaListener

    val uiState: LiveData<AddMediaToEditorUiState> = addMediaToEditorUseCase.uiState
    val snackBarMessage: LiveData<SnackbarMessageHolder> = addMediaToEditorUseCase.snackBarMessage
    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage: LiveData<ToastMessageHolder> = mergeNotNull(
            listOf(
                    _toastMessage,
                    getMediaModelUseCase.toastMessage
            ),
            distinct = false,
            singleEvent = true
    )

    // for keeping the media uri while asking for permissions
    var droppedMediaUris: ArrayList<Uri>? = null

    // TODO refactor
    val fetchMediaRunnable = Runnable {
        droppedMediaUris?.let {
            droppedMediaUris = null
            optimizeIfSupportedAndAddMediaToEditor(it, false)
        }
    }

    enum class AddExistingMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

    fun start(site: SiteModel, editorMediaListener: EditorMediaListener) {
        this.site = site
        this.editorMediaListener = editorMediaListener
    }

    fun optimizeIfSupportedAndAddMediaToEditor(mediaUri: Uri?, isNew: Boolean): Boolean {
        mediaUri?.let {
            optimizeIfSupportedAndAddMediaToEditor(listOf(it), isNew)
            return true
        }
        return false
    }

    fun optimizeIfSupportedAndAddMediaToEditor(uriList: List<Uri>, isNew: Boolean) {
        // fetch any shared media first - must be done on the main thread
        val fetchedUriList = fetchMediaList(uriList)
        addMediaToEditorUseCase.optimizeIfSupportedAndAddLocalMediaToEditorAsync(
                fetchedUriList,
                site,
                isNew,
                editorMediaListener
        )
    }

    fun dontOptimizeAndAddLocalMediaToEditor(localMediaIds: IntArray) {
        addMediaToEditorUseCase.dontOptimizeAndAddLocalMediaToEditorAsync(localMediaIds.toList(), editorMediaListener)
    }

    fun cancelAddMediaToEditorActions() {
        // TODO The current behavior seems broken - we show a blocking dialog so the user can't cancel the action, but
        //  when the user rotates the device we actually cancel the action ourselves ...
        addMediaToEditorUseCase.cancel()
    }

    // TODO remove activity - it doesn't work with appContext!! The dialog needs to be created on a activity.
    fun advertiseImageOptimisationAndAddMedia(uriList: List<Uri>, activity: Activity) {
        if (mediaUtilsWrapper.shouldAdvertiseImageOptimization()) {
            mediaUtilsWrapper.advertiseImageOptimization(activity) {
                optimizeIfSupportedAndAddMediaToEditor(
                        uriList,
                        false
                )
            }
        } else {
            optimizeIfSupportedAndAddMediaToEditor(uriList, false)
        }
    }

    fun addExistingMediaToEditor(mediaModels: List<MediaModel>, source: AddExistingMediaSource) {
        addExistingMediaToEditor(source, mediaModels.map { it.mediaId })
    }

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaIds: LongArray) {
        addExistingMediaToEditor(source, mediaIds.toList())
    }

    fun prepareMediaPost(mediaIds: LongArray) {
        addExistingMediaToEditor(AddExistingMediaSource.WP_MEDIA_LIBRARY, mediaIds.toList())
    }

    fun addExistingMediaToEditor(source: AddExistingMediaSource, mediaIdList: List<Long>) {
        addMediaToEditorUseCase.addMediaExistingInRemoteToEditorAsync(site, source, mediaIdList, editorMediaListener)
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

    fun updateMediaUploadState(uri: Uri, mediaUploadState: MediaUploadState): MediaModel? {
        // TODO Remove runBlocking block
        return runBlocking {
            getMediaModelUseCase.createMediaModelFromUri(site.id, uri)?.let {
                updateMediaModelUseCase.updateMediaModel(
                        it,
                        editorMediaListener.editorMediaPostData(),
                        mediaUploadState
                )
                it
            }
        }
    }

    fun startUploadService(mediaList: List<MediaModel>) {
        uploadMediaUseCase.savePostAndStartUpload(editorMediaListener, mediaList)
    }

    fun addFreshlyTakenVideoToEditor() {
        optimizeIfSupportedAndAddMediaToEditor(listOf(mediaUtilsWrapper.getLastRecordedVideoUri()), true)
                .also { AnalyticsTracker.track(Stat.EDITOR_ADDED_VIDEO_NEW) }
    }

    fun onPhotoPickerMediaChosen(uriList: MutableList<Uri>, activity: Activity) {
        val containsAtLeastOneImage = uriList.map { MediaUtils.isVideo(it.toString()) }
                .any { false }
        if (containsAtLeastOneImage) {
            advertiseImageOptimisationAndAddMedia(uriList, activity)
        } else {
            optimizeIfSupportedAndAddMediaToEditor(uriList, false)
        }
    }

    fun retryFailedMedia(failedMediaIds: MutableSet<String>) {
        failedMediaIds
                .map { Integer.valueOf(it) }
                .let {
                    enqueueAndStartUpload(it)
                }
                .also { AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED) }
    }

    private fun enqueueAndStartUpload(mediaModelLocalIds: List<Int>) {
        // TODO remove this runBlocking
        runBlocking {
            getMediaModelUseCase
                    .loadMediaModelFromDb(mediaModelLocalIds)
                    .map { mediaModel ->
                        updateMediaModelUseCase
                                .updateMediaModel(mediaModel, editorMediaListener.editorMediaPostData(), QUEUED)
                        mediaModel
                    }.let { mediaModels ->
                        uploadMediaUseCase.savePostAndStartUpload(editorMediaListener, mediaModels)
                    }
        }
    }

    /*
    * called before we add media to make sure we have access to any media shared from another app (Google Photos, etc.)
    */
    private fun fetchMediaList(uriList: List<Uri>): List<Uri> {
        // TODO refactor
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
}
