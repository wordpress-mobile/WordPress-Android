package org.wordpress.android.ui.stories.media

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_FAILED
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.ProgressDialogUiState.HiddenProgressDialog
import org.wordpress.android.ui.posts.ProgressDialogUiState.VisibleProgressDialog
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.AddLocalMediaToPostUseCase
import org.wordpress.android.ui.posts.editor.media.CleanUpMediaToPostAssociationUseCase
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.posts.editor.media.ReattachUploadingMediaUseCase
import org.wordpress.android.ui.posts.editor.media.RemoveMediaUseCase
import org.wordpress.android.ui.posts.editor.media.RetryFailedMediaUploadUseCase
import org.wordpress.android.ui.posts.editor.media.UpdateMediaModelUseCase
import org.wordpress.android.ui.stories.media.StoryEditorMedia.AddMediaToStoryPostUiState.AddingMediaToStoryIdle
import org.wordpress.android.ui.stories.media.StoryEditorMedia.AddMediaToStoryPostUiState.AddingMultipleMediaToStory
import org.wordpress.android.ui.stories.media.StoryEditorMedia.AddMediaToStoryPostUiState.AddingSingleMediaToStory
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class StoryEditorMedia @Inject constructor(
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val dispatcher: Dispatcher,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val addLocalMediaToPostUseCase: AddLocalMediaToPostUseCase,
    private val addExistingMediaToPostUseCase: AddExistingMediaToPostUseCase,
    private val retryFailedMediaUploadUseCase: RetryFailedMediaUploadUseCase,
    private val cleanUpMediaToPostAssociationUseCase: CleanUpMediaToPostAssociationUseCase,
    private val removeMediaUseCase: RemoveMediaUseCase,
    private val reattachUploadingMediaUseCase: ReattachUploadingMediaUseCase,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    // region Fields
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private lateinit var site: SiteModel
    private lateinit var editorMediaListener: EditorMediaListener

    private val deletedMediaItemIds = mutableListOf<String>()

    private val _uiState: MutableLiveData<AddMediaToStoryPostUiState> = MutableLiveData()
    val uiState: LiveData<AddMediaToStoryPostUiState> = _uiState

    private val _snackBarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val snackBarMessage = _snackBarMessage as LiveData<Event<SnackbarMessageHolder>>

    private val _toastMessage = SingleLiveEvent<Event<ToastMessageHolder>>()
    val toastMessage: LiveData<Event<ToastMessageHolder>> = _toastMessage

    fun start(site: SiteModel, editorMediaListener: EditorMediaListener) {
        this.site = site
        this.editorMediaListener = editorMediaListener
        _uiState.value = AddingMediaToStoryIdle
    }

    // region Adding new media to a post
    fun advertiseImageOptimisationAndAddMedia(uriList: List<Uri>) {
        if (mediaUtilsWrapper.shouldAdvertiseImageOptimization()) {
            editorMediaListener.advertiseImageOptimization {
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
                AddingMultipleMediaToStory
            } else {
                AddingSingleMediaToStory
            }
            val allMediaSucceed = addLocalMediaToPostUseCase.addNewMediaToEditorAsync(
                    uriList,
                    site,
                    freshlyTaken,
                    editorMediaListener,
                    false // don't start upload for StoryComposer, that'll be all started
                                            // when finished composing
            )
            if (!allMediaSucceed) {
                _snackBarMessage.value = Event(SnackbarMessageHolder(R.string.gallery_error))
            }
            _uiState.value = AddingMediaToStoryIdle
        }
    }

    /**
     * This won't create a MediaModel. It assumes the model was already created.
     */
    fun addGifMediaToPostAsync(localMediaIds: IntArray) {
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

    fun onPhotoPickerMediaChosen(uriList: List<Uri>) {
        val onlyVideos = uriList.all { mediaUtilsWrapper.isVideo(it.toString()) }
        if (onlyVideos) {
            addNewMediaItemsToEditorAsync(uriList, false)
        } else {
            advertiseImageOptimisationAndAddMedia(uriList)
        }
    }
    // endregion

    // region Add existing media to a post
    fun addExistingMediaToEditorAsync(
        mediaModels: List<MediaModel>,
        source: AddExistingMediaSource
    ) {
        addExistingMediaToEditorAsync(source, mediaModels.map { it.mediaId })
    }

    fun addExistingMediaToEditorAsync(source: AddExistingMediaSource, mediaIds: LongArray) {
        addExistingMediaToEditorAsync(source, mediaIds.toList())
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
    // endregion

    // region Other
    fun cancelMediaUploadAsync(localMediaId: Int, delete: Boolean) {
        launch {
            getMediaModelUseCase
                    .loadMediaByLocalId(listOf(localMediaId))
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
            _toastMessage.value = Event(
                    ToastMessageHolder(
                            R.string.error_media_refresh_no_connection,
                            Duration.SHORT
                    )
            )
        }
    }

    @Deprecated(message = "Blocking method shouldn't be used in new code.")
    fun updateMediaUploadStateBlocking(uri: Uri, mediaUploadState: MediaUploadState): MediaModel? {
        return runBlocking {
            getMediaModelUseCase.createMediaModelFromUri(site.id, uri).mediaModels.firstOrNull()
                    ?.let {
                        updateMediaModelUseCase.updateMediaModel(
                                it,
                                editorMediaListener.getImmutablePost(),
                                mediaUploadState
                        )
                        it
                    }
        }
    }

    fun retryFailedMediaAsync(failedMediaIds: List<Int>) {
        launch {
            retryFailedMediaUploadUseCase.retryFailedMediaAsync(editorMediaListener, failedMediaIds)
        }
    }

    fun purgeMediaToPostAssociationsIfNotInPostAnymoreAsync() {
        launch {
            cleanUpMediaToPostAssociationUseCase
                    .purgeMediaToPostAssociationsIfNotInPostAnymore(editorMediaListener.getImmutablePost())
        }
    }

    fun reattachUploadingMediaForAztec(
        editPostRepository: EditPostRepository,
        isAztec: Boolean,
        editorMediaUploadListener: EditorMediaUploadListener
    ) {
        if (isAztec) {
            reattachUploadingMediaUseCase.reattachUploadingMediaForAztec(
                    editPostRepository,
                    editorMediaUploadListener
            )
        }
    }

    /*
    * When the user deletes a media item that was being uploaded at that moment, we only cancel the
    * upload but keep the media item in FluxC DB because the user might have deleted it accidentally,
    * and they can always UNDO the delete action in Aztec.
    * So, when the user exits then editor (and thus we lose the undo/redo history) we are safe to
    * physically delete from the FluxC DB those items that have been deleted by the user using backspace.
    * */
    fun definitelyDeleteBackspaceDeletedMediaItemsAsync() {
        launch {
            removeMediaUseCase.removeMediaIfNotUploading(deletedMediaItemIds)
        }
    }

    fun updateDeletedMediaItemIds(localMediaId: String) {
        deletedMediaItemIds.add(localMediaId)
        UploadService.setDeletedMediaItemIds(deletedMediaItemIds)
    }

    // endregion

    fun cancelAddMediaToEditorActions() {
        job.cancel()
    }

    fun onMediaDeleted(
        showAztecEditor: Boolean,
        showGutenbergEditor: Boolean,
        localMediaId: String
    ) {
        updateDeletedMediaItemIds(localMediaId)
        if (showAztecEditor && !showGutenbergEditor) {
            // passing false here as we need to keep the media item in case the user wants to undo
            cancelMediaUploadAsync(StringUtils.stringToInt(localMediaId), false)
        } else {
            launch {
                removeMediaUseCase.removeMediaIfNotUploading(listOf(localMediaId))
            }
        }
    }

    fun onMediaUploadError(listener: EditorMediaUploadListener, media: MediaModel, error: MediaError) = launch {
        val properties: Map<String, Any?> = withContext(bgDispatcher) {
            analyticsUtilsWrapper
                    .getMediaProperties(media.isVideo, null, media.filePath)
                    .also {
                        it["error_type"] = error.type.name
                    }
        }
        analyticsTrackerWrapper.track(EDITOR_UPLOAD_MEDIA_FAILED, properties)
        listener.onMediaUploadFailed(media.id.toString())
    }

    sealed class AddMediaToStoryPostUiState(
        val editorOverlayVisibility: Boolean,
        val progressDialogUiState: ProgressDialogUiState
    ) {
        /**
         * Adding multiple media items at once can take several seconds on slower devices, so we show a blocking
         * progress dialog in this situation - otherwise the user could accidentally back out of the process
         * before all items were added
         */
        object AddingMultipleMediaToStory : AddMediaToStoryPostUiState(
                editorOverlayVisibility = true,
                progressDialogUiState = VisibleProgressDialog(
                        messageString = UiStringRes(R.string.add_media_progress),
                        cancelable = false,
                        indeterminate = true
                )
        )

        object AddingSingleMediaToStory : AddMediaToStoryPostUiState(true, HiddenProgressDialog)

        object AddingMediaToStoryIdle : AddMediaToStoryPostUiState(false, HiddenProgressDialog)
    }
}
