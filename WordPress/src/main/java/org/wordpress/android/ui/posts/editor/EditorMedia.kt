package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.net.Uri
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostActivity.AfterSavePostListener
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.ProgressDialogUiState.HiddenProgressDialog
import org.wordpress.android.ui.posts.ProgressDialogUiState.VisibleProgressDialog
import org.wordpress.android.ui.posts.editor.EditorMedia.AddMediaToPostUiState.AddingMediaIdle
import org.wordpress.android.ui.posts.editor.EditorMedia.AddMediaToPostUiState.AddingMultipleMedia
import org.wordpress.android.ui.posts.editor.EditorMedia.AddMediaToPostUiState.AddingSingleMedia
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.posts.editor.media.UpdateMediaModelUseCase
import org.wordpress.android.ui.posts.editor.media.UploadMediaUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
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
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

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
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase,
    private val addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private lateinit var site: SiteModel
    private lateinit var editorMediaListener: EditorMediaListener

    private val _uiState: MutableLiveData<AddMediaToPostUiState> = MutableLiveData()
    val uiState: LiveData<AddMediaToPostUiState> = _uiState

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>() // TODO use Event<ToastMessageHolder>
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
            addNewMediaItemsToEditorAsync(it, freshlyTaken = false)
        }
    }

    enum class AddExistingMediaSource {
        WP_MEDIA_LIBRARY,
        STOCK_PHOTO_LIBRARY
    }

    fun start(site: SiteModel, editorMediaListener: EditorMediaListener) {
        this.site = site
        this.editorMediaListener = editorMediaListener
        _uiState.value = AddingMediaIdle
    }


    //region Adding new media to a post
    fun advertiseImageOptimisationAndAddMedia(uriList: List<Uri>, activity: Activity) {
        // TODO remove activity - it doesn't work with appContext!! The dialog needs to be created on a activity.
        if (mediaUtilsWrapper.shouldAdvertiseImageOptimization()) {
            mediaUtilsWrapper.advertiseImageOptimization(activity) {
                addNewMediaItemsToEditorAsync(
                        uriList,
                        false
                )
            }
        } else {
            addNewMediaItemsToEditorAsync(uriList, false)
        }
    }

    fun addNewMediaToEditorAsync(mediaUri: Uri, freshlyTaken: Boolean) {
        addNewMediaItemsToEditorAsync(listOf(mediaUri), freshlyTaken)
    }

    fun addNewMediaItemsToEditorAsync(uriList: List<Uri>, freshlyTaken: Boolean) {
        launch {
            _uiState.value = if (uriList.size > 1) {
                AddingMultipleMedia
            } else {
                AddingSingleMedia
            }
            // fetch any shared media first - must be done on the main thread
            val fetchedUriList = fetchMediaList(uriList)
            val allMediaSucceed = addLocalMediaToPostUseCase.addNewMediaToEditorAsync(
                    fetchedUriList,
                    site,
                    freshlyTaken,
                    editorMediaListener
            )
            if(!allMediaSucceed) {
                _snackBarMessage.value = SnackbarMessageHolder(R.string.gallery_error)
            }
            _uiState.value = AddingMediaIdle
        }
    }

    /**
     * This won't create a MediaModel. It assumes the model was already created.
     */
    fun addMediaFromGiphyToPostAsync(localMediaIds: IntArray) {
        launch {
            addLocalMediaToPostUseCase.addLocalMediaToEditorAsync(
                    localMediaIds.toList(),
                    editorMediaListener
            )
        }
    }

    fun addFreshlyTakenVideoToEditor() {
        addNewMediaItemsToEditorAsync(listOf(mediaUtilsWrapper.getLastRecordedVideoUri()), true)
                .also { AnalyticsTracker.track(Stat.EDITOR_ADDED_VIDEO_NEW) }
    }

    fun onPhotoPickerMediaChosen(uriList: MutableList<Uri>, activity: Activity) {
        val containsAtLeastOneImage = uriList.any { MediaUtils.isVideo(it.toString()) }
        if (containsAtLeastOneImage) {
            advertiseImageOptimisationAndAddMedia(uriList, activity)
        } else {
            addNewMediaItemsToEditorAsync(uriList, false)
        }
    }
    //endregion


    //region Add existing media to a post
    fun addExistingMediaToEditorAsync(mediaModels: List<MediaModel>, source: AddExistingMediaSource) {
        addExistingMediaToEditorAsync(source, mediaModels.map { it.mediaId })
    }

    fun addExistingMediaToEditorAsync(source: AddExistingMediaSource, mediaIds: LongArray) {
        addExistingMediaToEditorAsync(source, mediaIds.toList())
    }

    fun prepareMediaPost(mediaIds: LongArray) {
        addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, mediaIds.toList())
    }

    fun addExistingMediaToEditorAsync(source: AddExistingMediaSource, mediaIdList: List<Long>) {
        launch {
            addExistingMediaToPostUseCase.addMediaExistingInRemoteToEditorAsync(
                    site,
                    source,
                    mediaIdList,
                    editorMediaListener
            )
        }
    }
    //endregion

    fun cancelAddMediaToEditorActions() {
        // TODO The current behavior seems broken - we show a blocking dialog so the user can't cancel the action, but
        //  when the user rotates the device we actually cancel the action ourselves ...
        job.cancel()
    }

    fun cancelMediaUploadAsync(localMediaId: Int, delete: Boolean) {
        launch {
            getMediaModelUseCase
                    .loadMediaModelFromDb(listOf(localMediaId))
                    .firstOrNull()
                    ?.let { mediaModel ->
                        val payload = CancelMediaPayload(site, mediaModel, delete)
                        dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
                    }
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
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, mediaList)
    }

    fun retryFailedMediaAsync(failedMediaIds: MutableSet<String>) {
        failedMediaIds
                .map { Integer.valueOf(it) }
                .let {
                    enqueueAndStartUploadAsync(it)
                }
                .also { AnalyticsTracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED) }
    }

    private fun enqueueAndStartUploadAsync(mediaModelLocalIds: List<Int>) {
        launch {
            getMediaModelUseCase
                    .loadMediaModelFromDb(mediaModelLocalIds)
                    .map { mediaModel ->
                        updateMediaModelUseCase
                                .updateMediaModel(mediaModel, editorMediaListener.editorMediaPostData(), QUEUED)
                        mediaModel
                    }.let { mediaModels ->
                        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, mediaModels)
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

    sealed class AddMediaToPostUiState(
        val editorOverlayVisibility: Boolean,
        val progressDialogUiState: ProgressDialogUiState
    ) {
        /**
         * Adding multiple media items at once can take several seconds on slower devices, so we show a blocking
         * progress dialog in this situation - otherwise the user could accidentally back out of the process
         * before all items were added
         */
        object AddingMultipleMedia : AddMediaToPostUiState(
                editorOverlayVisibility = true,
                progressDialogUiState = VisibleProgressDialog(
                        messageString = UiStringRes(R.string.add_media_progress),
                        cancelable = false,
                        indeterminate = true
                )
        )

        object AddingSingleMedia : AddMediaToPostUiState(true, HiddenProgressDialog)

        object AddingMediaIdle : AddMediaToPostUiState(false, HiddenProgressDialog)
    }
}
