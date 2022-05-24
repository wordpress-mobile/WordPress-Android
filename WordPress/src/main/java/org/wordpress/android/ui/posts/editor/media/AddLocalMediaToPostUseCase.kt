package org.wordpress.android.ui.posts.editor.media

import android.content.Context
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase.CopyMediaResult
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase.CreateMediaModelsResult
import org.wordpress.android.ui.posts.editor.media.OptimizeMediaUseCase.OptimizeMediaResult
import org.wordpress.android.util.MediaUtilsWrapper
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
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val context: Context
) {
    /**
     * Adds media items with existing localMediaId to the editor and optionally initiates an upload.
     * Does NOT optimize the items.
     */
    suspend fun addLocalMediaToEditorAsync(
        localMediaIds: List<Int>,
        editorMediaListener: EditorMediaListener,
        doUploadAfterAdding: Boolean = true
    ) {
        // Add media to editor and optionally initiate upload
        addToEditorAndOptionallyUpload(
                getMediaModelUseCase.loadMediaByLocalId(localMediaIds),
                editorMediaListener,
                doUploadAfterAdding
        )
    }

    /**
     * Copies files to app storage, optimizes them, adds them to the editor and optionally initiates an upload.
     */
    suspend fun addNewMediaToEditorAsync(
        uriList: List<Uri>,
        site: SiteModel,
        freshlyTaken: Boolean,
        editorMediaListener: EditorMediaListener,
        doUploadAfterAdding: Boolean = true,
        trackEvent: Boolean = true
    ): Boolean {
        val allowedUris = uriList.filter {
            // filter out long video files on free sites
            if (mediaUtilsWrapper.isProhibitedVideoDuration(context, site, it)) {
                // put out a notice to the user that the particular video file was rejected
                editorMediaListener.showVideoDurationLimitWarning(it.path.toString())
                return@filter false
            }

            return@filter true
        }

        return processMediaUris(
                allowedUris,
                site,
                freshlyTaken,
                editorMediaListener,
                doUploadAfterAdding,
                trackEvent)
    }

    private suspend fun processMediaUris(
        uriList: List<Uri>,
        site: SiteModel,
        freshlyTaken: Boolean,
        editorMediaListener: EditorMediaListener,
        doUploadAfterAdding: Boolean = true,
        trackEvent: Boolean = true
    ): Boolean {
        // Copy files to apps storage to make sure they are permanently accessible.
        val copyFilesResult: CopyMediaResult = copyMediaToAppStorageUseCase.copyFilesToAppStorageIfNecessary(uriList)

        // Optimize and rotate the media
        val optimizeMediaResult: OptimizeMediaResult = optimizeMediaUseCase
                .optimizeMediaIfSupportedAsync(
                        site,
                        freshlyTaken,
                        copyFilesResult.permanentlyAccessibleUris,
                        trackEvent
                )

        // Transform Uris to MediaModels
        val createMediaModelsResult: CreateMediaModelsResult = getMediaModelUseCase.createMediaModelFromUri(
                site.id,
                optimizeMediaResult.optimizedMediaUris
        )
        // here we pass a map of "old" (before optimisation) Uris to the new MediaModels which contain
        // both the mediaModel ids and the optimized media URLs.
        // this way, the listener will be able to process from other models pointing to the old URLs
        // and make any needed updates
        editorMediaListener.onMediaModelsCreatedFromOptimizedUris(
                uriList.zip(createMediaModelsResult.mediaModels).toMap()
        )

        // Add media to editor and optionally initiate upload
        addToEditorAndOptionallyUpload(createMediaModelsResult.mediaModels, editorMediaListener, doUploadAfterAdding)

        return !optimizeMediaResult.loadingSomeMediaFailed &&
                !createMediaModelsResult.loadingSomeMediaFailed &&
                !copyFilesResult.copyingSomeMediaFailed
    }

    private fun addToEditorAndOptionallyUpload(
        mediaModels: List<MediaModel>,
        editorMediaListener: EditorMediaListener,
        doUploadAfterAdding: Boolean
    ) {
        // 1. first, set the Post's data in the mediaModels and set them QUEUED if we want to upload
        if (doUploadAfterAdding) {
            updateMediaModel(mediaModels, editorMediaListener)
        }

        // 2. actually append media to the Editor
        appendMediaToEditorUseCase.addMediaToEditor(editorMediaListener, mediaModels)

        // 3. finally, upload
        if (doUploadAfterAdding) {
            uploadMediaUseCase.saveQueuedPostAndStartUpload(editorMediaListener, mediaModels)
        }
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
