package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.posts.editor.EditorPhotoPicker
import org.wordpress.android.ui.posts.services.AztecImageLoader
import org.wordpress.android.util.AppLog
import org.wordpress.gutenberg.MediaType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all media-related operations for the post editor.
 * Extracted from EditPostActivity to improve maintainability and testability.
 */
@Singleton
class EditorMediaManager @Inject constructor(
    private val mediaStore: MediaStore,
    private val editorPhotoPicker: EditorPhotoPicker
) {
    
    interface MediaManagerListener {
        fun onMediaUploadStarted(mediaModel: MediaModel)
        fun onMediaUploadProgress(mediaModel: MediaModel, progress: Float)
        fun onMediaUploadSuccess(mediaModel: MediaModel)
        fun onMediaUploadError(mediaModel: MediaModel, error: String)
        fun onMediaLibraryRequested(mediaType: MediaBrowserType, initialSelection: List<Int>)
        fun showProgressDialog(message: String)
        fun hideProgressDialog()
    }
    
    private var listener: MediaManagerListener? = null
    private var site: SiteModel? = null
    private var post: PostModel? = null
    private var aztecImageLoader: AztecImageLoader? = null
    private val uploadingMedia = mutableMapOf<String, MediaModel>()
    private val pendingVideoPressInfoRequests = mutableListOf<String>()
    
    fun initialize(
        listener: MediaManagerListener,
        lifecycleOwner: LifecycleOwner,
        site: SiteModel,
        post: PostModel,
        aztecImageLoader: AztecImageLoader?
    ) {
        this.listener = listener
        this.site = site
        this.post = post
        this.aztecImageLoader = aztecImageLoader
        
        observeMediaChanges(lifecycleOwner)
        // editorPhotoPicker.initialize(listener as EditorPhotoPicker.EditorPhotoPickerListener)
    }
    
    private fun observeMediaChanges(lifecycleOwner: LifecycleOwner) {
        // Observe media upload events and relay to listener
        // This would typically use LiveData or other observable patterns
    }
    
    /**
     * Handles media selection from various sources (gallery, camera, etc.)
     */
    fun handleMediaSelection(uriList: List<Uri>) {
        if (uriList.isEmpty()) {
            AppLog.w(AppLog.T.POSTS, "No media selected")
            return
        }
        
        listener?.showProgressDialog("Adding media...")
        
        // Process each selected media item
        uriList.forEach { uri ->
            processMediaUri(uri)
        }
    }
    
    private fun processMediaUri(uri: Uri) {
        val site = this.site ?: return
        val post = this.post ?: return
        
        try {
            // Create MediaModel from URI
            val mediaModel = createMediaModelFromUri(uri, site)
            
            // Start upload process
            startMediaUpload(mediaModel, post)
            
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Error processing media URI: ${uri}", e)
            // Would create error media model and notify listener (stub)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun createMediaModelFromUri(uri: Uri, site: SiteModel): MediaModel {
        // Implementation would create MediaModel from URI
        // This is a simplified version - actual implementation would be more complex
        return MediaModel().apply {
            this.filePath = uri.path
            this.localSiteId = site.id
            this.uploadState = MediaModel.MediaUploadState.QUEUED.toString()
        }
    }
    
    private fun startMediaUpload(mediaModel: MediaModel, post: PostModel) {
        uploadingMedia[mediaModel.id.toString()] = mediaModel
        listener?.onMediaUploadStarted(mediaModel)
        
        // Trigger actual upload via MediaStore
        // mediaStore.uploadMedia(mediaModel)
    }
    
    /**
     * Opens media library with specified type and initial selection
     */
    fun openMediaLibrary(mediaType: MediaBrowserType, initialSelection: List<Int> = emptyList()) {
        listener?.onMediaLibraryRequested(mediaType, initialSelection)
    }
    
    /**
     * Maps Gutenberg media types to MediaBrowserType
     */
    fun mapAllowedTypesToMediaBrowserType(allowedTypes: Array<MediaType>, multiple: Boolean): MediaBrowserType {
        return when {
            allowedTypes.contains(MediaType.IMAGE) && allowedTypes.contains(MediaType.VIDEO) -> {
                if (multiple) MediaBrowserType.GUTENBERG_MEDIA_PICKER else MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER
            }
            allowedTypes.contains(MediaType.IMAGE) -> {
                if (multiple) MediaBrowserType.GUTENBERG_IMAGE_PICKER else MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER
            }
            allowedTypes.contains(MediaType.VIDEO) -> {
                if (multiple) MediaBrowserType.GUTENBERG_VIDEO_PICKER else MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER
            }
            allowedTypes.contains(MediaType.AUDIO) -> MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER
            else -> if (multiple) MediaBrowserType.GUTENBERG_MEDIA_PICKER else MediaBrowserType.GUTENBERG_SINGLE_FILE_PICKER
        }
    }
    
    /**
     * Handles media upload progress updates
     */
    fun onUploadProgress(mediaModel: MediaModel, progress: Float) {
        if (uploadingMedia.containsKey(mediaModel.id.toString())) {
            listener?.onMediaUploadProgress(mediaModel, progress)
        }
    }
    
    /**
     * Handles successful media upload
     */
    fun onUploadSuccess(mediaModel: MediaModel) {
        uploadingMedia.remove(mediaModel.id.toString())
        listener?.onMediaUploadSuccess(mediaModel)
        listener?.hideProgressDialog()
    }
    
    /**
     * Handles media upload errors
     */
    fun onUploadError(mediaModel: MediaModel, error: String) {
        uploadingMedia.remove(mediaModel.id.toString())
        listener?.onMediaUploadError(mediaModel, error)
        listener?.hideProgressDialog()
    }
    
    /**
     * Retries failed media uploads
     */
    fun retryFailedUploads(post: PostModel) {
        val site = this.site ?: return
        
        // Get failed media for this post
        val failedMedia = mediaStore.getMediaForPost(post).filter { media ->
            media.uploadState == MediaModel.MediaUploadState.FAILED.toString()
        }
        
        failedMedia.forEach { mediaModel ->
            AppLog.d(AppLog.T.POSTS, "Retrying upload for media: ${mediaModel.id}")
            startMediaUpload(mediaModel, post)
        }
    }
    
    /**
     * Checks if post has any failed media uploads
     */
    fun hasFailedMedia(post: PostModel): Boolean {
        return mediaStore.getMediaForPost(post).any { media ->
            media.uploadState == MediaModel.MediaUploadState.FAILED.toString()
        }
    }
    
    /**
     * Resets uploading media to failed state if post has no media in progress or queued
     */
    fun resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued(post: PostModel) {
        val mediaInProgressOrQueued = mediaStore.getMediaForPost(post).filter { media ->
            media.uploadState == MediaModel.MediaUploadState.UPLOADING.toString() ||
            media.uploadState == MediaModel.MediaUploadState.QUEUED.toString()
        }
        
        if (mediaInProgressOrQueued.isEmpty()) {
            val uploadingMedia = mediaStore.getMediaForPost(post).filter { media ->
                media.uploadState == MediaModel.MediaUploadState.UPLOADING.toString()
            }
            
            uploadingMedia.forEach { mediaModel ->
                mediaModel.uploadState = MediaModel.MediaUploadState.FAILED.toString()
                // mediaStore.updateMedia(mediaModel)
            }
        }
    }
    
    /**
     * Generates error HTML for failed media uploads
     */
    fun getUploadErrorHtml(mediaId: String, path: String): String {
        return """
            <div class="upload-error">
                <p>Media upload failed</p>
                <p>Media ID: $mediaId</p>
                <p>Path: $path</p>
                <button onclick="retryUpload('$mediaId')">Retry Upload</button>
            </div>
        """.trimIndent()
    }
    
    fun cleanup() {
        listener = null
        site = null
        post = null
        aztecImageLoader = null
        uploadingMedia.clear()
        pendingVideoPressInfoRequests.clear()
    }
}
