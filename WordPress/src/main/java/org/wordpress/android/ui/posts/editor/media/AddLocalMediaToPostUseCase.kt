package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase.CopyMediaResult
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase.CreateMediaModelsResult
import org.wordpress.android.ui.posts.editor.media.OptimizeMediaUseCase.OptimizeMediaResult
import javax.inject.Inject

/**
 * Processes a list of local media items in the background (optimizing, resizing, rotating, etc.), adds them to
 * the editor one at a time and initiates their upload.
 */
@Reusable
class AddLocalMediaToPostUseCase @Inject constructor(
    private val copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase,
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
        // Add media to editor and initiate upload
        addToEditorAndUpload(getMediaModelUseCase.loadMediaByLocalId(localMediaIds), editorMediaListener)
    }

    /**
     * Copies files to app storage, optimizes them, adds them to the editor and optionally initiates an upload.
     */
    suspend fun addNewMediaToEditorAsync(
        uriList: List<Uri>,
        site: SiteModel,
        freshlyTaken: Boolean,
        editorMediaListener: EditorMediaListener,
        doUploadAfterAdding: Boolean = true
    ): Boolean {
        // Copy files to apps storage to make sure they are permanently accessible.
        val copyFilesResult: CopyMediaResult = copyMediaToAppStorageUseCase.copyFilesToAppStorageIfNecessary(uriList)

        // Optimize and rotate the media
        val optimizeMediaResult: OptimizeMediaResult = optimizeMediaUseCase
                .optimizeMediaIfSupportedAsync(
                        site,
                        freshlyTaken,
                        copyFilesResult.permanentlyAccessibleUris
                )

        // Transform Uris to MediaModels
        val createMediaModelsResult: CreateMediaModelsResult = getMediaModelUseCase.createMediaModelFromUri(
                site.id,
                optimizeMediaResult.optimizedMediaUris
        )

        // Add media to editor and initiate upload
        if (doUploadAfterAdding) {
            addToEditorAndUpload(createMediaModelsResult.mediaModels, editorMediaListener)
        } else {
            // only add media without uploading
            appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, createMediaModelsResult.mediaModels)
        }

        return !optimizeMediaResult.loadingSomeMediaFailed &&
                !createMediaModelsResult.loadingSomeMediaFailed &&
                !copyFilesResult.copyingSomeMediaFailed
    }

    private fun addToEditorAndUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        updateMediaModel(mediaModels, editorMediaListener)
        appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, mediaModels)
        uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, mediaModels)
    }

    private fun updateMediaModel(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener
    ) {
        mediaModels.forEach {
            updateMediaModelUseCase.updateMediaModel(
                    it,
                    editorMediaListener.getImmutablePost(),
                    QUEUED
            )
        }
    }
}
