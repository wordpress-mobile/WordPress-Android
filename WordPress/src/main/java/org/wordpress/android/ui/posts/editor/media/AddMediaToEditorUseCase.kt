package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.ProgressDialogUiState.HiddenProgressDialog
import org.wordpress.android.ui.posts.ProgressDialogUiState.VisibleProgressDialog
import org.wordpress.android.ui.posts.editor.EditorMedia.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.ui.posts.editor.media.AddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMediaIdle
import org.wordpress.android.ui.posts.editor.media.AddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMultipleMedia
import org.wordpress.android.ui.posts.editor.media.AddMediaToEditorUseCase.AddMediaToEditorUiState.AddingSingleMedia
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Processes a list of media in the background (optimizing, resizing, rotating, etc.) and adds them to
 * the editor one at a time.
 */
class AddMediaToEditorUseCase @Inject constructor(
    private val optimizeMediaUseCase: OptimizeMediaUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val editorTracker: EditorTracker,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = mainDispatcher + job

    private val _uiState: MutableLiveData<AddMediaToEditorUiState> = MutableLiveData()
    val uiState: LiveData<AddMediaToEditorUiState> = _uiState

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    init {
        _uiState.value = AddingMediaIdle
    }

    fun addMediaExistingInRemoteToEditorAsync(
        site: SiteModel,
        source: AddExistingMediaSource,
        mediaIdList: List<Long>,
        editorMediaListener: EditorMediaListener
    ) {
        launch {
            getMediaModelUseCase
                    .loadMediaModelFromDb(site, mediaIdList)
                    .onEach { media ->
                        editorTracker.trackAddMediaEvent(site, source, media.isVideo)
                    }
                    .apply {
                        addMediaToEditor(editorMediaListener, this)
                        editorMediaListener.savePostAsyncFromEditorMedia()
                    }
        }
    }

    fun dontOptimizeAndAddLocalMediaToEditorAsync(
        localMediaIds: List<Int>,
        editorMediaListener: EditorMediaListener
    ) {
        launch {
            getMediaModelUseCase.loadMediaModelFromDb(localMediaIds).let {
                addToEditorAndUpload(it, editorMediaListener)
            }
        }
    }

    fun optimizeAndAddLocalMediaToEditorAsync(
        uriList: List<Uri>,
        site: SiteModel,
        isNew: Boolean,
        editorMediaListener: EditorMediaListener
    ) {
        launch {
            _uiState.value = if (uriList.size > 1) {
                AddingMultipleMedia
            } else {
                AddingSingleMedia
            }
            val optimizeMediaResult = optimizeMediaUseCase.optimizeMediaAsync(site, isNew, uriList)
            val mediaModels = getMediaModelUseCase.createMediaModelFromUri(
                    site.id,
                    optimizeMediaResult.optimizedMediaUris
            )
            addToEditorAndUpload(mediaModels, editorMediaListener)

            if (optimizeMediaResult.someMediaCouldNotBeRetrieved) {
                _snackBarMessage.value = SnackbarMessageHolder(R.string.gallery_error)
            }

            _uiState.value = AddingMediaIdle
        }
    }

    private fun addToEditorAndUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        enqueueForUpload(mediaModels, editorMediaListener)
        addMediaToEditor(editorMediaListener, mediaModels)
        uploadMediaUseCase.savePostAndStartUpload(editorMediaListener, mediaModels)
    }

    private fun enqueueForUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        mediaModels.forEach {
            updateMediaModelUseCase.updateMediaModel(
                    it,
                    editorMediaListener.editorMediaPostData(),
                    QUEUED
            )
        }
    }

    private fun addMediaToEditor(
        editorMediaListener: EditorMediaListener,
        mediaModels: List<MediaModel>
    ) {
        mediaModels
                .mapNotNull { media ->
                    fluxCUtilsWrapper.mediaFileFromMediaModel(media)?.let { mediaFile ->
                        Pair(media.urlToUse, mediaFile)
                    }
                }.toMap(ArrayMap())
                .apply {
                    editorMediaListener.appendMediaFiles(this)
                }
    }

    fun cancel() {
        job.cancel()
    }

    sealed class AddMediaToEditorUiState(
        val editorOverlayVisibility: Boolean,
        val progressDialogUiState: ProgressDialogUiState
    ) {
        /**
         * Adding multiple media items at once can take several seconds on slower devices, so we show a blocking
         * progress dialog in this situation - otherwise the user could accidentally back out of the process
         * before all items were added
         */
        object AddingMultipleMedia : AddMediaToEditorUiState(
                editorOverlayVisibility = true,
                progressDialogUiState = VisibleProgressDialog(
                        messageString = UiStringRes(R.string.add_media_progress),
                        cancelable = false,
                        indeterminate = true
                )
        )

        object AddingSingleMedia : AddMediaToEditorUiState(true, HiddenProgressDialog)

        object AddingMediaIdle : AddMediaToEditorUiState(false, HiddenProgressDialog)
    }
}

private val MediaModel.urlToUse
    get() = if (url.isNullOrBlank()) filePath else url
