package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import android.util.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.posts.ProgressDialogUiState.HiddenProgressDialog
import org.wordpress.android.ui.posts.ProgressDialogUiState.VisibleProgressDialog
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMediaIdle
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingMultipleMedia
import org.wordpress.android.ui.posts.editor.media.OptimizeAndAddMediaToEditorUseCase.AddMediaToEditorUiState.AddingSingleMedia
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Processes a list of media in the background (optimizing, resizing, rotating, etc.) and adds them to
 * the editor one at a time.
 */
class OptimizeAndAddMediaToEditorUseCase @Inject constructor(
    private val editorTracker: EditorTracker,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val uploadMediaUseCase: UploadMediaUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
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

    fun optimizeAndAddAsync(
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

            val optimizeMediaResult = optimizeMediaAsync(site, isNew, uriList)
            val enqueuedFiles = enqueueMediaForUpload(
                    site.id,
                    optimizeMediaResult.optimizedMediaUris,
                    editorMediaListener
            )
            addMediaToEditor(editorMediaListener, enqueuedFiles)

            editorMediaListener.savePostAsyncFromEditorMedia()

            if (optimizeMediaResult.someMediaCouldNotBeRetrieved) {
                _snackBarMessage.value = SnackbarMessageHolder(R.string.gallery_error)
            }

            _uiState.value = AddingMediaIdle
        }
    }

    private suspend fun optimizeMediaAsync(
        site: SiteModel,
        isNew: Boolean,
        uriList: List<Uri>
    ): OptimizeMediaResult {
        return withContext(bgDispatcher) {
            uriList
                    .map { async { optimizeMediaAndTrackEvent(it, isNew, site) } }
                    .map { it.await() }
                    .toList()
                    .let {
                        OptimizeMediaResult(
                                optimizedMediaUris = it.filterNotNull(),
                                someMediaCouldNotBeRetrieved = it.contains(null)
                        )
                    }
        }
    }

    private suspend fun enqueueMediaForUpload(
        localSiteId: Int,
        uris: List<Uri>,
        editorMediaListener: EditorMediaListener
    ): List<MediaFile> {
        return uris.mapNotNull { uri -> enqueueMediaForUpload(localSiteId, uri, editorMediaListener) }
    }

    private fun addMediaToEditor(
        editorMediaListener: EditorMediaListener,
        mediaFiles: List<MediaFile>
    ) {
        mediaFiles
                .associateByTo(ArrayMap(), { it.filePath }, { it })
                .apply {
                    editorMediaListener.appendMediaFiles(this)
                }
    }

    private fun optimizeMediaAndTrackEvent(mediaUri: Uri, isNew: Boolean, site: SiteModel): Uri? {
        val path = mediaUtilsWrapper.getRealPathFromURI(mediaUri) ?: return null
        val isVideo = mediaUtilsWrapper.isVideo(mediaUri.toString())
        /**
         * If the user enabled the optimize images feature, the image gets rotated in mediaUtils.getOptimizedMedia.
         * If the user haven't enabled it, WPCom server takes care of rotating the image, however we need to rotate it
         * manually on self-hosted sites. (https://github.com/wordpress-mobile/WordPress-Android/issues/5737)
         */
        val updatedMediaUri: Uri = mediaUtilsWrapper.getOptimizedMedia(path, isVideo)
                ?: if (!site.isWPCom) {
                    mediaUtilsWrapper.fixOrientationIssue(path, isVideo) ?: mediaUri
                } else {
                    mediaUri
                }

        editorTracker.trackAddMediaFromDevice(site, isNew, isVideo, updatedMediaUri)

        return updatedMediaUri
    }

    private suspend fun enqueueMediaForUpload(
        localSiteId: Int,
        uri: Uri,
        editorMediaListener: EditorMediaListener
    ): MediaFile? {
        val media = uploadMediaUseCase.queueFileForUpload(editorMediaListener, localSiteId, uri)
        val mediaFile = fluxCUtilsWrapper.mediaFileFromMediaModel(media)
        return media?.let { mediaFile }
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

    private data class OptimizeMediaResult(
        val optimizedMediaUris: List<Uri>,
        val someMediaCouldNotBeRetrieved: Boolean
    )
}
