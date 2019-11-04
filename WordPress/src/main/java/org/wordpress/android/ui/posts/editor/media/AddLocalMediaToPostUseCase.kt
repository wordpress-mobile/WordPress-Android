package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import javax.inject.Inject

/**
 * Processes a list of local media items in the background (optimizing, resizing, rotating, etc.), adds them to
 * the editor one at a time and initiates their upload.
 */
@Reusable
class AddLocalMediaToPostUseCase @Inject constructor(
    private val optimizeMediaUseCase: OptimizeMediaUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val appendMediaToEditorUseCase: AppendMediaToEditorUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase
) {
    /**
     * Adds media items with existing localMediaId to the editor and initiates an upload. Does NOT optimize the
     * items.
     */
    suspend fun addLocalMediaToEditorAsync(
        localMediaIds: List<Int>,
        editorMediaListener: EditorMediaListener
    ) {
        addToEditorAndUpload(getMediaModelUseCase.loadMediaModelFromDb(localMediaIds), editorMediaListener)
    }

    /**
     * Optimizes new media items, adds then to the editor and initiates an upload.
     */
    suspend fun addNewMediaToEditorAsync(
        uriList: List<Uri>,
        site: SiteModel,
        freshlyTaken: Boolean,
        editorMediaListener: EditorMediaListener
    ): Boolean {
        val optimizeMediaResult = optimizeMediaUseCase
                .optimizeMediaIfSupportedAsync(site, freshlyTaken, uriList)
        val createMediaModelsResult = getMediaModelUseCase.createMediaModelFromUri(
                site.id,
                optimizeMediaResult.optimizedMediaUris
        )
        addToEditorAndUpload(createMediaModelsResult.mediaModels, editorMediaListener)

        return !optimizeMediaResult.loadingSomeMediaFailed && !createMediaModelsResult.loadingSomeMediaFailed
    }

    private fun addToEditorAndUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        changeUploadStatusToQueued(mediaModels, editorMediaListener)
        appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, mediaModels)
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, mediaModels)
    }

    private fun changeUploadStatusToQueued(
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
