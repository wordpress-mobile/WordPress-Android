package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.lifecycle.LiveData
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

/**
 * Processes a list of media in the background (optimizing, resizing, rotating, etc.) and adds them to
 * the editor one at a time.
 */
class AddMediaToPostUseCase @Inject constructor(
    private val optimizeMediaUseCase: OptimizeMediaUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val appendMediaToEditorUseCase: AppendMediaToEditorUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase
) {
    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    suspend fun addLocalMediaToEditorAsync(
        localMediaIds: List<Int>,
        editorMediaListener: EditorMediaListener
    ) {
        addToEditorAndUpload(getMediaModelUseCase.loadMediaModelFromDb(localMediaIds), editorMediaListener)
    }

    suspend fun optimizeIfSupportedAndAddLocalMediaToEditorAsync(
        uriList: List<Uri>,
        site: SiteModel,
        isNew: Boolean,
        editorMediaListener: EditorMediaListener
    ) {
        val optimizeMediaResult = optimizeMediaUseCase.optimizeMediaIfSupportedAsync(
                site,
                isNew,
                uriList
        )
        val mediaModels = getMediaModelUseCase.createMediaModelFromUri(
                site.id,
                optimizeMediaResult.optimizedMediaUris
        )
        addToEditorAndUpload(mediaModels, editorMediaListener)

        if (optimizeMediaResult.someMediaCouldNotBeRetrieved) {
            _snackBarMessage.value = SnackbarMessageHolder(R.string.gallery_error)
        }
    }

    private fun addToEditorAndUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        enqueueForUpload(mediaModels, editorMediaListener)
        appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, mediaModels)
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
}
