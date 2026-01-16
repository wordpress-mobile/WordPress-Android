package org.wordpress.android.ui.posts.editor.media

import android.text.TextUtils
import dagger.Reusable
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadService.UploadMediaRetryEvent
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject

/**
 * Coordinates media upload events between FluxC and the editor.
 *
 * This class processes media upload events (progress, success, error, retry) and
 * delegates to the appropriate handlers. It extracts upload coordination logic
 * from EditPostActivity to improve testability.
 */
@Reusable
class MediaUploadCoordinator @Inject constructor(
    private val editorMedia: EditorMedia,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    /**
     * Listener interface for upload events that require Activity-level handling.
     */
    interface UploadEventListener {
        /**
         * Called when a featured image upload completes successfully.
         * @param mediaId The remote media ID of the uploaded featured image.
         */
        fun onFeaturedImageUploaded(mediaId: Long)

        /**
         * @return true if the Activity is finishing and events should be ignored.
         */
        fun isFinishing(): Boolean

        /**
         * @return The local post ID associated with the current editor session.
         */
        fun getPostId(): Int

        /**
         * Shows a snackbar error message to the user.
         * @param message The error message to display.
         */
        fun showUploadError(message: String)
    }

    private var listener: UploadEventListener? = null
    private var editorMediaUploadListener: EditorMediaUploadListener? = null

    /**
     * Initializes the coordinator with the required listeners.
     *
     * @param listener Listener for Activity-level upload events.
     * @param editorMediaUploadListener Listener for editor-level upload events.
     */
    fun start(listener: UploadEventListener, editorMediaUploadListener: EditorMediaUploadListener?) {
        this.listener = listener
        this.editorMediaUploadListener = editorMediaUploadListener
    }

    /**
     * Updates the editor media upload listener. This may change during the Activity lifecycle.
     */
    fun setEditorMediaUploadListener(editorMediaUploadListener: EditorMediaUploadListener?) {
        this.editorMediaUploadListener = editorMediaUploadListener
    }

    /**
     * Processes a FluxC OnMediaUploaded event.
     *
     * @param event The media uploaded event from FluxC.
     * @return true if the event was processed, false if it should be ignored.
     */
    @Suppress("ReturnCount")
    fun processMediaUploadedEvent(event: OnMediaUploaded): Boolean {
        if (listener?.isFinishing() == true) return false

        if (event.isError && !networkUtilsWrapper.isNetworkAvailable()) {
            handleUploadPausedDueToNetwork(event)
            return true
        }

        val media = event.media ?: return false
        when {
            event.isError -> handleMediaUploadedError(media, event.error)
            event.completed -> handleMediaUploadedCompleted(media)
            else -> handleUploadProgress(media, event.progress)
        }
        return true
    }

    private fun handleUploadPausedDueToNetwork(event: OnMediaUploaded) {
        val uploadListener = editorMediaUploadListener ?: return
        val media = event.media ?: return
        editorMedia.onMediaUploadPaused(uploadListener, media, event.error)
    }

    /**
     * Processes a progress event from the upload service.
     *
     * @param event The progress event.
     */
    fun processProgressEvent(event: ProgressEvent) {
        if (listener?.isFinishing() != true) {
            // Use upload progress rather than optimizer progress since the former includes
            // upload + optimization
            val progress = UploadService.getUploadProgressForMedia(event.media)
            handleUploadProgress(event.media, progress)
        }
    }

    /**
     * Processes an upload retry event.
     *
     * @param event The retry event containing media to retry.
     */
    @Suppress("ReturnCount")
    fun processUploadRetryEvent(event: UploadMediaRetryEvent) {
        if (listener?.isFinishing() == true) return
        val mediaList = event.mediaModelList ?: return
        val uploadListener = editorMediaUploadListener ?: return

        mediaList.forEach { media ->
            val localMediaId = media.id.toString()
            val mediaType = if (media.isVideo) {
                EditorFragmentAbstract.MediaType.VIDEO
            } else {
                EditorFragmentAbstract.MediaType.IMAGE
            }
            uploadListener.onMediaUploadRetry(localMediaId, mediaType)
        }
    }

    /**
     * Handles a successful media upload.
     *
     * @param media The uploaded media model.
     */
    fun handleUploadSuccess(media: MediaModel) {
        val postId = listener?.getPostId() ?: return

        if (!media.markedLocallyAsFeatured) {
            editorMediaUploadListener?.onMediaUploadSucceeded(
                media.id.toString(),
                FluxCUtils.mediaFileFromMediaModel(media)
            )
        } else if (media.localPostId == postId) {
            listener?.onFeaturedImageUploaded(media.mediaId)
        }
    }

    /**
     * Handles upload progress updates.
     *
     * @param media The media being uploaded (can be null).
     * @param progress The upload progress (0.0 to 1.0).
     */
    fun handleUploadProgress(media: MediaModel?, progress: Float) {
        val localMediaId = media?.id.toString()
        editorMediaUploadListener?.onMediaUploadProgress(localMediaId, progress)
    }

    private fun handleMediaUploadedError(media: MediaModel, error: MediaError) {
        editorMediaUploadListener?.let { uploadListener ->
            editorMedia.onMediaUploadError(uploadListener, media, error)
        }
    }

    private fun handleMediaUploadedCompleted(media: MediaModel) {
        // If the remote URL on completion is null, we consider this upload unsuccessful
        editorMediaUploadListener?.let { uploadListener ->
            if (TextUtils.isEmpty(media.url)) {
                val error = MediaError(MediaErrorType.GENERIC_ERROR)
                if (!networkUtilsWrapper.isNetworkAvailable()) {
                    editorMedia.onMediaUploadPaused(uploadListener, media, error)
                } else {
                    editorMedia.onMediaUploadError(uploadListener, media, error)
                }
            } else {
                handleUploadSuccess(media)
            }
        }
    }

    /**
     * Creates a MediaFile from a MediaModel for editor callbacks.
     */
    fun createMediaFile(media: MediaModel): MediaFile = FluxCUtils.mediaFileFromMediaModel(media)
}
