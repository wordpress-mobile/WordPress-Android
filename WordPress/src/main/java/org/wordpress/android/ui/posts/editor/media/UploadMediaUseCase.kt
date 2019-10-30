package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.posts.editor.EditorMediaPostData
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import java.io.File
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named

class UploadMediaUseCase @Inject constructor(
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val uploadServiceFacade: UploadServiceFacade,
    private val dispatcher: Dispatcher,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val _toastMessage = SingleLiveEvent<ToastMessageHolder>()
    val toastMessage = _toastMessage as LiveData<ToastMessageHolder>

    /**
     * Queues a media file for upload and starts the UploadService. Toasts will alert the user
     * if there are issues with the file.
     */
    suspend fun queueFileForUpload(
        editorMediaListener: EditorMediaListener,
        localSiteId: Int,
        uri: Uri,
        startingState: MediaUploadState = MediaUploadState.QUEUED
    ): MediaModel? {
        return withContext(bgDispatcher) {
             when (val result = verifyFileExists(uri)) {
                 is FileExistsResult.Error -> {
                     _toastMessage.value = ToastMessageHolder(result.resId, result.msgDuration)
                     null
                 }
                 is FileExistsResult.Success -> {
                    // we need to update media with the local post Id
                    val media = buildMediaModel(localSiteId, uri, startingState, editorMediaListener.editorMediaPostData())
                    dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
                    startUploadService(editorMediaListener, listOf(media))
                    media
                }
            }
        }
    }

    private fun buildMediaModel(
        localSiteId: Int,
        uri: Uri,
        initialUploadState: MediaUploadState,
        postData: EditorMediaPostData
    ): MediaModel {
        val mimeType = mediaUtilsWrapper.getMimeType(uri)
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, localSiteId)!!.let { media ->
            setThumbnailIfAvailable(media, uri)
            setPostIds(media, postData)
            media.setUploadState(initialUploadState)
            media
        }
    }

    /**
     *
     * Only [MediaModel] objects that have `MediaUploadState.QUEUED` statuses will be uploaded. .
     */
    fun startUploadService(editorMediaListener: EditorMediaListener,mediaModels: List<MediaModel>) {
        mediaModels.filter { media ->
            MediaUploadState.fromString(media.uploadState) == MediaUploadState.QUEUED
        }.let { queuedMediaModels ->
            // before starting the service, we need to update the posts' contents so we are sure the service
            // can retrieve it from there on
            editorMediaListener.savePostAsyncFromEditorMedia(EditPostActivity.AfterSavePostListener {
                uploadServiceFacade.uploadMediaFromEditor(ArrayList(queuedMediaModels))
            })
        }
    }

    private fun verifyFileExists(uri: Uri): FileExistsResult {
        return mediaUtilsWrapper.getRealPathFromURI(uri)?.let { path ->
            val file = File(path)
            return if (file.exists()) {
                FileExistsResult.Success
            } else {
                FileExistsResult.Error(R.string.file_not_found, SHORT)
            }
        } ?: FileExistsResult.Error(R.string.editor_toast_invalid_path, SHORT)
    }

    sealed class FileExistsResult {
        data class Error(@StringRes val resId: Int, val msgDuration: Duration) : FileExistsResult()
        object Success : FileExistsResult()
    }

    private fun setPostIds(
        media: MediaModel,
        postData: EditorMediaPostData
    ) {
        if (!postData.isLocalDraft) {
            media.postId = postData.remotePostId
        }
        media.localPostId = postData.localPostId
    }

    private fun setThumbnailIfAvailable(
        media: MediaModel,
        uri: Uri
    ) {
        if (mediaUtilsWrapper.isVideoMimeType(media.mimeType)) {
            media.thumbnailUrl = createVideoThumbnail(uri)
        }
    }

    private fun createVideoThumbnail(uri: Uri): String? {
        val path = mediaUtilsWrapper.getRealPathFromURI(uri)
        return path?.let { mediaUtilsWrapper.getVideoThumbnail(it) }
    }
}
